package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

 /*
 * @author: m095
 * @team: L2CatsSoftware
 */

public class ZakenMove implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	=
	{
		L2SkillType.ZAKEN_MOVE
	};

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		int chance = Rnd.get(100);
		if (skill.getId() == 4216 && chance < 15) // One target
			return;
		if (skill.getId() == 4217 && chance < 20) // Mass target
			return;
		try
		{
			for (L2Character target : targets)
			{
				if (!(target instanceof L2Character))
					continue;
				int mode = (Rnd.get(14) + 1);
				switch (mode)
				{
					case 1:
						target.teleToLocation(55299,219120,-2952, true);
						break;
					case 2:
						target.teleToLocation(56363,218043,-2952, true);
						break;
					case 3:
						target.teleToLocation(54245,220162,-2952, true);
						break;
					case 4:
						target.teleToLocation(56289,220126,-2952, true);
						break;
					case 5:
						target.teleToLocation(55299,219120,-3224, true);
						break;
					case 6:
						target.teleToLocation(56363,218043,-3224, true);
						break;
					case 7:
						target.teleToLocation(54245,220162,-3224, true);
						break;
					case 8:
						target.teleToLocation(56289,220126,-3224, true);
						break;
					case 9:
						target.teleToLocation(55299,219120,-3496, true);
						break;
					case 10:
						target.teleToLocation(56363,218043,-3496, true);
						break;
					case 11:
						target.teleToLocation(54245,220162,-3496, true);
						break;
					case 12:
						target.teleToLocation(56289,220126,-3496, true);
						break;
					default:
						target.teleToLocation(53930,217760,-2944, true);
						break;
				}
			}
		}
		catch (Throwable e)
		{
			_log.error("Can't move Zaken: " + e);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}