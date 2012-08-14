package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExMultiPartyCommandChannelInfo;

public class ChannelListUpdate implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 97 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;
		if (activeChar.getParty() == null || activeChar.getParty().getCommandChannel() == null)
			return false;

		activeChar.sendPacket(new ExMultiPartyCommandChannelInfo(activeChar.getParty().getCommandChannel()));
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}