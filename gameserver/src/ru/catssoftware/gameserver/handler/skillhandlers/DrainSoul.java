package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

/**
 * @author _drunk_
 */
public class DrainSoul implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.DRAIN_SOUL };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}