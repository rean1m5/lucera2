package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Town;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegionRestart;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class Loc implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 0 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		L2MapRegionRestart restart = null;
		SystemMessageId msg = SystemMessageId.LOC_ADEN_S1_S2_S3;

		//Standard Town
		Town town = TownManager.getInstance().getTown(Config.ALT_DEFAULT_RESTARTTOWN);
		if (town != null && town.getMapRegion() != null)
		{
			restart = MapRegionManager.getInstance().getRestartLocation(town.getMapRegion().getRestartId());
			if (restart != null)
				msg = SystemMessageId.getSystemMessageId(restart.getLocName());
		}

		L2MapRegion region = MapRegionManager.getInstance().getRegion(activeChar);
		if (region != null)
		{
			int restartId = region.getRestartId();
			restart = MapRegionManager.getInstance().getRestartLocation(restartId);
			msg = SystemMessageId.getSystemMessageId(restart.getLocName());
		}

		SystemMessage sm = new SystemMessage(msg);
		sm.addNumber(activeChar.getX());
		sm.addNumber(activeChar.getY());
		sm.addNumber(activeChar.getZ());
		activeChar.sendPacket(sm);

		if (restart != null)
		{
			if (restart.getLocName() < 1222)
			{
				if (restart.getLocName() != 943)
					activeChar.sendPacket(SystemMessageId.getSystemMessageId(msg.getId() + 31));
				else
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Gludio."));
			}
			else
			{
				if (SystemMessageId.LOC_GM_CONSULATION_SERVICE_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Jail."));
				else if (SystemMessageId.LOC_RUNE_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Rune."));
				else if (SystemMessageId.LOC_GODDARD_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Goddard."));
				else if (SystemMessageId.CURRENT_LOCATION_S1_S2_S3_DIMENSIONAL_GAP.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Dimension gap."));
				else if (SystemMessageId.LOC_CEMETARY_OF_THE_EMPIRE_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Cemetary of the empire."));
				else if (SystemMessageId.LOC_SCHUTTGART_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Shuttgart."));
				else if (SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3.getId() == restart.getLocName())
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RESTART_POINT_AT), "Primeval isle."));
			}
		}
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}