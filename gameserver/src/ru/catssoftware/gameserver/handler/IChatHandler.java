package ru.catssoftware.gameserver.handler;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;

public interface IChatHandler
{
	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text);

	public SystemChatChannelId[] getChatTypes();
}
