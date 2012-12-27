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
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.knownlist.PlayableKnownList;
import ru.catssoftware.gameserver.model.actor.stat.PcStat;
import ru.catssoftware.gameserver.model.actor.stat.PlayableStat;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * This class represents all Playable characters in the world.<BR><BR>
 *
 * L2PlayableInstance :<BR><BR>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li><BR><BR>
 */
public abstract class L2PlayableInstance extends L2Character
{
	public static final L2PlayableInstance[] EMPTY_ARRAY = new L2PlayableInstance[0];

	private boolean	_isNoblesseBlessed	= false; // for Noblesse Blessing skill, restores buffs after death
	private boolean	_getCharmOfLuck		= false; // Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	private boolean	_isPhoenixBlessed	= false; // for Soul of The Phoenix or Salvation buffs
	private boolean	_isSilentMoving		= false; // Silent Move
	private boolean _protectionBlessing = false; // Blessed by Blessing of Protection

	/**
	 * Constructor of L2PlayableInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2PlayableInstance </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2PlayableInstance
	 *
	 */
	public L2PlayableInstance(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
	}

	@Override
	public PlayableKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new PlayableKnownList(this);

		return (PlayableKnownList) _knownList;
	}

	@Override
	public PlayableStat getStat()
	{
		if (_stat == null)
			_stat = new PlayableStat(this);

		return (PcStat) _stat;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (killer != null && killer.getPlayer() != null && isPlayer())
			killer.getPlayer().onKillUpdatePvPKarma(this);

		return true;
	}

	public boolean checkIfPvP(L2Character target)
	{
		if (target == null)
			return false; // Target is null
		if (target == this)
			return false; // Target is self
		if (!(target instanceof L2PlayableInstance))
			return false; // Target is not a L2PlayableInstance

		L2PcInstance player = null;
		if (isPlayer())
			player = (L2PcInstance) this;
		else if (this instanceof L2Summon)
			player = ((L2Summon) this).getOwner();

		if (player == null)
			return false; // Active player is null
		if (player.getKarma() != 0)
			return false; // Active player has karma

		L2PcInstance targetPlayer = null;
		if (target.isPlayer())
			targetPlayer = (L2PcInstance) target;
		else if (target instanceof L2Summon)
			targetPlayer = ((L2Summon) target).getOwner();

		if (targetPlayer == null)
			return false; // Target player is null
		if (targetPlayer == this)
			return false; // Target player is self

		return targetPlayer.getKarma() == 0;
	}

	/**
	 * Return true.<BR><BR>
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}

	// Support for Noblesse Blessing skill, where buffs are retained
	// after resurrect
	public final boolean isNoblesseBlessed()
	{
		getStat().calcStat(Stats.NOBLE_BLESS, 1, this, null);
		return _isNoblesseBlessed;
	}

	public final void setIsNoblesseBlessed(boolean value)
	{
		_isNoblesseBlessed = value;
	}

	public final void startNoblesseBlessing()
	{
		setIsNoblesseBlessed(true);
		updateAbnormalEffect();
	}

	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		else
			removeEffect(effect);

		setIsNoblesseBlessed(false);
		updateAbnormalEffect();
	}

	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed()
	{
		return _isPhoenixBlessed;
	}

	public final void setIsPhoenixBlessed(boolean value)
	{
		_isPhoenixBlessed = value;
	}

	public final void startPhoenixBlessing()
	{
		setIsPhoenixBlessed(true);
		updateAbnormalEffect();
	}

	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		else
			removeEffect(effect);

		setIsPhoenixBlessed(false);
		updateAbnormalEffect();
	}

	/**
	 * Set the Silent Moving mode Flag.<BR><BR>
	 */
	public void setSilentMoving(boolean flag)
	{
		_isSilentMoving = flag;
	}

	/**
	 * Return true if the Silent Moving mode is active.<BR><BR>
	 */
	public boolean isSilentMoving()
	{
		return _isSilentMoving;
	}

	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing()
	{
		return _protectionBlessing;
	}

	public final void setProtectionBlessing(boolean value)
	{
		_protectionBlessing = value;
	}

	public void startProtectionBlessing()
	{
		setProtectionBlessing(true);
		updateAbnormalEffect();
	}

	public void stopProtectionBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		else
			removeEffect(effect);

		setProtectionBlessing(false);
		updateAbnormalEffect();
	}
	//Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	public final boolean getCharmOfLuck()
	{
		return _getCharmOfLuck;
	}


	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// All messages are verified on retail
		L2PcInstance attOwner = getPlayer();
		L2PcInstance trgOwner = target.getPlayer();

		if (miss)
		{
			target.sendAvoidMessage(this);
			return;
		}

		if (pcrit)
			attOwner.sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));

		if (mcrit)
			sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC);

		SystemMessage sm=null;
		if (target.isInvul() && !target.isNpc())
		{
			sm = SystemMessageId.ATTACK_WAS_BLOCKED.getSystemMessage();
		}
		// Still needs retail verification
		else if (isPlayer())
		{
			sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
			sm.addNumber(damage);
		}
/*		else
		{
			sm = new SystemMessage(SystemMessageId.C1_GAVE_C2_DAMAGE_OF_S3);
			sm.addCharName(this);
			sm.addCharName(target);
			sm.addNumber(damage);
		} */
		if(sm!=null)
			attOwner.sendPacket(sm);
	}	
	
	@Override	
	public final void sendAvoidMessage(L2Character attacker)
	{
/*		SystemMessage sm = new SystemMessage(SystemMessageId.C1_EVADED_C2_ATTACK);
		sm.addCharName(this);
		sm.addCharName(attacker);
		getPlayer().sendPacket(sm); */
	}

	public final void setCharmOfLuck(boolean value)
	{
		_getCharmOfLuck = value;
	}

	public final void startCharmOfLuck()
	{
		setCharmOfLuck(true);
		updateAbnormalEffect();
	}

	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		else
			removeEffect(effect);

		setCharmOfLuck(false);
		updateAbnormalEffect();
	}

	public final void updateEffectIcons()
	{
		updateEffectIconsImpl();
	}
	
	public abstract void updateEffectIconsImpl();
	
	@Override
	public final void addEffect(L2Effect newEffect)
	{
		super.addEffect(newEffect);
		
		updateEffectIcons();
	}
	
	@Override
	public final void removeEffect(L2Effect effect)
	{
		super.removeEffect(effect);
		
		updateEffectIcons();
	}
	
	// ********* Jail Spawn System *************************
	private boolean	_isInJailMission		= false;
	private int		_jailPoints;
	private boolean	_hasCompletedMission	= false;

	public boolean hasCompletedMission()
	{
		if (getJailPoints() >= Config.REQUIRED_JAIL_POINTS)
			_hasCompletedMission = true;
		else
			_hasCompletedMission = false;

		return _hasCompletedMission;
	}

	/**
	 * sets jail points
	 *
	 * @param count
	 * @return
	 */
	public int setJailPoints(int count)
	{
		return _jailPoints = count;
	}

	/**
	 * resets jail points
	 */
	public void resetJailPoints()
	{
		setJailPoints(0);
	}

	/**
	 * returns player jail points
	 * @return
	 */
	public int getJailPoints()
	{
		return _jailPoints;
	}

	/**
	 * remove jail points
	 * @return
	 */
	public int removeJailPoints()
	{
		return setJailPoints(getJailPoints() - Config.POINTS_LOST_PER_DEATH);
	}

	/**
	 * sets in jail mission
	 * @param value
	 */
	public void setIsInJailMission(boolean value)
	{
		_isInJailMission = value;
	}

	/**
	 *returns jail mission state
	 */
	public boolean isInJailMission()
	{
		return _isInJailMission;
	}

	/**
	 * increases jail ponts
	 */
	public void incrementJailPoints()
	{
		setJailPoints(getJailPoints() + Config.POINTS_PER_KILL);
	}

	public boolean isGM()
	{
		L2PcInstance player = null;
		if (isPlayer())
			player = (L2PcInstance) this;

		return player.isGM();
	}

	@Override
	public boolean isPlayable()
	{
		return true;
	}

	@Override
	public L2PlayableInstance getPlayable()
	{
		return this;
	}
}