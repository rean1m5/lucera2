package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class CPperHeal implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.COMBATPOINTPERCENTHEAL };

	public void useSkill(L2Character actChar, L2Skill skill, L2Character... targets)
	{
		SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF).useSkill(actChar, skill, targets);

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			double perCp = target.getMaxCp() * skill.getPower();
			double newCp = target.getStatus().getCurrentCp() + perCp;

			if (newCp > target.getMaxCp())
				perCp = target.getMaxCp() - target.getStatus().getCurrentCp();

			target.getStatus().setCurrentCp(target.getStatus().getCurrentCp() + perCp);
			StatusUpdate sucp = new StatusUpdate(target.getObjectId());
			sucp.addAttribute(StatusUpdate.CUR_CP, (int) target.getStatus().getCurrentCp());
			target.sendPacket(sucp);
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
			sm.addNumber((int) perCp);
			target.sendPacket(sm);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}