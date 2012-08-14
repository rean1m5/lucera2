package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;


public class TakeFort implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.TAKEFORT };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		L2Object target = player.getTarget();

		if (player.getClan() == null)
			return;

		if (target == null)
			return;

		Fort fort = FortManager.getInstance().getFort(player);
		if (fort == null || !checkIfOkToCastFlagDisplay(player, fort, true, skill, target))
			return;

		fort.endOfSiege(player.getClan());
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}

	public static boolean checkIfOkToCastFlagDisplay(L2Character activeChar, boolean isCheckOnly, L2Skill skill, L2Object target)
	{
		return checkIfOkToCastFlagDisplay(activeChar, FortManager.getInstance().getFort(activeChar), isCheckOnly, skill, target);
	}

	public static boolean checkIfOkToCastFlagDisplay(L2Character activeChar, Fort fort, boolean isCheckOnly, L2Skill skill, L2Object target)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;

		SystemMessage sm;
		L2PcInstance player = (L2PcInstance) activeChar;

		if (fort == null || fort.getFortId() <= 0)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, player, player.getTarget(), true))
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
				fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1), player.getClan().getName()+" пытается захватить форт!");
			return true;
		}

		if (!isCheckOnly)
			player.sendPacket(sm);

		return false;
	}
}