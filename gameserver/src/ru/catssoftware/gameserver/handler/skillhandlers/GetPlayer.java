package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

/*
 * Mobs can teleport players to them
 */
public class GetPlayer implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.GET_PLAYER };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;

		for (L2Object target : targets)
		{
			if (target.isPlayer())
			{
				L2PcInstance trg = (L2PcInstance) target;

				if (trg.isAlikeDead())
					continue;

				trg.getPosition().setXYZ(activeChar.getX() + Rnd.get(-10, 10), activeChar.getY() + Rnd.get(-10, 10), activeChar.getZ());
				trg.stopMove(null);
				trg.sendPacket(new ValidateLocation(trg));
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}