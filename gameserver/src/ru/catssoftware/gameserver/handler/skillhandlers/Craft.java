package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Craft implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.COMMON_CRAFT, L2SkillType.DWARVEN_CRAFT };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar.isPlayer()))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		if (player.getPrivateStoreType() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_CREATED_WHILE_ENGAGED_IN_TRADING);
			return;
		}
		RecipeController.getInstance().requestBookOpen(player, (skill.getSkillType() == L2SkillType.DWARVEN_CRAFT));
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}