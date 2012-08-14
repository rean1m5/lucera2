package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatAlliance implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Alliance };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		if (activeChar.getClan() == null)
			return;

		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName(), text);
		activeChar.getClan().broadcastToOnlineAllyMembers(cs);
		activeChar.getClan().broadcastSnoopToOnlineAllyMembers(chatType.getId(), activeChar.getName(), text);
	}
}