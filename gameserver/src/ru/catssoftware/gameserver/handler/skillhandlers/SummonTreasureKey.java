package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

public class SummonTreasureKey implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SUMMON_TREASURE_KEY };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar.isPlayer()))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		int item_id = 0;

		switch (skill.getLevel())
		{
			case 1:
			{
				item_id = Rnd.get(6667, 6669);
				break;
			}
			case 2:
			{
				item_id = Rnd.get(6668, 6670);
				break;
			}
			case 3:
			{
				item_id = Rnd.get(6669, 6671);
				break;
			}
			case 4:
			{
				item_id = Rnd.get(6670, 6672);
				break;
			}
		}
		if (item_id != 0)
			player.addItem("Skill", item_id, Rnd.get(2, 3), player, false);
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}