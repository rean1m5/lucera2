package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class CustomSkills implements ISkillHandler {

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill,
			L2Character... targets) {
		SkillHandler.getInstance().handleCustomSkill(skill, activeChar, targets);
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return new L2SkillType[] { L2SkillType.CUSTOM };
	}

}
