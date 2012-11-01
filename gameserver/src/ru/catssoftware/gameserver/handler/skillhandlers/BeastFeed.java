package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class BeastFeed implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.BEAST_FEED };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar.isPlayer()))
			return;
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}