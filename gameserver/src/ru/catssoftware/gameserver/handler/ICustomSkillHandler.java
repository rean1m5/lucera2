package ru.catssoftware.gameserver.handler;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;

public interface ICustomSkillHandler {
	public int [] getSkills();
	public void useSkill(L2Character caster, L2Skill skill, L2Character...targets);
}
