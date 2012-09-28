/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2SummonAI;
import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Attackable.AggroInfo;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.model.actor.knownlist.SummonKnownList;
import ru.catssoftware.gameserver.model.actor.stat.SummonStat;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.model.itemcontainer.PetInventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.network.serverpackets.EffectInfoPacket.EffectInfoPacketList;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Weapon;

public abstract class L2Summon extends L2PlayableInstance
{
	public static final int		SIEGE_GOLEM_ID			= 14737;
	public static final int		HOG_CANNON_ID			= 14768;
	public static final int		SWOOP_CANNON_ID			= 14839;

	private L2PcInstance		_owner;
	private int					_attackRange			= 36;											//Melee range
	private boolean				_follow					= true;
	private boolean				_previousFollowStatus	= true;

	private int					_chargedSoulShot;
	private int					_chargedSpiritShot;

	// TODO: currently, all servitors use 1 shot.  However, this value should vary depending on the servitor template (id and level)!
	private int					_soulShotsPerHit		= 1;
	private int					_spiritShotsPerHit		= 1;

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		public L2Summon getSummon()
		{
			return L2Summon.this;
		}

		public boolean isAutoFollow()
		{
			return L2Summon.this.getFollowStatus();
		}

		public void doPickupItem(L2Object object)
		{
			L2Summon.this.doPickupItem(object);
		}
	}

	public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status

		setInstanceId(owner.getInstanceId()); // set instance to owners one
		_event = owner.getGameEvent();
		_showSummonAnimation = true;
		_owner = owner;
		_ai = new L2SummonAI(new L2Summon.AIAccessor());

		getPosition().setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setFollowStatus(true);
		setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
		// if someone comes into range now, the animation shouldnt show any more
		broadcastFullInfoImpl(0);
		getOwner().broadcastRelationChanged();
	}

	@Override
	public final SummonKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new SummonKnownList(this);

		return (SummonKnownList) _knownList;
	}

	@Override
	public SummonStat getStat()
	{
		if (_stat == null)
			_stat = new SummonStat(this);

		return (SummonStat) _stat;
	}

	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2SummonAI(new L2Summon.AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	// this defines the action buttons, 1 for Summon, 2 for Pets
	public abstract int getSummonType();

	/**
	 * @return Returns the mountable.
	 */
	public boolean isMountable()
	{
		return false;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (player == _owner && player.getTarget() == this)
		{
			player.sendPacket(new PetStatusShow(this));
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (player.getTarget() != this)
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			//sends HP/MP status of the summon to other characters
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
		}
		else if (player.getTarget() == this)
		{
			if (isAutoAttackable(player))
			{
				if (Config.GEODATA)
				{
					if (player.canSee(this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					player.onActionRequest();
				}
			}
			else
			{
				// This Action Failed packet avoids player getting stuck when clicking three or more times
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (Config.GEODATA)
				{
					if (player.canSee(this))
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
				else
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
			}
		}

	}

	public long getExpForThisLevel()
	{
		if (getLevel() >= Experience.LEVEL.length)
		{
			return 0;
		}
		return Experience.LEVEL[getLevel()];
	}

	public long getExpForNextLevel()
	{
		if (getLevel() >= Experience.LEVEL.length - 1)
		{
			return 0;
		}
		return Experience.LEVEL[getLevel() + 1];
	}

	public final int getKarma()
	{
		return getOwner() != null ? getOwner().getKarma() : 0;
	}

	public final L2PcInstance getOwner()
	{
		return _owner;
	}

	public final int getNpcId()
	{
		return getTemplate().getNpcId();
	}

	public final int getSoulShotsPerHit()
	{
		return _soulShotsPerHit;
	}

	public final int getSpiritShotsPerHit()
	{
		return _spiritShotsPerHit;
	}

	public void setChargedSoulShot(int shotType)
	{
		_chargedSoulShot = shotType;
		if (getOwner()!=null)
			if ((shotType==L2ItemInstance.CHARGED_NONE)&&(getOwner().getAutoSoulShot().size()>0))
				getOwner().rechargeAutoSoulShot(true, false, true, false);
	}

	public void setChargedSpiritShot(int shotType)
	{
		_chargedSpiritShot = shotType;
		if (getOwner()!=null)
			if ((shotType==L2ItemInstance.CHARGED_NONE)&&(getOwner().getAutoSoulShot().size()>0))
				getOwner().rechargeAutoSoulShot(false, true, true, true);		
	}

	public void followOwner()
	{
		setFollowStatus(true);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		L2PcInstance owner = getOwner();

		if (owner != null)
		{
			for (L2Character TgMob : getKnownList().getKnownCharacters())
			{
				// get the mobs which have aggro on the this instance
				if (TgMob instanceof L2Attackable)
				{
					if (TgMob.isDead())
						continue;

					AggroInfo info = ((L2Attackable) TgMob).getAggroListRP().get(this);
					if (info != null)
						((L2Attackable) TgMob).addDamageHate(owner, info._damage, info._hate);
				}
			}
			if(killer instanceof L2PcInstance) {
				L2PcInstance pk = killer.getActingPlayer();
				pk.onKillUpdatePvPKarma(this);
			}
			
		}

		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	public boolean doDie(L2Character killer, boolean decayed)
	{
		if (!super.doDie(killer))
			return false;
		if (!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this);
		}
		if(killer instanceof L2PcInstance) {
			L2PcInstance pk = killer.getActingPlayer();
			System.out.println("Update karma1");
			pk.onKillUpdatePvPKarma(this);
		}
		return true;
	}

	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}

	@Override
	public final void broadcastStatusUpdateImpl()
	{
		getOwner().sendPacket(new PetStatusUpdate(this));


		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp());
		broadcastPacket(su);
	}

	@Override
	public final void updateEffectIconsImpl()
	{
		final EffectInfoPacketList list = new EffectInfoPacketList(this);

		final L2Party party = getParty();

		if (party != null)
			party.broadcastToPartyMembers(new PartySpelled(list));
		else
			getOwner().sendPacket(new PartySpelled(list));
	}

	public void deleteMe(L2PcInstance owner)
	{
		getAI().stopFollow();
		owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));
		//pet will be deleted along with all his items
		if (getInventory() != null)
			getInventory().destroyAllItems("pet deleted", getOwner(), this);

		stopAllEffects();
		getStatus().stopHpMpRegeneration();
		L2WorldRegion oldRegion = getWorldRegion();
		decayMe();
		if (oldRegion != null)
			oldRegion.removeFromZones(this);

		getKnownList().removeAllKnownObjects();
		owner.setPet(null);
		setTarget(null);
	}

	public void unSummon(L2PcInstance owner)
	{
		if (isVisible())
		{
			stopAllEffects();

			getAI().stopFollow();
			owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));

			store();

			giveAllToOwner();

			stopAllEffects();
			getStatus().stopHpMpRegeneration();
			L2WorldRegion oldRegion = getWorldRegion();
			decayMe();
			if (oldRegion != null)
				oldRegion.removeFromZones(this);

			getKnownList().removeAllKnownObjects();
			owner.setPet(null);
			setTarget(null);
		}
	}

	public int getAttackRange()
	{
		return _attackRange;
	}

	public void setAttackRange(int range)
	{
		if (range < 36)
			range = 36;
		_attackRange = range;
	}

	public void setFollowStatus(boolean state)
	{
		_follow = state;
		if (_follow)
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
		else
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
	}

	public boolean getFollowStatus()
	{
		return _follow;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(_owner==null)
			return false;
		return _owner.isAutoAttackable(attacker);
	}

	public final boolean checkStartAttacking()
	{
		return isStunned() || isSleeping() || isImmobileUntilAttacked() || isParalyzed() || isPetrified() || isFallsdown() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}

	public int getChargedSoulShot()
	{
		return _chargedSoulShot;
	}

	public int getChargedSpiritShot()
	{
		return _chargedSpiritShot;
	}

	public int getControlItemId()
	{
		return 0;
	}

	public L2Weapon getActiveWeapon()
	{
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return null;
	}

	public int getCurrentLoad()
	{
		return 0;
	}

	public int getMaxLoad()
	{
		return 0;
	}

	/**
	 * @param object
	 */
	protected void doPickupItem(L2Object object)
	{
	}

	public void giveAllToOwner()
	{
	}

	public void store()
	{
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	/**
	 * Return true if the L2Summon is invulnerable or if the summoner is in spawn protection.<BR><BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || getOwner().getProtection() > GameTimeController.getGameTicks();
	}

	public abstract int getCurrentFed();

	public abstract int getMaxFed();

	/**
	 * Return the L2Party object of its L2PcInstance owner or null.<BR><BR>
	 */
	@Override
	public L2Party getParty()
	{
		if (_owner == null)
			return null;

		return _owner.getParty();
	}

	/**
	 * Return true if the L2Character has a Party in progress.<BR><BR>
	 */
	@Override
	public boolean isInParty()
	{
		if (_owner == null)
			return false;

		return _owner.getParty() != null;
	}

	/**
	 * Check if the active L2Skill can be casted.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Check if the target is correct </li>
	 * <li>Check if the target is in the skill cast range </li>
	 * <li>Check if the summon owns enough HP and MP to cast the skill </li>
	 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
	 * <li>Check if the skill is active </li><BR><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
	 *
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 * 
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null || isDead())
			return;

		// Check if the skill is active
		if (skill.isPassive())
			return;

		//************************************* Check Casting in Progress *******************************************

		// If a skill is currently being used
		if (isCastingNow())
			return;

		//************************************* Check Target *******************************************

		// Get the target for the skill
		L2Character target = null;

		switch (skill.getTargetType())
		{
			// OWNER_PET should be cast even if no target has been found
			case TARGET_OWNER_PET:
				target = getOwner();
				break;
			// PARTY, AURA, SELF should be cast even if no target has been found
			case TARGET_PARTY:
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
				// Get the first target of the list
				target = skill.getFirstOfTargetList(this);
				break;
		}

		// Check the validity of the target
		if (target == null)
		{
			if (getOwner() != null)
				getOwner().sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return;
		}

		//************************************* Check skill availability *******************************************

		// Check if this skill is enabled (e.g. reuse time)
		if (isSkillDisabled(skill.getId()))
		{
			if (getOwner() != null)
				getOwner().sendPacket(new SystemMessage(SystemMessageId.S1).addString("Умение не может быть использовано")); 
			return;
		}

		//************************************* Check Consumables *******************************************
		if (skill.getItemConsume() > 0 && getOwner().getInventory() != null)
		{
			L2ItemInstance requiredItems = getOwner().getInventory().getItemByItemId(skill.getItemConsumeId());
			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Send a System Message to the caster
				getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
		}

		// Check if the summon has enough MP
		if (getStatus().getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			// Send a System Message to the caster
			if (getOwner() != null)
				getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			return;
		}

		// Check if the summon has enough HP
		if (getStatus().getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			if (getOwner() != null)
				getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			return;
		}

		//************************************* Check Summon State *******************************************

		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if (isInsidePeaceZone(this, target) && getOwner() != null && (!getOwner().allowPeaceAttack()))
			{
				if (!isInFunEvent() || !target.isInFunEvent())
				{
					// If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
					sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
					return;
				}
			}

			if (getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Check if the target is attackable
			if (target instanceof L2DoorInstance)
			{
				if (!((L2DoorInstance) target).isAttackable(getOwner()))
					return;
			}
			else
			{
				if (!target.isAttackable() && getOwner() != null && (!getOwner().allowPeaceAttack()))
					return;

				// Check if a Forced ATTACK is in progress on non-attackable target
				if (!target.isAutoAttackable(this) && !forceUse)
				{
					switch (skill.getTargetType())
					{
						case TARGET_AURA:
						case TARGET_FRONT_AURA:
						case TARGET_BEHIND_AURA:
						case TARGET_CLAN:
						case TARGET_ALLY:
						case TARGET_PARTY:
						case TARGET_SELF:
							break;
						default:
							return;
					}
				}
			}
		}

		getOwner().setCurrentPetSkill(skill, forceUse, dontMove);
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}

	@Override
	public void setIsImmobilized(boolean value)
	{
		super.setIsImmobilized(value);

		if (value)
		{
			_previousFollowStatus = getFollowStatus();
			// if immobilized temporarly disable follow mode
			if (_previousFollowStatus)
				setFollowStatus(false);
		}
		else
			// if not more immobilized restore previous follow mode
			setFollowStatus(_previousFollowStatus);
	}

	public void setOwner(L2PcInstance newOwner)
	{
		_owner = newOwner;
	}

	public void reduceCurrentHp(int damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);

		if (isDOT)
			return;

		SystemMessage sm;
		if (this instanceof L2SummonInstance)
			sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		else
			sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1);

		sm.addCharName(attacker);
		sm.addNumber(damage);
		getOwner().sendPacket(sm);
	}

	@Override
	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isBetrayed();
	}

	/**
	 * Servitors' skills automatically change their level based on the servitor's level.
	 * Until level 70, the servitor gets 1 lv of skill per 10 levels. After that, it is 1 
	 * skill level per 5 servitor levels.  If the resulting skill level doesn't exist use 
	 * the max that does exist!
	 * 
	 * @see ru.catssoftware.gameserver.model.L2Character#doCast(ru.catssoftware.gameserver.model.L2Skill)
	 */
	@Override
	public void doCast(L2Skill skill)
	{
		if (PetDataTable.isImprovedBaby(getNpcId()))
		{
			super.doCast(skill);
			return;
		}

		if (!_owner.checkPvpSkill(getTarget(), skill))
		{
			_owner.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			_owner.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			int petLevel = getLevel();
			int skillLevel = petLevel / 10;
			if (petLevel >= 70)
				skillLevel += (petLevel - 65) / 10;

			// adjust the level for servitors less than lv 10
			if (skillLevel < 1)
				skillLevel = 1;

			L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(), skillLevel);
			if (skillToCast != null)
				super.doCast(skillToCast);
			else
				super.doCast(skill);
		}
	}

	@Override
	public final L2PcInstance getActingPlayer()
	{
		return getOwner();
	}

	@Override
	public final L2Summon getActingSummon()
	{
		return this;
	}

	@Override
	public boolean isInCombat()
	{
		return getOwner().isInCombat();
	}

	public int getWeapon()
	{
		return 0;
	}

	public int getArmor()
	{
		return 0;
	}

	public int getPetSpeed()
	{
		return getTemplate().getBaseRunSpd();
	}

	public boolean isHungry()
	{
		return false;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
		broadcastFullInfoImpl(1);
	}

	public void broadcastFullInfoImpl(int val)
	{
		if (getOwner() == null)
			return;

		getOwner().sendPacket(new PetInfo(this, val));
		getOwner().sendPacket(new PetStatusUpdate(this));

		broadcastPacket(new NpcInfo(this, val));

		updateEffectIcons();
	}

	@Override
	protected void doAttack(L2Character target)
	{
		if (isInsidePeaceZone(this, target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		super.doAttack(target);
	}

	@Override
	public L2Summon getSummon()
	{
		return this;
	}

	@Override
	public boolean isSummon()
	{
		return true;
	}
}