package ru.catssoftware.gameserver.handler;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public interface ISkillHandler
{
	public static final Logger	_log	= Logger.getLogger(ISkillHandler.class);

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets);

	public L2SkillType[] getSkillIds();
}