package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2GuardInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeFlagInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Heal implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	=
	{
		L2SkillType.HEAL,
		L2SkillType.HEAL_PERCENT,
		L2SkillType.HEAL_STATIC,
		L2SkillType.HEAL_MOB
	};
	
	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF).useSkill(activeChar, skill, targets);

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2PcInstance player = null;
		boolean consumeSoul = true;

		if (activeChar.isPlayer())
			player = (L2PcInstance) activeChar;

		for (L2Character target : targets)
		{
			if (target == null)
				continue;
			//We should not heal if char is dead
			if (target.isDead())
				continue;
			if(Math.abs(target.getZ()-activeChar.getZ())>200)
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target.isPlayer() && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;
				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}
			if((target.isBoss() || target.isGuard()) && activeChar.getPlayer()!=null )
				activeChar.getPlayer().updatePvPStatus();
			double hp = skill.getPower();

			if (skill.getSkillType() == L2SkillType.HEAL_PERCENT)
			{
				hp = target.getMaxHp() * hp / 100.0;
			}
			else if (skill.getSkillType() != L2SkillType.HEAL_STATIC)
			{
				if (weaponInst != null)
				{
					if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
					{
						hp *= 1.5;
						if (consumeSoul)
							weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
						consumeSoul = false;
					}
					else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
					{
						hp *= 1.3;
						if (consumeSoul)
							weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
						consumeSoul = false;
					}
				}
				else if (activeChar instanceof L2Summon)
				{
					L2Summon activeSummon = (L2Summon) activeChar;
					if (activeSummon != null)
					{
						if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
						{
							hp *= 1.5;
							if (consumeSoul)
								activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
							consumeSoul = false;
						}
						else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
						{
							hp *= 1.3;
							if (consumeSoul)
								activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
							consumeSoul = false;
						}
					}
				}
				else if (activeChar instanceof L2NpcInstance)
				{
					if (((L2NpcInstance) activeChar).isUsingShot(false))
						hp *= 1.5;
				}
			}

			if (target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance)
			{
				hp = 0;
			}
			else
			{
				if (skill.getSkillType() == L2SkillType.HEAL_STATIC)
				{
					hp = skill.getPower();
				}
				else if (skill.getSkillType() != L2SkillType.HEAL_PERCENT)
				{
					hp *= target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
					// Healer proficiency (since CT1)
					hp *= activeChar.calcStat(Stats.HEAL_PROFICIENCY, 100, null, null) / 100;
					// Extra bonus (since CT1.5)
					if (!skill.isPotion())
						hp += target.calcStat(Stats.HEAL_STATIC_BONUS, 0, null, null);
				}
			}

			//from CT2 u will receive exact HP, u can't go over it, if u have full HP and u get HP buff, u will receive 0HP restored message
			if ((target.getStatus().getCurrentHp() + hp) >= target.getMaxHp())
				hp = target.getMaxHp() - target.getStatus().getCurrentHp();

			if (hp > 0)
			{
				target.getStatus().increaseHp(hp);
				
				target.setLastHealAmount((int) hp);
				StatusUpdate su = new StatusUpdate(target.getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) target.getStatus().getCurrentHp());
				target.sendPacket(su);
				L2PcInstance pc = target.getPlayer();
				if(pc!=null && pc.getPvpFlag()>0 && activeChar.isPlayer())
					activeChar.getPlayer().updatePvPStatus();
			}
			
			if (target.isPlayer())
			{
				
				if (skill.getId() == 4051)
				{
					target.sendPacket(SystemMessageId.REJUVENATING_HP);
				}
				else
				{
					if (activeChar.isPlayer() && activeChar != target)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1);
						sm.addString(activeChar.getName());
						sm.addNumber((int) hp);
						target.sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_HP_RESTORED);
						sm.addNumber((int) hp);
						target.sendPacket(sm);
					}
				}
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}