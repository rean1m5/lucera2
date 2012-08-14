package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatCommander implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Commander, SystemChatChannelId.Chat_Inner_Partymaster };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		String charName = "";
		int charObjId = 0;

		if (activeChar == null)
			return;

		charName = activeChar.getName();
		charObjId = activeChar.getObjectId();

		L2Party party = activeChar.getParty();
		if (party != null && party.isInCommandChannel())
		{
			if (chatType == SystemChatChannelId.Chat_Commander)
			{
				if (party.getCommandChannel().getChannelLeader() == activeChar)
				{
					CreatureSay cs = new CreatureSay(charObjId, chatType, charName, text);
					party.getCommandChannel().broadcastToChannelMembers(cs);
				}
				else
					activeChar.sendPacket(SystemMessageId.ONLY_CHANNEL_CREATOR_CAN_GLOBAL_COMMAND);
			}
			else if (chatType == SystemChatChannelId.Chat_Inner_Partymaster)
			{
				if (party.getLeader() == activeChar)
				{
					CreatureSay cs = new CreatureSay(charObjId, chatType, charName, text);
					party.getCommandChannel().broadcastCSToChannelMembers(cs, activeChar);
				}
				else
					activeChar.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_FOR_PARTY_LEADER);
			}
		}
	}
}