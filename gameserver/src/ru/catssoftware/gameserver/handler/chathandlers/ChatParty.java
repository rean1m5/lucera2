package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatParty implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Party };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName(), text);

		if (activeChar.isInParty())
		{
			activeChar.getParty().broadcastCSToPartyMembers(cs, activeChar);
			activeChar.getParty().broadcastSnoopToPartyMembers(chatType.getId(), activeChar.getName(), text);
		}
	}
}