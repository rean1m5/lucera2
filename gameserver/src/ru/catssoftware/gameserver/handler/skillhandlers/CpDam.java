package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class CpDam implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.CPDAM };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;

		boolean ss = false;
		boolean sps = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (weaponInst != null)
		{
			if (skill.isMagic())
			{
				if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
					bss = true;
				else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
					sps = true;
			}
			else if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
				ss = true;
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

			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isFakeDeath())
				target.stopFakeDeath(null);

			else if (target.isDead())
				continue;

			if (!Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				return;

			int damage = (int) (target.getStatus().getCurrentCp() * (1 - skill.getPower()));

			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			skill.getEffects(activeChar, target);
			activeChar.sendDamageMessage(target, damage, false, false, false);
			target.getStatus().setCurrentCp(target.getStatus().getCurrentCp() - damage);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
