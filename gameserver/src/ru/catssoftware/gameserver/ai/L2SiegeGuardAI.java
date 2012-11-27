package ru.catssoftware.gameserver.ai;

import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

import static ru.catssoftware.gameserver.ai.CtrlIntention.*;


public class L2SiegeGuardAI extends L2CharacterAI implements Runnable
{
	private static final class SiegeGuardAiTaskManager extends AbstractIterativePeriodicTaskManager<L2SiegeGuardAI>
	{
		private static final SiegeGuardAiTaskManager _instance = new SiegeGuardAiTaskManager();
		
		private static SiegeGuardAiTaskManager getInstance()
		{
			return _instance;
		}
		
		private SiegeGuardAiTaskManager()
		{
			super(1000);
		}
		
		@Override
		protected void callTask(L2SiegeGuardAI task)
		{
			task.run();
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "run()";
		}
	}

	private static final int	MAX_ATTACK_TIMEOUT	= 300;
	private SelfAnalysis		_selfAnalysis		= new SelfAnalysis();
	private int					_attackTimeout;
	private int					_globalAggro;
	private volatile boolean	_thinking;
	private int					_attackRange;

	public L2SiegeGuardAI(L2Character.AIAccessor accessor)
	{
		super(accessor);

		_selfAnalysis.init();
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn

		_attackRange = _actor.getPhysicalAttackRange()<100?100:_actor.getPhysicalAttackRange();
	}

	public void run()
	{
		onEvtThink();
	}

	private boolean autoAttackCondition(L2Character target)
	{
		if (target == null || target instanceof L2SiegeGuardInstance || target instanceof L2FolkInstance || target instanceof L2DoorInstance)
			return false;

		if (target.isInvul())
		{
			if (target.isPlayer() && ((L2PcInstance) target).isGM())
				return false;
			if (target instanceof L2Summon && ((L2Summon) target).getOwner().isGM())
				return false;
		}

		if (target.isAlikeDead())
			return false;

		if (target instanceof L2Summon)
		{
			L2PcInstance owner = ((L2Summon) target).getOwner();
			if (_actor.isInsideRadius(owner, 1000, true, false))
				target = owner;
		}

		/*if (target instanceof L2PlayableInstance)
		{
			if (((L2PlayableInstance) target).isSilentMoving() && !_actor.isInsideRadius(target, 250, false, false))
				return false;
		}*/
		return (_actor.isAutoAttackable(target) && _actor.canSee(target));
	}

	@Override
	public synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == AI_INTENTION_IDLE /*|| intention == AI_INTENTION_ACTIVE*/) // active becomes idle if only a summon is present
		{
			if (!_actor.isAlikeDead())
			{
				L2Attackable npc = (L2Attackable) _actor;

				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!npc.getKnownList().getKnownPlayers().isEmpty())
					intention = AI_INTENTION_ACTIVE;
				else
					intention = AI_INTENTION_IDLE;
			}

			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);

				// Stop AI task and detach AI from NPC
				stopAITask();

				return;
			}
		}

		// Set the Intention of this L2AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);

		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		SiegeGuardAiTaskManager.getInstance().startTask(this);
	}

	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.<BR><BR>
	 *
	 * @param target The L2Character to attack
	 *
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		//if (_actor.getTarget() != null)
		super.onIntentionAttack(target);
	}

	/**
	 * Manage AI standard thinks of a L2Attackable (called by onEvtThink).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor  can't attack, order to it to return to its home location</li>
	 *
	 */
	private void thinkActive()
	{
		L2Attackable npc = (L2Attackable) _actor;

		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
				_globalAggro++;
			else
				_globalAggro--;
		}

		// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			Iterable<L2Character> chars = npc.getKnownList().getKnownCharactersInRadius(_attackRange);
			if (chars != null)
				for (L2Character target : chars)
				{
					if (target == null)
						continue;
					if (autoAttackCondition(target)) // check aggression
					{
						// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
						int hating = npc.getHating(target);

						// Add the attacker to the L2Attackable _aggroList with 0 damage and 1 hate
						if (hating == 0)
							npc.addDamageHate(target, 0, 1);
					}
				}

			// Chose a target from its aggroList
			L2Character hated;
			if (_actor.isConfused())
				hated = getAttackTarget(); // Force mobs to attack anybody if confused
			else
				hated = npc.getMostHated();

			//_mostHatedAnalysis.Update(hated);

			// Order to the L2Attackable to attack the target
			if (hated != null)
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				int aggro = npc.getHating(hated);

				if (aggro + _globalAggro > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
					if (!_actor.isRunning())
						_actor.setRunning();

					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated, null);
				}

				return;
			}

		}
		// Order to the L2SiegeGuardInstance to return to its home location because there's no target to attack
		((L2SiegeGuardInstance) _actor).returnHome();
	}

	/**
	 * Manage AI attack thinks of a L2Attackable (called by onEvtThink).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all L2Object of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li><BR><BR>
	 *
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 *
	 */
	private void thinkAttack()
	{
		if (_attackTimeout < GameTimeController.getGameTicks())
		{
			// Check if the actor is running
			if (_actor.isRunning())
			{
				// Set the actor movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance
				_actor.setWalking();

				// Calculate a new attack timeout
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}

		L2Character attackTarget = getAttackTarget();
		// Check if target is dead or if timeout is expired to stop this attack
		if (attackTarget == null || _attackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getGameTicks())
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (attackTarget != null)
			{
				L2Attackable npc = (L2Attackable) _actor;
				npc.stopHating(attackTarget);
			}

			// Cancel target and timeout
			_attackTimeout = Integer.MAX_VALUE;
			setAttackTarget(null);

			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE, null, null);

			_actor.setWalking();
			return;
		}

		factionNotifyAndSupport();
		attackPrepare();
	}


	private final void factionNotifyAndSupport()
	{
		final L2Character target = getAttackTarget();
		
		if (((L2NpcInstance)_actor).getFactionId() == null || target == null || target.isInvul())
			return;

		final String faction_id = ((L2NpcInstance)_actor).getFactionId();

		for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(1000))
		{
			if (cha == null)
				continue;

			if (!(cha instanceof L2NpcInstance))
			{
				if (_selfAnalysis.hasHealOrResurrect && cha.isPlayer() && ((L2NpcInstance) _actor).getCastle().getSiege().checkIsDefender(((L2PcInstance) cha).getClan()))
				{
					if (!_actor.isAttackingDisabled() && cha.getStatus().getCurrentHp() < cha.getMaxHp() * 0.6 && _actor.getStatus().getCurrentHp() > _actor.getMaxHp() / 2 && _actor.getStatus().getCurrentMp() > _actor.getMaxMp() / 2 && cha.isInCombat())
					{
						for (L2Skill sk : _selfAnalysis.healSkills)
						{
							if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
								continue;
							if (_actor.isSkillDisabled(sk))
								continue;
							if (!Util.checkIfInRange(sk.getCastRange(), _actor, cha, true))
								continue;
							if (5 >= Rnd.get(100)) // chance
								continue;
							if (!_actor.canSee(cha))
								break;

							L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(cha);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
				continue;
			}

			final L2NpcInstance npc = (L2NpcInstance) cha;

			if (!faction_id.equals(npc.getFactionId()))
				continue;

			if (npc.getAI() != null)
			{
				if (!npc.isDead() && Math.abs(target.getZ() - npc.getZ()) < 600 && (npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE || npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE) && target.isInsideRadius(npc, 1500, true, false) && npc.canSee(target))
				{
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					return;
				}
				// heal friends
				if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackingDisabled() && npc.getStatus().getCurrentHp() < npc.getMaxHp() * 0.6 && _actor.getStatus().getCurrentHp() > _actor.getMaxHp() / 2 && _actor.getStatus().getCurrentMp() > _actor.getMaxMp() / 2 && npc.isInCombat())
				{
					for (L2Skill sk : _selfAnalysis.healSkills)
					{
						if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
							continue;
						if (_actor.isSkillDisabled(sk))
							continue;
						if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
							continue;
						if (4 >= Rnd.get(100)) // chance
							continue;
						if (!_actor.canSee(npc))
							break;
						L2Object OldTarget = _actor.getTarget();
						clientStopMoving(null);
						_actor.setTarget(npc);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
		}
	}

	private void attackPrepare()
	{
		// Get all information needed to choose between physical or magical attack
		L2Skill[] skills = null;
		double dist_2 = 0;
		int range = 0;
		L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
		L2Character attackTarget = getAttackTarget();

		if (attackTarget != null)
		{
			_actor.setTarget(attackTarget);
			skills = _actor.getAllSkills();
			dist_2 = _actor.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY());
			range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
			if (attackTarget.isMoving())
				range += 50;
		}
		else
		{
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		// never attack defenders
		if (attackTarget.isPlayer() && (sGuard.getCastle().getSiege().checkIsDefender(((L2PcInstance) attackTarget).getClan())))
		{
			// Cancel the target
			sGuard.stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		if (!_actor.canSee(attackTarget))
		{
			// Siege guards differ from normal mobs currently:
			// If target cannot seen, don't attack any more
			sGuard.stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		// Check if the actor isn't muted and if it is far from target
		if (!_actor.isMuted() && dist_2 > range * range)
		{
			// check for long ranged skills and heal/buff skills
			for (L2Skill sk : skills)
			{
				int castRange = sk.getCastRange();

				if ((dist_2 <= castRange * castRange) && castRange > 70 && !_actor.isSkillDisabled(sk)
						&& _actor.getStatus().getCurrentMp() >= _actor.getStat().getMpConsume(sk) && !sk.isPassive())
				{

					L2Object OldTarget = _actor.getTarget();
					if (sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
					{
						boolean useSkillSelf = true;
						if (sk.getSkillType() == L2SkillType.HEAL && _actor.getStatus().getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
						{
							useSkillSelf = false;
							break;
						}
						if (sk.getSkillType() == L2SkillType.BUFF)
						{
							L2Effect[] effects = _actor.getAllEffects();
							for (int i = 0; effects != null && i < effects.length; i++)
							{
								L2Effect effect = effects[i];
								if (effect.getSkill() == sk)
								{
									useSkillSelf = false;
									break;
								}
							}
						}
						if (useSkillSelf)
							_actor.setTarget(_actor);
					}

					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(OldTarget);
					return;
				}
			}

			// Check if the L2SiegeGuardInstance is attacking, knows the target and can't run
			if (!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (_actor.getKnownList().knowsObject(attackTarget)))
			{
				// Cancel the target
				_actor.getKnownList().removeKnownObject(attackTarget);
				_actor.setTarget(null);
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			else
			{
				double dx = _actor.getX() - attackTarget.getX();
				double dy = _actor.getY() - attackTarget.getY();
				double dz = _actor.getZ() - attackTarget.getZ();
				double homeX = attackTarget.getX() - sGuard.getSpawn().getLocx();
				double homeY = attackTarget.getY() - sGuard.getSpawn().getLocy();

				// Check if the L2SiegeGuardInstance isn't too far from it's home location
				if ((dx * dx + dy * dy > 10000) && (homeX * homeX + homeY * homeY > 3240000) // 1800 * 1800
						&& (_actor.getKnownList().knowsObject(attackTarget)))
				{
					// Cancel the target
					_actor.getKnownList().removeKnownObject(attackTarget);
					_actor.setTarget(null);
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				else
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
				{
					// Temporary hack for preventing guards jumping off towers,
					// before replacing this with effective geodata checks and AI modification
					if (dz * dz < 28900) // 170 // normally 130 if guard z coordinates correct
					{
						if (_selfAnalysis.isHealer)
							return;
						if (_selfAnalysis.isMage)
							range = _selfAnalysis.maxCastRange - 50;
						if (_attackTarget.isMoving())
							moveToPawn(attackTarget, range - 70);
						else
							moveToPawn(attackTarget, range);
					}
				}
			}
		}
		// Else, if the actor is muted and far from target, just "move to pawn"
		else if (_actor.isMuted() && dist_2 > range * range && !_selfAnalysis.isHealer)
		{
			// Temporary hack for preventing guards jumping off towers,
			// before replacing this with effective geodata checks and AI modification
			double dz = _actor.getZ() - attackTarget.getZ();
			if (dz * dz < 170 * 170) // normally 130 if guard z coordinates correct
			{
				if (_selfAnalysis.isMage)
					range = _selfAnalysis.maxCastRange - 50;
				if (_attackTarget.isMoving())
					moveToPawn(attackTarget, range - 70);
				else
					moveToPawn(attackTarget, range);
			}
		}
		// Else, if this is close enough to attack
		else if (dist_2 <= range * range)
		{
			// Force mobs to attack anybody if confused
			L2Character hated = null;
			if (_actor.isConfused())
				hated = attackTarget;
			else
				hated = ((L2Attackable) _actor).getMostHated();

			if (hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE, null, null);
				return;
			}
			if (hated != attackTarget)
				attackTarget = hated;

			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

			// check for close combat skills && heal/buff skills
			if (!_actor.isMuted() && Rnd.nextInt(100) <= 5)
			{
				for (L2Skill sk : skills)
				{
					int castRange = sk.getCastRange();

					if (castRange * castRange >= dist_2 && !sk.isPassive() && _actor.getStatus().getCurrentMp() >= _actor.getStat().getMpConsume(sk)
							&& !_actor.isSkillDisabled(sk))
					{
						L2Object OldTarget = _actor.getTarget();
						if (sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
						{
							boolean useSkillSelf = true;
							if (sk.getSkillType() == L2SkillType.HEAL && _actor.getStatus().getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
							{
								useSkillSelf = false;
								break;
							}
							if (sk.getSkillType() == L2SkillType.BUFF)
							{
								L2Effect[] effects = _actor.getAllEffects();
								for (int i = 0; effects != null && i < effects.length; i++)
								{
									L2Effect effect = effects[i];
									if (effect.getSkill() == sk)
									{
										useSkillSelf = false;
										break;
									}
								}
							}
							if (useSkillSelf)
								_actor.setTarget(_actor);
						}

						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
			// Finally, do the physical attack itself
			if (!_selfAnalysis.isHealer)
				_accessor.doAttack(attackTarget);
		}
	}

	/**
	 * Manage AI thinking actions of a L2Attackable.<BR><BR>
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the thinking action is already in progress
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
			return;

		// Start thinking action
		_thinking = true;

		try
		{
			// Manage AI thinks of a L2Attackable
			if (getIntention() == AI_INTENTION_ACTIVE)
				thinkActive();
			else if (getIntention() == AI_INTENTION_ATTACK)
				thinkAttack();
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}

	/**
	 * Launch actions corresponding to the Event Attacked.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li><BR><BR>
	 *
	 * @param attacker The L2Character that attacks the actor
	 *
	 */
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
			_globalAggro = 0;

		// Add the attacker to the _aggroList of the actor
		((L2Attackable) _actor).addDamageHate(attacker, 0, 1);

		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning())
			_actor.setRunning();

		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker, null);

		super.onEvtAttacked(attacker);
	}

	/**
	 * Launch actions corresponding to the Event Aggression.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Add the target to the actor _aggroList or update hate if already present </li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li><BR><BR>
	 *
	 * @param attacker The L2Character that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 *
	 */
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		if (_actor == null)
			return;
		L2Attackable me = (L2Attackable) _actor;

		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);

			// Get the hate of the actor against the target
			aggro = me.getHating(target);

			if (aggro <= 0)
			{
				if (me.getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList();
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				return;
			}

			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!_actor.isRunning())
					_actor.setRunning();

				L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
				double homeX = target.getX() - sGuard.getSpawn().getLocx();
				double homeY = target.getY() - sGuard.getSpawn().getLocy();

				// Check if the L2SiegeGuardInstance is not too far from its home location
				if (homeX * homeX + homeY * homeY < 3240000) // 1800 * 1800
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, target, null);
			}
		}
		else
		{
			//currently only for setting lower general aggro
			if (aggro >= 0)
				return;

			L2Character mostHated = me.getMostHated();
			if (mostHated == null)
			{
				_globalAggro = -25;
				return;
			}
			for (L2Character aggroed : me.getAggroListRP().keySet())
				me.addDamageHate(aggroed, 0, aggro);

			aggro = me.getHating(mostHated);
			if (aggro <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList();
				setIntention(AI_INTENTION_IDLE, null, null);
			}
		}
	}

	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}

	@Override
	public void stopAITask()
	{
		SiegeGuardAiTaskManager.getInstance().stopTask(this);
		_accessor.detachAI();
	}
}