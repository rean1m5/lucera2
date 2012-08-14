package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class StrSiegeAssault implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.STRSIEGEASSAULT };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		if (SiegeManager.checkIfOkToUseStriderSiegeAssault(player, false) || FortSiegeManager.checkIfOkToUseStriderSiegeAssault(player, false))
		{
			//TODO: damage calculation below is crap - needs rewrite
			int damage = 0;

			for (L2Character target : targets)
			{
				if (target == null)
					continue;

				L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
				if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isFakeDeath())
					target.stopFakeDeath(null);

				else if (target.isDead())
					continue;

				boolean dual = activeChar.isUsingDualWeapon();
				byte shld = Formulas.calcShldUse(activeChar, target);
				boolean crit = Formulas.calcCrit(activeChar, target, activeChar.getCriticalHit(target, skill));
				boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

				if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
					damage = 0;
				else
					damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, crit, dual, soul);

				if (damage > 0)
				{
					target.reduceCurrentHp(damage, activeChar, skill);
					if (soul && weapon != null)
						weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
					activeChar.sendDamageMessage(target, damage, false, false, false);
				}
				else
					activeChar.sendMessage(skill.getName() + " failed.");
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}