package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class ChannelLeave implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 96 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		if (activeChar.isInParty())
		{
			if (activeChar.getParty().isLeader(activeChar) && activeChar.getParty().isInCommandChannel())
			{
				L2Party party = activeChar.getParty();
				L2CommandChannel channel = party.getCommandChannel();
				channel.removeParty(party);
				SystemMessage sm = new SystemMessage(SystemMessageId.LEFT_COMMAND_CHANNEL);
				party.broadcastToPartyMembers(sm);
				sm = new SystemMessage(SystemMessageId.S1_PARTY_LEFT_COMMAND_CHANNEL);
				sm.addString(activeChar.getName());
				channel.broadcastToChannelMembers(sm);
				return true;
			}
			activeChar.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_LEAVE_CHANNEL);
		}

		return false;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}