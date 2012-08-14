package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class CombatPointHeal implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.COMBATPOINTHEAL };

	public void useSkill(L2Character actChar, L2Skill skill, L2Character... targets)
	{
		SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF).useSkill(actChar, skill, targets);

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			double cp = skill.getPower();

			//from CT2 u will receive exact CP, u can't go over it, if u have full CP and u get CP buff, u will receive 0CP restored message
			if ((target.getStatus().getCurrentCp() + cp) >= target.getMaxCp())
				cp = target.getMaxCp() - target.getStatus().getCurrentCp();

			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
			sm.addNumber((int) cp);
			target.sendPacket(sm);
			target.getStatus().setCurrentCp(cp + target.getStatus().getCurrentCp());
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_CP, (int) target.getStatus().getCurrentCp());
			target.sendPacket(sump);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}