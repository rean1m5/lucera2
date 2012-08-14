package ru.catssoftware.gameserver.handler;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2CubicInstance;

public interface ICubicSkillHandler extends ISkillHandler
{
	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Character... targets);
}