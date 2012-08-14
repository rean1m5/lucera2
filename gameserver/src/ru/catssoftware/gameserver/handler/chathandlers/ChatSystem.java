package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatSystem implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_System };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName() + "'s Emote", text);

		for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
		{
			if (player != null && activeChar.isInsideRadius(player, 1250, false, true))
			{
				player.sendPacket(cs);
				player.broadcastSnoop(chatType.getId(), activeChar.getName(), text);
			}
		}
		activeChar.sendPacket(cs);
		activeChar.broadcastSnoop(chatType.getId(), activeChar.getName(), text);
	}
}