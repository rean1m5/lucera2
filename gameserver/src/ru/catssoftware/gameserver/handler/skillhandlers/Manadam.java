package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Manadam implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.MANADAM };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;

		boolean ss = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
			else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (activeChar instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) activeChar;

			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
			else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
		}
		else if (activeChar instanceof L2NpcInstance)
		{
			bss = ((L2NpcInstance) activeChar).isUsingShot(false);
			ss = ((L2NpcInstance) activeChar).isUsingShot(true);
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;

			boolean acted = Formulas.calcMagicAffected(activeChar, target, skill);
			if (!acted || target.isInvul() || target.isPetrified())
				activeChar.sendPacket(SystemMessageId.MISSED_TARGET);
			else if (target instanceof L2ClanHallManagerInstance)
				activeChar.sendPacket(SystemMessageId.MISSED_TARGET);
			else
			{
				double damage = Formulas.calcManaDam(activeChar, target, skill, ss, bss);

				double mp = (damage > target.getStatus().getCurrentMp() ? target.getStatus().getCurrentMp() : damage);
				target.reduceCurrentMp(mp);
				if (damage > 0)
				{
					if (target.isSleeping())
						target.stopSleeping(null);
					if (target.isImmobileUntilAttacked())
						target.stopImmobileUntilAttacked(null);
				}

				if (target.isPlayer())
				{
					StatusUpdate sump = new StatusUpdate(target.getObjectId());
					sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getStatus().getCurrentMp());
					target.sendPacket(sump);

					SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_HAS_BEEN_DRAINED_BY_S1);
					sm.addCharName(activeChar);
					sm.addNumber((int) mp);
					target.sendPacket(sm);
				}

				if (activeChar.isPlayer())
				{
					SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1);
					sm2.addNumber((int) mp);
					activeChar.sendPacket(sm2);
				}
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}