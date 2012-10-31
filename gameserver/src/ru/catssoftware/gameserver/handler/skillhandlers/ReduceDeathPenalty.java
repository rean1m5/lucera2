package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

/*
 * @author Ro0TT
 * @date 30.10.2012
 */

public class ReduceDeathPenalty implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.DEATH_PENALTY };

	public void useSkill(L2Character actChar, L2Skill skill, L2Character... targets)
	{
		for (L2Character target : targets)
		{
			if (target == null || !target.isPlayer())
				continue;

			target.getPlayer().reduceDeathPenaltyBuffLevel();
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}