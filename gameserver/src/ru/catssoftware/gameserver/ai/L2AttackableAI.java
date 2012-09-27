package ru.catssoftware.gameserver.ai;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

import java.util.Collection;
import java.util.List;

import static ru.catssoftware.gameserver.ai.CtrlIntention.*;

public class L2AttackableAI extends L2CharacterAI implements Runnable
{
	private static final class AttackableAiTaskManager extends AbstractIterativePeriodicTaskManager<L2AttackableAI>
	{
		private static final AttackableAiTaskManager _instance = new AttackableAiTaskManager();

		private static AttackableAiTaskManager getInstance()
		{
			return _instance;
		}

		private AttackableAiTaskManager()
		{
			super(1000);
		}

		@Override
		protected void callTask(L2AttackableAI task)
		{
			task.run();
		}

		@Override
		protected String getCalledMethodName()
		{
			return "run()";
		}
	}

	private static final int	RANDOM_WALK_RATE			= 30;
	private static final int	MAX_ATTACK_TIMEOUT			= 300;

	private SelfAnalysis		_selfAnalysis				= new SelfAnalysis();
	private TargetAnalysis		_mostHatedAnalysis			= new TargetAnalysis();
	private TargetAnalysis		_secondMostHatedAnalysis	= new TargetAnalysis();

	private int					_attackTimeout;
	private int					_globalAggro;
	private volatile boolean	_thinking;

	public L2AttackableAI(L2Character.AIAccessor accessor)
	{
		super(accessor);

		_selfAnalysis.init();
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10;
	}

	public void run()
	{
		onEvtThink();
	}

	protected boolean autoAttackCondition(L2Character target)
	{
		if (target == null || !(_actor instanceof L2Attackable))
			return false;

		if (target instanceof L2FolkInstance || target instanceof L2DoorInstance)
			return false;

		L2Attackable me = (L2Attackable) _actor;

		// Если цель мертва или невходит в агро рэнж, то пропускаем
		if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || Math.abs(_actor.getZ() - target.getZ()) > 300)
			return false;

		if (_selfAnalysis.cannotMoveOnLand && !target.isInsideZone(L2Zone.FLAG_WATER))
			return false;

		// Проверка цель, если это L2PlayableInstance
		if (target instanceof L2PlayableInstance)
		{
			L2PcInstance player = target.getActingPlayer();
			if(Config.ALT_MOB_NOAGRO>0 && player!=null) {
				if(player.getLevel()>=me.getLevel()+Config.ALT_MOB_NOAGRO)
					return false;
			}

			// Check if the AI isn't a Raid Boss and the target isn't in silent move mode
			if (!(me instanceof L2Boss) && ((L2PlayableInstance) target).isSilentMoving() && !me.getFactionId().equals("exfact"))
				return false;
		}

		// Проверка цель, если это L2PcInstance
		if (target instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) target;

			// Проверка GM статусов
			if (player.isGM() && player.isInvul())
					return false;

			// Проверка факшенов сервера
			if (me.getFactionId() != null)
				if (me.getFactionId().equals("varka") && player.isAlliedWithVarka())
					return false;
				else if (me.getFactionId().equals("ketra") && player.isAlliedWithKetra())
					return false;

			// Игроки участники эвентов не получают агро
			if (player.isInFunEvent())
				return false;

			// Игрок в период восстановления не получает агро
			if (player.isRecentFakeDeath())
				return false;

			// Игрок в away режиме не получает агро, если включено по конфигу
			if (player.isAway() && !Config.ALT_AWAY_PLAYER_TAKE_AGGRO)
				return false;
		}

		// Проверка цель, если это L2Summon
		if (target instanceof L2Summon)
		{
			L2PcInstance owner = ((L2Summon) target).getOwner();

			if (owner != null)
			{
				// Проверка GM статусов
				if (owner.isGM() && owner.isInvul())
					return false;

				// Проверка факшенов сервера
				if (me.getFactionId().equals("varka") && owner.isAlliedWithVarka())
					return false;
				if (me.getFactionId().equals("ketra") && owner.isAlliedWithKetra())
					return false;

				// Если AI работает на Pagan Gatekeeper, то ищем марки в инвентаре
				if (me.getNpcId() == 22136)
				{
					for (L2ItemInstance item : owner.getInventory().getItems())
					{
						if (item != null && (item.getItemId() == 8064) || (item.getItemId() == 8065) || (item.getItemId() == 8067))
							return false;
					}
				}
			}
		}

		// Проверка цель, если это L2GuardInstance
		if (me instanceof L2GuardInstance)
		{
			// Гварды атакуют PK
			if (target instanceof L2PcInstance)
				return ((L2PcInstance) target).getKarma() > 0 && GeoData.getInstance().canSeeTarget(me, target);

			// Гварды атакуют Агров (Мобов)
			if (Config.GUARD_ATTACK_MOBS &&  target instanceof L2MonsterInstance) {
				if(target instanceof L2RaidBossInstance) 
					return false;
				L2MonsterInstance monster = (L2MonsterInstance) target;
				if(monster.isInCombat() && monster.getTarget().getActingPlayer()!=null)
					return GeoData.getInstance().canSeeTarget(me, target);
			}

			return false;
		}
		// Проверка цель, если это L2FriendlyMobInstance
		else if (me instanceof L2FriendlyMobInstance)
		{
			if (target instanceof L2NpcInstance)
				return false;

			// Чары атакуют PK
			return target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0 && GeoData.getInstance().canSeeTarget(me, target);
		}
		else
		{
			if (target instanceof L2NpcInstance)
				return false;

			// Проверка возможен ли агр в безопасной зоне
			if (!Config.ALT_MOB_AGGRO_IN_PEACEZONE && target.isInsideZone(L2Zone.FLAG_PEACE))
				return false;

			// Если моб чемпион и агр чемпионов отключен, то не агримся
			if (me.isChampion() && Config.CHAMPION_PASSIVE)
				return false;

			// Если AI работает на Pagan Gatekeeper, то ищем марки в инвентаре
			if (me.getNpcId() == 22136)
			{
				for (L2ItemInstance item : target.getInventory().getItems())
				{
					if (item != null && (item.getItemId() == 8064) || (item.getItemId() == 8065) || (item.getItemId() == 8067))
						return false;
				}
			}
			return (me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
		}
	}

	public void startAITask()
	{
		AttackableAiTaskManager.getInstance().startTask(this);
	}

	@Override
	public void stopAITask()
	{
		AttackableAiTaskManager.getInstance().stopTask(this);
		_accessor.detachAI();
	}

	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}

	@Override
	public synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE)
		{
			if (!_actor.isAlikeDead())
			{
				L2Attackable npc = (L2Attackable) _actor;
				if (!npc.getKnownList().getKnownPlayers().isEmpty())
					intention = AI_INTENTION_ACTIVE;
			}

			if (intention == AI_INTENTION_IDLE)
			{
				super.changeIntention(AI_INTENTION_IDLE, null, null);

				stopAITask();
				return;
			}
		}

		super.changeIntention(intention, arg0, arg1);
		
		startAITask();
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		if (_selfAnalysis.lastBuffTick + 100 < GameTimeController.getGameTicks())
		{
			for (L2Skill sk : _selfAnalysis.buffSkills)
			{
				if (_actor.getFirstEffect(sk.getId()) == null)
				{
					if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
						continue;
					if (_actor.isSkillDisabled(sk.getId()))
						continue;
					if (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_CLAN)
						continue;

					L2Object OldTarget = _actor.getTarget();

					_actor.setTarget(_actor);
					clientStopMoving(null);
					_accessor.doCast(sk);
					_selfAnalysis.lastBuffTick = GameTimeController.getGameTicks();
					_actor.setTarget(OldTarget);
				}
			}
		}
		super.onIntentionAttack(target);
	}

	protected void thinkActive()
	{
		L2Attackable npc = (L2Attackable) _actor;

		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
				_globalAggro++;
			else
				_globalAggro--;
		}

		if (_globalAggro >= 0)
		{
			for (L2Object obj : npc.getKnownList().getKnownObjects().values())
			{
				if (!(obj instanceof L2Character))
					continue;
				L2Character target = (L2Character) obj;

				if ((_actor instanceof L2FestivalMonsterInstance) && obj instanceof L2PcInstance)
				{
					L2PcInstance targetPlayer = (L2PcInstance) obj;
					if (!targetPlayer.isFestivalParticipant())
						continue;
				}

				if (autoAttackCondition(target))
				{
					if (npc.getHating(target) == 0)
						npc.addDamageHate(target, 0, 1);
				}
			}

			L2Character hated;

			if (_actor.isConfused())
				hated = getAttackTarget();
			else
				hated = npc.getMostHated();

			// Order to the L2Attackable to attack the target
			if (hated != null)
			{
				int aggro = npc.getHating(hated);

				if (aggro + _globalAggro > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
					if (!_actor.isRunning())
						_actor.setRunning();

					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);

					L2MinionInstance minion;
					L2MonsterInstance boss;
					List<L2MinionInstance> minions;

					if (_actor instanceof L2MonsterInstance)
					{
						boss = (L2MonsterInstance) _actor;
						if (boss.hasMinions())
						{
							minions = boss.getSpawnedMinions();
							if (minions != null)
							{
								for (L2MinionInstance m : minions)
								{
									if (m == null)
										continue;

									if (!m.isRunning())
										m.setRunning();
									m.getAI().startFollow(_actor);
								}
							}
						}
					}
					else if (_actor instanceof L2MinionInstance)
					{
						minion = (L2MinionInstance) _actor;
						boss = minion.getLeader();
						if (!boss.isRunning())
							boss.setRunning();
						boss.getAI().startFollow(_actor);
						minions = boss.getSpawnedMinions();
						for (L2MinionInstance m : minions)
						{
							
							if (!(m.getObjectId() == _actor.getObjectId()))
							{
								if (!m.isRunning())
									m.setRunning();
								m.getAI().startFollow(_actor);
							}
						}

					}
				}
				return;
			}
		}


		if (_actor instanceof L2FestivalMonsterInstance)
			return;

		if (!npc.canReturnToSpawnPoint())
			return;
		
		

		if (_actor instanceof L2MinionInstance)
		{
			L2MinionInstance minion = (L2MinionInstance) _actor;

			if (minion.getLeader() == null)
				return;

			int offset;

			if (_actor.isRaid())
				offset = 500;
			else
				offset = 200;

			if (minion.getLeader().isRunning())
				_actor.setRunning();
			else
				_actor.setWalking();

			if (_actor.getPlanDistanceSq(minion.getLeader()) > offset * offset)
			{
				int x1, y1, z1;
				x1 = minion.getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				y1 = minion.getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				z1 = minion.getLeader().getZ();

				moveTo(x1, y1, z1);
			}
			else if (Rnd.nextInt(RANDOM_WALK_RATE) == 0)
			{
				for (L2Skill sk : _selfAnalysis.buffSkills)
				{
					if (_actor.getFirstEffect(sk.getId()) == null)
					{
						if (sk.getTargetType() != L2Skill.SkillTargetType.TARGET_SELF && Rnd.nextInt(2) != 0)
							continue;
						if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
							continue;
						if (_actor.isSkillDisabled(sk.getId()))
							continue;

						L2Object OldTarget = _actor.getTarget();

						_actor.setTarget(_actor);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
		}
		else if (npc.getSpawn() != null && Rnd.nextInt(RANDOM_WALK_RATE) == 0 && !(_actor.isRaid() || _actor instanceof L2MinionInstance || _actor instanceof L2ChestInstance || _actor instanceof L2GuardInstance))
		{
			for (L2Skill sk : _selfAnalysis.buffSkills)
			{
				if (_actor.getFirstEffect(sk.getId()) == null)
				{
					if (sk.getTargetType() != L2Skill.SkillTargetType.TARGET_SELF && Rnd.nextInt(2) != 0)
						continue;
					if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
						continue;
					if (_actor.isSkillDisabled(sk.getId()))
						continue;

					L2Object OldTarget = _actor.getTarget();

					_actor.setTarget(_actor);
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(OldTarget);
					return;
				}
			}

			if (_actor instanceof L2Boss || _actor instanceof L2MinionInstance || _actor instanceof L2ChestInstance || _actor instanceof L2GuardInstance)
				return;

			int range = Config.MAX_DRIFT_RANGE;
			int x1 = npc.getSpawn().getLocx();
			int y1 = npc.getSpawn().getLocy();
			int z1 = npc.getSpawn().getLocz();

			boolean turnBackToStartLocation=false;

			if (Math.sqrt(_actor.getPlanDistanceSq(x1, y1)) > range * 2)
			{
				turnBackToStartLocation=true;
				if (!_actor.isDead())
					_actor.teleToLocation(x1, y1, z1);
			}
			else
			{
				x1 += Rnd.nextInt(range);
				y1 += Rnd.nextInt(range);
				z1 = npc.getZ();
				moveTo(x1, y1, z1);
			}

			if (_actor instanceof L2MonsterInstance)
			{
				L2MonsterInstance boss = (L2MonsterInstance) _actor;
				if (boss.hasMinions())
					boss.callMinions(turnBackToStartLocation);
			}
		}

		_actor.returnHome();
	}

	protected void thinkIdle() {
		
	}
	protected void thinkAttack()
	{
		if (_attackTimeout < GameTimeController.getGameTicks())
		{
			if (_actor.isRunning())
			{
				_actor.setWalking();
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}

		L2Character originalAttackTarget = getAttackTarget();

		// Check if target is dead or if timeout is expired to stop this attack
		if (originalAttackTarget == null || originalAttackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getGameTicks())
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (originalAttackTarget != null)
				((L2Attackable) _actor).stopHating(originalAttackTarget);

			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);

			_actor.setWalking();
			return;
		}

		// Handle all L2Object of its Faction inside the Faction Range
		if (_actor.getNpc().getFactionId() != null)
		{
			String faction_id = ((L2NpcInstance) _actor).getFactionId();

			// Go through all L2Object that belong to its faction
			Collection <L2Object> objectsCollect = _actor.getKnownList().getKnownObjects().values();

			if (objectsCollect==null)
				return;

			//TODO: Временный фикс, не совсем понятно откуда берется NullPointer
			try
			{
				for (L2Object obj : objectsCollect)
				{
					if (obj instanceof L2NpcInstance)
					{
						L2NpcInstance npc = (L2NpcInstance) obj;

						//Handle SevenSigns mob Factions

						boolean sevenSignFaction = false;

						if (!faction_id.equals(npc.getFactionId()) && !sevenSignFaction)
							continue;
						if (_actor.isInsideRadius(npc, npc.getFactionRange() + npc.getTemplate().getCollisionRadius(), true, false) && npc.getAI() != null)
						{
							if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackingDisabled() && npc.getStatus().getCurrentHp() < npc.getMaxHp() * 0.6 && _actor.getStatus().getCurrentHp() > _actor.getMaxHp() / 2 && _actor.getStatus().getCurrentMp() > _actor.getMaxMp() / 2)
							{
								if (npc.isDead() && _actor instanceof L2MinionInstance)
								{
									if (((L2MinionInstance) _actor).getLeader() == npc)
									{
										for (L2Skill sk : _selfAnalysis.resurrectSkills)
										{
											if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
												continue;
											if (_actor.isSkillDisabled(sk.getId()))
												continue;
											if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
												continue;
											if (10 >= Rnd.get(100)) // chance
												continue;
											if (!GeoData.getInstance().canSeeTarget(_actor, npc))
												break;

											L2Object OldTarget = _actor.getTarget();

											_actor.setTarget(npc);
											DecayTaskManager.getInstance().cancelDecayTask(npc);
											DecayTaskManager.getInstance().addDecayTask(npc);
											clientStopMoving(null);
											_accessor.doCast(sk);
											_actor.setTarget(OldTarget);
											return;
										}
									}
								}
								else if (npc.isInCombat())
								{
									for (L2Skill sk : _selfAnalysis.healSkills)
									{
										if (_actor.getStatus().getCurrentMp() < sk.getMpConsume())
											continue;
										if (_actor.isSkillDisabled(sk.getId()))
											continue;
										if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
											continue;

										int chance = 4;
										if (_actor instanceof L2MinionInstance)
										{
											// minions support boss
											if (((L2MinionInstance) _actor).getLeader() == npc)
												chance = 6;
											else
												chance = 3;
										}
										if (npc instanceof L2Boss)
											chance = 6;
										if (chance >= Rnd.get(100)) // chance
											continue;
										if (!GeoData.getInstance().canSeeTarget(_actor, npc))
											break;

										L2Object OldTarget = _actor.getTarget();
										_actor.setTarget(npc);
										clientStopMoving(null);
										_accessor.doCast(sk);
										_actor.setTarget(OldTarget);
										return;
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (_actor.isAttackingDisabled())
			return;

		L2Character[] hated = ((L2Attackable) _actor).get2MostHated();
		if (_actor.isConfused())
		{
			if (hated != null)
				hated[0] = originalAttackTarget; // effect handles selection
			else
				hated = new L2Character[] { originalAttackTarget, null };
		}

		if (hated == null || hated[0] == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		if (hated[0] != originalAttackTarget)
			setAttackTarget(hated[0]);
		_mostHatedAnalysis.update(hated[0]);
		_secondMostHatedAnalysis.update(hated[1]);
		// Get all information needed to choose between physical or magical attack
		_actor.setTarget(_mostHatedAnalysis.character);
		double dist2 = _actor.getPlanDistanceSq(_mostHatedAnalysis.character.getX(), _mostHatedAnalysis.character.getY());
		int combinedCollision = _actor.getTemplate().getCollisionRadius() + _mostHatedAnalysis.character.getTemplate().getCollisionRadius();
		int range = _actor.getPhysicalAttackRange() + combinedCollision;

		// Reconsider target next round if _actor hasn't got hits in for last 14 seconds
		if (!_actor.isMuted() && _attackTimeout - 160 < GameTimeController.getGameTicks() && _secondMostHatedAnalysis.character != null)
		{
			if (Util.checkIfInRange(900, _actor, hated[1], true))
			{
				// take off 2* the amount the aggro is larger than second most
				((L2Attackable) _actor).reduceHate(hated[0], 2 * (((L2Attackable) _actor).getHating(hated[0]) - ((L2Attackable) _actor).getHating(hated[1])));
				// Calculate a new attack timeout
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}
		// Reconsider target during next round if actor is rooted and cannot reach mostHated but can
		// reach secondMostHated
		if (_actor.isRooted() && _secondMostHatedAnalysis.character != null)
		{
			if (_selfAnalysis.isMage
					&& dist2 > _selfAnalysis.maxCastRange * _selfAnalysis.maxCastRange
					&& _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY()) < _selfAnalysis.maxCastRange
					* _selfAnalysis.maxCastRange)
			{
				((L2Attackable) _actor).reduceHate(hated[0], 1 + (((L2Attackable) _actor).getHating(hated[0]) - ((L2Attackable) _actor).getHating(hated[1])));
			}
			else if (dist2 > range * range && _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY()) < range * range)
				((L2Attackable) _actor).reduceHate(hated[0], 1 + (((L2Attackable) _actor).getHating(hated[0]) - ((L2Attackable) _actor).getHating(hated[1])));
		}

		// Considering, if bigger range will be attempted
		if ((dist2 < 10000 + combinedCollision * combinedCollision) && !_selfAnalysis.isFighter && !_selfAnalysis.isBalanced && (_selfAnalysis.hasLongRangeSkills || _selfAnalysis.isArcher || _selfAnalysis.isHealer) && (_mostHatedAnalysis.isBalanced || _mostHatedAnalysis.isFighter) && (_mostHatedAnalysis.character.isRooted() || _mostHatedAnalysis.isSlower) && (Config.PATHFINDING? 20 : 12) >= Rnd.get(100))
		{
			int posX = _actor.getX();
			int posY = _actor.getY();
			int posZ = _actor.getZ();
			double distance = Math.sqrt(dist2); // This way, we only do the sqrt if we need it

			int signx = -1;
			int signy = -1;
			if (_actor.getX() > _mostHatedAnalysis.character.getX())
				signx = 1;
			if (_actor.getY() > _mostHatedAnalysis.character.getY())
				signy = 1;
			posX += Math.round((float) ((signx * ((range / 2) + (Rnd.get(range)))) - distance));
			posY += Math.round((float) ((signy * ((range / 2) + (Rnd.get(range)))) - distance));
			setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, 0));
			return;
		}

		// Cannot see target, needs to go closer, currently just goes to range 300 if mage
		if ((dist2 > 96100 + combinedCollision * combinedCollision) && _selfAnalysis.hasLongRangeSkills && !GeoData.getInstance().canSeeTarget(_actor, _mostHatedAnalysis.character))
		{
			if (!(_selfAnalysis.isMage && _actor.isMuted()))
			{
				moveToPawn(_mostHatedAnalysis.character, 300);
				return;
			}
		}

		if (_mostHatedAnalysis.character.isMoving())
			range += 50;
		// Check if the actor is far from target
		if (dist2 > range * range)
		{
			if (!_actor.isMuted() && (_selfAnalysis.hasLongRangeSkills || !_selfAnalysis.healSkills.isEmpty()))
			{
				// check for long ranged skills and heal/buff skills
				if (!_mostHatedAnalysis.isCanceled)
				{
					for (L2Skill sk : _selfAnalysis.cancelSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= 8)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_mostHatedAnalysis.isCanceled = true;
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (_selfAnalysis.lastDebuffTick + 60 < GameTimeController.getGameTicks())
				{
					for (L2Skill sk : _selfAnalysis.debuffSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						int chance = 8;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isMage)
							chance = 3;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isArcher)
							chance = 12;
						if (_selfAnalysis.isMage && !_mostHatedAnalysis.isMage)
							chance = 10;
						if (_selfAnalysis.isHealer)
							chance = 12;
						if (_mostHatedAnalysis.isMagicResistant)
							chance /= 2;

						if (Rnd.nextInt(100) <= chance)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_selfAnalysis.lastDebuffTick = GameTimeController.getGameTicks();
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (!_mostHatedAnalysis.character.isMuted())
				{
					int chance = 8;
					if (!(_mostHatedAnalysis.isMage || _mostHatedAnalysis.isBalanced))
						chance = 3;
					for (L2Skill sk : _selfAnalysis.muteSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= chance)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isMuted() && (_secondMostHatedAnalysis.isMage || _secondMostHatedAnalysis.isBalanced))
				{
					double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
					for (L2Skill sk : _selfAnalysis.muteSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (secondHatedDist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= 2)
						{
							_actor.setTarget(_secondMostHatedAnalysis.character);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(_mostHatedAnalysis.character);
							return;
						}
					}
				}
				if (!_mostHatedAnalysis.character.isSleeping())
				{
					for (L2Skill sk : _selfAnalysis.sleepSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 10 : 1))
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isSleeping())
				{
					double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
					for (L2Skill sk : _selfAnalysis.sleepSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (secondHatedDist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 10 : 3))
						{
							_actor.setTarget(_secondMostHatedAnalysis.character);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(_mostHatedAnalysis.character);
							return;
						}
					}
				}
				if (!_mostHatedAnalysis.character.isRooted())
				{
					for (L2Skill sk : _selfAnalysis.rootSkills)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= (_mostHatedAnalysis.isSlower ? 3 : 8))
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (!_mostHatedAnalysis.character.isAttackingDisabled())
				{
					for (L2Skill sk : _selfAnalysis.generalDisablers)
					{
						int castRange = sk.getCastRange() + combinedCollision;
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
								|| (dist2 > castRange * castRange))
							continue;
						if (Rnd.nextInt(100) <= ((_selfAnalysis.isFighter && _actor.isRooted()) ? 15 : 7))
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
				if (_actor.getStatus().getCurrentHp() < _actor.getMaxHp() * 0.4)
				{
					for (L2Skill sk : _selfAnalysis.healSkills)
					{
						if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk))
							continue;
						int chance = 7;
						if (_mostHatedAnalysis.character.isAttackingDisabled())
							chance += 10;
						if (_secondMostHatedAnalysis.character == null || _secondMostHatedAnalysis.character.isAttackingDisabled())
							chance += 10;
						if (Rnd.nextInt(100) <= chance)
						{
							_actor.setTarget(_actor);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(_mostHatedAnalysis.character);
							return;
						}
					}
				}

				// chance decision for launching long range skills
				int castingChance = 5;
				if (_selfAnalysis.isMage || _selfAnalysis.isHealer)
					castingChance = 50; // mages
				if (_selfAnalysis.isBalanced)
				{
					if (!_mostHatedAnalysis.isFighter) // advance to mages
						castingChance = 15;
					else
						castingChance = 25; // stay away from fighters
				}
				if (_selfAnalysis.isFighter)
				{
					if (_mostHatedAnalysis.isMage)
						castingChance = 3;
					else
						castingChance = 7;
					if (_actor.isRooted())
						castingChance = 20; // doesn't matter if no success first round
				}
				for (L2Skill sk : _selfAnalysis.generalSkills)
				{
					int castRange = sk.getCastRange() + combinedCollision;
					if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
							|| (dist2 > castRange * castRange))
						continue;

					if (Rnd.nextInt(100) <= castingChance)
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
						return;
					}
				}
			}

			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
			if (_selfAnalysis.isMage && !_actor.isMuted())
			{
				// mages stay a bit further away if not muted or low mana
				if ((_actor.getMaxMp() / 3) < _actor.getStatus().getCurrentMp())
				{
					range = _selfAnalysis.maxCastRange;
					if (dist2 < range * range) // don't move backwards here
						return;
				}
			}
			// healers do not even follow
			if (_selfAnalysis.isHealer)
				return;

			if (_mostHatedAnalysis.character.isMoving())
				range -= 100;
			if (range < 5)
				range = 5;
			moveToPawn(_mostHatedAnalysis.character, range);
			return;
		}

/*		if (Rnd.nextInt(100) <= 33) // check it once per 3 seconds
		{
			for (L2Object nearby : _actor.getKnownList().getKnownCharactersInRadius(10))
			{
				if (nearby instanceof L2Attackable && nearby != _mostHatedAnalysis.character)
				{
					int diffx = Rnd.get(combinedCollision, combinedCollision + 40);
					if (Rnd.get(10) < 5)
						diffx = -diffx;
					int diffy = Rnd.get(combinedCollision, combinedCollision + 40);
					if (Rnd.get(10) < 5)
						diffy = -diffy;
					moveTo(_mostHatedAnalysis.character.getX() + diffx, _mostHatedAnalysis.character.getY() + diffy, _mostHatedAnalysis.character.getZ());
					return;
				}
			}
		}
*/
		// Calculate a new attack timeout. 
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		// check for close combat skills && heal/buff skills

		if (!_mostHatedAnalysis.isCanceled)
		{
			for (L2Skill sk : _selfAnalysis.cancelSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (dist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= 8)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					_mostHatedAnalysis.isCanceled = true;
					return;
				}
			}
		}
		if (_selfAnalysis.lastDebuffTick + 60 < GameTimeController.getGameTicks())
		{
			for (L2Skill sk : _selfAnalysis.debuffSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (dist2 > castRange * castRange))
					continue;
				int chance = 5;
				if (_selfAnalysis.isFighter && _mostHatedAnalysis.isMage)
					chance = 3;
				if (_selfAnalysis.isFighter && _mostHatedAnalysis.isArcher)
					chance = 3;
				if (_selfAnalysis.isMage && !_mostHatedAnalysis.isMage)
					chance = 4;
				if (_selfAnalysis.isHealer)
					chance = 12;
				if (_mostHatedAnalysis.isMagicResistant)
					chance /= 2;
				if (sk.getCastRange() < 200)
					chance += 3;
				if (Rnd.nextInt(100) <= chance)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					_selfAnalysis.lastDebuffTick = GameTimeController.getGameTicks();
					return;
				}
			}
		}
		if (!_mostHatedAnalysis.character.isMuted() && (_mostHatedAnalysis.isMage || _mostHatedAnalysis.isBalanced))
		{
			for (L2Skill sk : _selfAnalysis.muteSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (dist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= 7)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					return;
				}
			}
		}
		if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isMuted()
				&& (_secondMostHatedAnalysis.isMage || _secondMostHatedAnalysis.isBalanced))
		{
			double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
			for (L2Skill sk : _selfAnalysis.muteSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (secondHatedDist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= 3)
				{
					_actor.setTarget(_secondMostHatedAnalysis.character);
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(_mostHatedAnalysis.character);
					return;
				}
			}
		}
		if (!_mostHatedAnalysis.character.isSleeping() && _selfAnalysis.isHealer)
		{
			for (L2Skill sk : _selfAnalysis.sleepSkills)
			{
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk) || (dist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= 10)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
					return;
				}
			}
		}
		if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isSleeping())
		{
			double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
			for (L2Skill sk : _selfAnalysis.sleepSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (secondHatedDist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 10 : 4))
				{
					_actor.setTarget(_secondMostHatedAnalysis.character);
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(_mostHatedAnalysis.character);
					return;
				}
			}
		}
		if (!_mostHatedAnalysis.character.isRooted() && _mostHatedAnalysis.isFighter && !_selfAnalysis.isFighter)
		{
			for (L2Skill sk : _selfAnalysis.rootSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (dist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 10 : 4))
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					return;
				}
			}
		}
		if (!_mostHatedAnalysis.character.isAttackingDisabled())
		{
			for (L2Skill sk : _selfAnalysis.generalDisablers)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getCastRange() + combinedCollision;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
						|| (dist2 > castRange * castRange))
					continue;
				if (Rnd.nextInt(100) <= ((sk.getCastRange() < 200) ? 10 : 7))
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					return;
				}
			}
		}
		if (_actor.getStatus().getCurrentHp() < _actor.getMaxHp() * (_selfAnalysis.isHealer ? 0.7 : 0.4))
		{
			for (L2Skill sk : _selfAnalysis.healSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk))
					continue;
				int chance = (_selfAnalysis.isHealer ? 15 : 7);
				if (_mostHatedAnalysis.character.isAttackingDisabled())
					chance += 10;
				if (_secondMostHatedAnalysis.character == null || _secondMostHatedAnalysis.character.isAttackingDisabled())
					chance += 10;
				if (Rnd.nextInt(100) <= chance)
				{
					_actor.setTarget(_actor);
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(_mostHatedAnalysis.character);
					return;
				}
			}
		}
		for (L2Skill sk : _selfAnalysis.generalSkills)
		{
			if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
				continue;
			int castRange = sk.getCastRange() + combinedCollision;
			if (_actor.isSkillDisabled(sk.getId()) || _actor.getStatus().getCurrentMp() < _actor.getStat().getMpConsume(sk)
					|| (dist2 > castRange * castRange))
				continue;

			// chance decision for launching general skills in melee fight
			// close range skills should be higher, long range lower
			int castingChance = 5;
			if (_selfAnalysis.isMage || _selfAnalysis.isHealer)
			{
				if (sk.getCastRange() < 200)
					castingChance = 35;
				else
					castingChance = 25; // mages
			}
			if (_selfAnalysis.isBalanced)
			{
				if (sk.getCastRange() < 200)
					castingChance = 12;
				else
				{
					if (_mostHatedAnalysis.isMage) // hit mages
						castingChance = 2;
					else
						castingChance = 5;
				}
			}
			if (_selfAnalysis.isFighter)
			{
				if (sk.getCastRange() < 200)
					castingChance = 12;
				else
				{
					if (_mostHatedAnalysis.isMage)
						castingChance = 1;
					else
						castingChance = 3;
				}
			}

			if (Rnd.nextInt(100) <= castingChance)
			{
				clientStopMoving(null);
				_accessor.doCast(sk);
				return;
			}
		}

		// Finally, physical attacks
		if (!_selfAnalysis.isHealer)
		{
			clientStopMoving(null);
			_accessor.doAttack(_mostHatedAnalysis.character);
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
		try {
			if (getIntention() == AI_INTENTION_ACTIVE) 
				thinkActive(); 
			else if (getIntention() == AI_INTENTION_ATTACK) 
				thinkAttack(); 		
			else if(getIntention() == AI_INTENTION_IDLE)
				thinkIdle();
		} finally {
			_thinking = false;
		}
	}

	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		notifyFaction(attacker);
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
			_globalAggro = 0;

		// Add the attacker to the _aggroList of the actor
		if (!_actor.isCoreAIDisabled())
			((L2Attackable) _actor).addDamageHate(attacker, 0, 1);

		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning())
			_actor.setRunning();

		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK && !_actor.isCoreAIDisabled())
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		else if (((L2Attackable) _actor).getMostHated() != getAttackTarget() && !_actor.isCoreAIDisabled())
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		else if (getIntention() != AI_INTENTION_INTERACT && _actor.isCoreAIDisabled())
			setIntention(CtrlIntention.AI_INTENTION_INTERACT, attacker);

		super.onEvtAttacked(attacker);
		if(_actor instanceof L2MonsterInstance) {
			L2MonsterInstance leader = (L2MonsterInstance)_actor;
			if(_actor instanceof L2MinionInstance) {
				L2MinionInstance minion = (L2MinionInstance)_actor;
				leader = minion.getLeader();
				if(leader!=null && !leader.isInCombat())
					leader.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED,attacker);
			}
			if(leader.hasMinions())
				leader.callMinionsToAssist(attacker);
				
		}
	}

	private void notifyFaction(L2Character attacker)
	{
		try
		{
			String faction_id = ((L2NpcInstance) _actor).getFactionId();
			Collection <L2Object> objectsCollect = _actor.getKnownList().getKnownObjects().values();
			for (L2Object obj : objectsCollect)
			{
				if (obj instanceof L2NpcInstance)
				{
					
					L2NpcInstance npc = (L2NpcInstance) obj;
					//Handle SevenSigns mob Factions
					boolean sevenSignFaction = false;
					if (!faction_id.equals(npc.getFactionId()) && !sevenSignFaction)
						continue;
					if (_actor.isInsideRadius(npc, npc.getFactionRange() + npc.getTemplate().getCollisionRadius()*2, true, false) && npc.getAI() != null)
					{

						if (Math.abs(attacker.getZ() - npc.getZ()) < 600 && (!npc.isInCombat()) && GeoData.getInstance().canSeeTarget(_actor, npc))
						{
							if (attacker instanceof L2PcInstance && attacker.isInParty() && attacker.getParty().isInDimensionalRift())
							{
								byte riftType = attacker.getParty().getDimensionalRift().getType();
								byte riftRoom = attacker.getParty().getDimensionalRift().getCurrentRoom();

								if (_actor instanceof L2RiftInvaderInstance)
								{
									DimensionalRiftRoom room = DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom);
									if (room != null && !room.checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
										continue;
								}
							}
							npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 1);

							if ((attacker instanceof L2PcInstance) || (attacker instanceof L2Summon))
							{
								L2PcInstance player = (attacker instanceof L2PcInstance) ? (L2PcInstance) attacker : ((L2Summon) attacker).getOwner();
								if (npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL) != null)
								{
									for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL))
										quest.notifyFactionCall(npc, (L2NpcInstance) _actor, player, (attacker instanceof L2Summon));
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		L2Attackable me = (L2Attackable) _actor;

		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!_actor.isRunning())
					_actor.setRunning();

				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}
	}

	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}

	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
}