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
package ru.catssoftware.gameserver.model.actor.stat;

import ru.catssoftware.Config;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.skills.Stats;

public class PcStat extends PlayableStat
{
	private int	_oldMaxHp, _oldMaxMp, _oldMaxCp;

	private double _modifires[] = new double[8];
	public PcStat(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addExp(long value)
	{
		L2PcInstance activeChar = getActiveChar();

		if (!super.addExp(value))
			return false;
		// Set new karma
		if (!activeChar.isCursedWeaponEquipped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Zone.FLAG_PVP)))
		{
			int karmaLost = activeChar.calculateKarmaLost((int) value);
			if (karmaLost > 0)
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
		}
		activeChar.sendPacket(new UserInfo(activeChar));
		return true;
	}

	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		float ratioTakenByPet = 0;
		//Player is Gm and access level is below or equal to canGainExp and is in party, don't give Xp/Sp
		L2PcInstance activeChar = getActiveChar();
		
		
		float baseRates[] = new float[2];
		baseRates[0] = ((float)addToExp) / (activeChar.getPremiumService()>0?Config.PREMIUM_RATE_XP:Config.RATE_XP);
		baseRates[1] = ((float)addToSp) / (activeChar.getPremiumService()>0?Config.PREMIUM_RATE_SP:Config.RATE_SP);
		Long newVal = (Long)GameExtensionManager.getInstance().handleAction(activeChar, Action.PC_CALCEXP, addToExp,baseRates[0]);
		if(newVal!=null)
			addToExp = newVal;
		Integer intVal = (Integer)GameExtensionManager.getInstance().handleAction(activeChar, Action.PC_CALCSP, addToSp,baseRates[1]);
		if(intVal!=null)
			addToSp = intVal;
		
		// if this player has a pet that takes from the owner's Exp, give the pet Exp now
		if (activeChar.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			ratioTakenByPet = pet.getPetData().getOwnerExpTaken();

			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			if (ratioTakenByPet > 0 && !pet.isDead())
				pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			if (ratioTakenByPet > 1)
				ratioTakenByPet = 1;
			addToExp = (long) (addToExp * (1 - ratioTakenByPet));
			addToSp = (int) (addToSp * (1 - ratioTakenByPet));
		}

		if (!super.addExpAndSp(addToExp, addToSp))
			return false;
		if (addToExp == 0 && addToSp > 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_SP);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		else if (addToExp > 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
			sm.addNumber((int) addToExp);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		return true;
	}

	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		if (!super.removeExpAndSp(addToExp, addToSp))
			return false;
		L2PcInstance activeChar = getActiveChar();
		// Send a Server->Client System Message to the L2PcInstance
		SystemMessage sm = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
		sm.addNumber((int) addToExp);
		activeChar.sendPacket(sm);
		sm = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
		sm.addNumber(addToSp);
		activeChar.sendPacket(sm);
		return true;
	}

	@Override
	public final boolean addLevel(byte value)
	{
		if (getLevel() + value > Experience.MAX_LEVEL - 1)
			return false;

		boolean levelIncreased = super.addLevel(value);

		L2PcInstance activeChar = getActiveChar();

		if (levelIncreased)
		{
			QuestState qs = activeChar.getQuestState("255_Tutorial");
			if (qs != null)
				qs.getQuest().notifyEvent("CE40", null, activeChar);

			activeChar.getStatus().setCurrentCp(getMaxCp());
			activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 15));
			activeChar.sendPacket(SystemMessageId.YOU_INCREASED_YOUR_LEVEL);
		}

		activeChar.rewardSkills(); // Give Expertise skill of this level

		if (activeChar.getClan() != null)
		{
			activeChar.getClan().updateClanMember(activeChar);
			activeChar.getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(activeChar));
		}
		if (activeChar.isInParty())
			activeChar.getParty().recalculatePartyLevel(); // Recalculate the party level

		StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		activeChar.sendPacket(su);
		activeChar.refreshOverloaded();
		activeChar.refreshExpertisePenalty();
		activeChar.sendPacket(new UserInfo(activeChar));
		if(Config.CLASS_MASTER_POPUP) {
			IVoicedCommandHandler handler = VoicedCommandHandler.getInstance().getVoicedCommandHandler("classmaster");
			if(handler!=null) {
				int classLevel = activeChar.getClassLevel();
				if(classLevel==0 && getLevel()>=20) {
					handler.useVoicedCommand("classmaster", activeChar, "");
				} 
				else if(classLevel==1 && getLevel()>=40) {
					handler.useVoicedCommand("classmaster", activeChar, "");
				}
				else if(classLevel==2 && getLevel()>=76) {
					handler.useVoicedCommand("classmaster", activeChar, "");
				}
			}
		}
		activeChar.intemediateStore();
		activeChar.sendSkillList();
		GameExtensionManager.getInstance().handleAction(activeChar, Action.PC_LEVEL_UP);
		return levelIncreased;
	}

	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
			return false;
		L2PcInstance activeChar = getActiveChar();
		activeChar.sendPacket(new UserInfo(activeChar));
		return true;
	}

	@Override
	public final long getExpForLevel(int level)
	{
		return Experience.LEVEL[level];
	}

	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) _activeChar;
	}

	@Override
	public final long getExp()
	{
		L2PcInstance activeChar = getActiveChar();

		if (activeChar.isSubClassActive()) try {
			return activeChar.getSubclassByIndex(activeChar.getClassIndex()).getExp();
		} catch(NullPointerException ignored) {}
		return super.getExp();
			
	}

	@Override
	public final void setExp(long value)
	{
		L2PcInstance activeChar = getActiveChar();

		if (activeChar.isSubClassActive())
			activeChar.getSubclassByIndex(activeChar.getClassIndex()).setExp(value);
 		else
			super.setExp(value);
	}

	@Override
	public final byte getLevel()
	{
		L2PcInstance activeChar = getActiveChar();

		if (activeChar.isSubClassActive() ) try {
			return activeChar.getSubclassByIndex(activeChar.getClassIndex()).getLevel();
		} catch(NullPointerException npe) {}
		return super.getLevel();
	}

	@Override
	public final void setLevel(byte value)
	{
		L2PcInstance activeChar = getActiveChar();

		if (value > Experience.MAX_LEVEL - 1)
			value = Experience.MAX_LEVEL - 1;

		if (activeChar.isSubClassActive())
			activeChar.getSubclassByIndex(activeChar.getClassIndex()).setLevel(value);
		else
			super.setLevel(value);
		activeChar.sendSkillList();
	}

	@Override
	public final int getMaxHp()
	{
		L2PcInstance activeChar = getActiveChar();

		// Get the Max HP (base+modifier) of the L2PcInstance
		int val = super.getMaxHp();
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			if(activeChar.getStatus()==null)
				return val;

			// Launch a regen task if the new Max HP is higher than the old one
			if (activeChar.getStatus().getCurrentHp() != val)
				activeChar.getStatus().setCurrentHp(activeChar.getStatus().getCurrentHp()); // trigger start of regeneration
		}

		return val;
	}

	@Override
	public final int getMaxMp()
	{
		L2PcInstance activeChar = getActiveChar();

		// Get the Max MP (base+modifier) of the L2PcInstance
		int val = super.getMaxMp();

		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			if(activeChar.getStatus()==null)
				return val;

			// Launch a regen task if the new Max MP is higher than the old one
			if (activeChar.getStatus().getCurrentMp() != val)
				activeChar.getStatus().setCurrentMp(activeChar.getStatus().getCurrentMp()); // trigger start of regeneration
		}

		return val;
	}

	@Override
	public final int getMaxCp()
	{
		L2PcInstance activeChar = getActiveChar();

		// Get the Max CP (base+modifier) of the L2PcInstance
		int val = super.getMaxCp();

		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;

			if(activeChar.getStatus()==null)
				return val;
			// Launch a regen task if the new Max CP is higher than the old one
			if (activeChar.getStatus().getCurrentCp() != val)
				activeChar.getStatus().setCurrentCp(activeChar.getStatus().getCurrentCp()); // trigger start of regeneration
		}

		return val;
	}

	@Override
	public final int getSp()
	{
		L2PcInstance activeChar = getActiveChar();

		if (activeChar.isSubClassActive())
			return activeChar.getSubclassByIndex(activeChar.getClassIndex()).getSp();
		return super.getSp();
	}

	@Override
	public final void setSp(int value)
	{
		L2PcInstance activeChar = getActiveChar();

		if (activeChar.isSubClassActive())
			activeChar.getSubclassByIndex(activeChar.getClassIndex()).setSp(value);
		else
			super.setSp(value);
	}

	/**
	 * Return the RunSpeed (base+modifier) of the L2Character in function of the
	 * Armour Expertise Penalty.
	 */
	@Override
	public int getRunSpeed()
	{
		int val = super.getRunSpeed();

		val /= _activeChar.getArmourExpertisePenalty();

		// Apply max run speed cap.
		if (val > Config.MAX_RUN_SPEED && Config.MAX_RUN_SPEED > 0 && !getActiveChar().isGM())
			return Config.MAX_RUN_SPEED;

		return val;
	}

	/**
	 * Return the PAtk Speed (base+modifier) of the L2Character in function of
	 * the Armour Expertise Penalty.
	 */
	@Override
	public int getPAtkSpd()
	{
		int val = super.getPAtkSpd();

		val /= _activeChar.getArmourExpertisePenalty();
		val *= _modifires[0];
		if (val > Config.MAX_PATK_SPEED && Config.MAX_PATK_SPEED > 0 && !getActiveChar().isGM())
			return Config.MAX_PATK_SPEED;

		return val;
	}

	/**
	 * Return the MAtk Speed (base+modifier) of the L2Character in function of
	 * the Armour Expertise Penalty.
	 */
	@Override
	public int getMAtkSpd()
	{
		int val = super.getMAtkSpd();

		val /= _activeChar.getArmourExpertisePenalty();

		
		val *= _modifires[1]; 
		if (val > Config.MAX_MATK_SPEED && Config.MAX_MATK_SPEED > 0 && !getActiveChar().isGM())
			return Config.MAX_MATK_SPEED;

		
		return val;
	}

	/** Return the Attack Evasion rate (base+modifier) of the L2Character. */
	@Override
	public int getEvasionRate(L2Character target)
	{
		int val = super.getEvasionRate(target);

		if (val > Config.MAX_EVASION && Config.MAX_EVASION > 0 && !getActiveChar().isGM())
			return Config.MAX_EVASION;

		return val;
	}
	@Override
	public int getMAtk(L2Character target, L2Skill skill) {
		return (int)( super.getMAtk(target, skill) * _modifires[2]);
	}
	@Override
	public int getPAtk(L2Character target) {
		return (int)(super.getPAtk(target) * _modifires[3]);
	}
	@Override
	public int getMCriticalHit(L2Character target, L2Skill skill) {
		return (int)(super.getMCriticalHit(target, skill) * _modifires[4]);
	}
	@Override
	public int getCriticalHit(L2Character target, L2Skill skill) {
		return (int)(super.getCriticalHit(target, skill) * _modifires[5]);
	}
	@Override
	public double getBowCritRate() {
		return _modifires[7]>0?_modifires[7]:1;
	}
	public void resetModifiers() {
		try {
		_modifires[0] = Config.getCharModifier(getActiveChar(), Stats.POWER_ATTACK_SPEED);
		_modifires[1] = Config.getCharModifier(getActiveChar(), Stats.MAGIC_ATTACK_SPEED); 
		_modifires[2] = Config.getCharModifier(getActiveChar(), Stats.MAGIC_ATTACK);
		_modifires[3] = Config.getCharModifier(getActiveChar(), Stats.POWER_ATTACK);
		_modifires[4] = Config.getCharModifier(getActiveChar(), Stats.MCRITICAL_RATE);
		_modifires[5] = Config.getCharModifier(getActiveChar(), Stats.CRITICAL_RATE);
		_modifires[6] = Config.getCharModifier(getActiveChar(), Stats.CRITICAL_DAMAGE);
		_modifires[7] = Config.getCharModifier(getActiveChar(), Stats.BOW_CRIT_RATE);
		return;
		} catch(Exception e) { }
			for(int i=0;i<_modifires.length;i++) {
				if(_modifires[i]<1) _modifires[i] = 1;
		}
	}
	@Override
	public double getCriticalDmg(L2Character target, double init)
	{
		return super.getCriticalDmg(target, init) * _modifires[6];
	}
}