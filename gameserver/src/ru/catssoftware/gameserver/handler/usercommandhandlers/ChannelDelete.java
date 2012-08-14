package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class ChannelDelete implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 93 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		if (activeChar.isInParty())
		{
			if (activeChar.getParty().isLeader(activeChar) && activeChar.getParty().isInCommandChannel()
					&& activeChar.getParty().getCommandChannel().getChannelLeader() == activeChar)
			{
				L2CommandChannel channel = activeChar.getParty().getCommandChannel();

				SystemMessage sm = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED);
				channel.broadcastToChannelMembers(sm);

				channel.disbandChannel();
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