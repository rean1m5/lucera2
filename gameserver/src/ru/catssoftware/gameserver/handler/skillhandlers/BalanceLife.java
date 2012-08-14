package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class BalanceLife implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.BALANCE_LIFE };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF).useSkill(activeChar, skill, targets);

		L2PcInstance player = null;
		if (activeChar instanceof L2PcInstance)
			player = (L2PcInstance) activeChar;

		double fullHP = 0;
		double currentHPs = 0;

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			// We should not heal if char is dead
			if (target.isDead())
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;

				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}

			fullHP += target.getMaxHp();
			currentHPs += target.getStatus().getCurrentHp();
		}

		double percentHP = currentHPs / fullHP;

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			double newHP = target.getMaxHp() * percentHP;
			double totalHeal = newHP - target.getStatus().getCurrentHp();

			target.getStatus().increaseHp(totalHeal);

			if (totalHeal > 0)
				target.setLastHealAmount((int) totalHeal);

			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getStatus().getCurrentHp());
			target.sendPacket(su);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}