package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyRoom implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Party_Room };


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
		if (party != null)
		{
			L2CommandChannel chan = party.getCommandChannel();
			if (chan != null && party.isLeader(activeChar))
			{
				CreatureSay cs = new CreatureSay(charObjId, chatType, charName, text);
				chan.broadcastCSToChannelMembers(cs, activeChar);
			}
		}
	}
}