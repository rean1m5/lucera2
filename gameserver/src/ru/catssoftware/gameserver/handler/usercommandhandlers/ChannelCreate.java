package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class ChannelCreate implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 92 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;
		if (activeChar == null)
			return false;
		if (activeChar.isInParty())
		{
			SystemMessage sm;
			L2Party activeParty = activeChar.getParty();
			if (activeParty.getLeader() == activeChar)
			{
				if (!activeParty.isInCommandChannel())
				{
					tryCreateMPCC(activeChar);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL);
					sm.addString(activeChar.getName());
					activeChar.sendPacket(sm);
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);

		}
		return true;
	}
	private void tryCreateMPCC(L2PcInstance requestor)
	{
		boolean hasRight = false;
		if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId() && requestor.getClan().getLevel() >= 5) // Clanleader
			hasRight = true;
		else
		{
			for (L2Skill skill : requestor.getAllSkills())
			{
				if (skill.getId() == 391)
				{
					hasRight = true;
					break;
				}
			}
		}
		if (!hasRight)
		{
			if (requestor.destroyItemByItemId("MPCC", 8871, 1, requestor, false))
			{
				hasRight = true;
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(8871);
				sm.addNumber(1);
				requestor.sendPacket(sm);
			}
		}
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return;
		}

		if (!requestor.isProcessingRequest())
		{
			new L2CommandChannel(requestor); // Create new CC
			SystemMessage sm = new SystemMessage(SystemMessageId.JOINED_COMMAND_CHANNEL);
			requestor.getParty().broadcastToPartyMembers(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(requestor.getName());
			requestor.sendPacket(sm);
		}
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}