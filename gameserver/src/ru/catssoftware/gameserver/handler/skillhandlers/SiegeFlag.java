package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeFlagInstance;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

/**
 * @author _drunk_
 */
public class SiegeFlag implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SIEGEFLAG };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		Siege siege = SiegeManager.getInstance().getSiege(player);
		FortSiege fsiege = FortSiegeManager.getInstance().getSiege(player);
		// In a siege zone
		if (siege != null && SiegeManager.checkIfOkToPlaceFlag(player, false))
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(35062);
			if (skill != null && template != null)
			{
				// spawn a new flag
				L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), template, skill.isAdvanced(),false,null);
				flag.setTitle(player.getClan().getName());
				flag.getStatus().setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
				flag.setHeading(player.getHeading());
				flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
				siege.getFlag(player.getClan()).add(flag);
			}
		}
		else if (fsiege != null && FortSiegeManager.checkIfOkToPlaceFlag(activeChar, false))
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(35062);
			if (skill != null && template != null)
			{
				// spawn a new flag
				L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), template, skill.isAdvanced(),false,null);
				flag.setTitle(player.getClan().getName());
				flag.getStatus().setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
				flag.setHeading(player.getHeading());
				flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
				fsiege.getFlag(player.getClan()).add(flag);
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}