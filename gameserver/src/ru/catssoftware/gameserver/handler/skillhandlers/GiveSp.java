package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class GiveSp implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.GIVE_SP };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			int spToAdd = (int)skill.getPower();
			target.addExpAndSp(0, spToAdd);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}