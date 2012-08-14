package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Normal };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		String name = activeChar.getAppearance().getVisibleName();
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, name, text);

		for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
		{
			if (player != null && activeChar.isInsideRadius(player, 1250, false, true) && !(Config.REGION_CHAT_ALSO_BLOCKED && BlockList.isBlocked(player, activeChar)))
			{
				player.sendPacket(cs);
				player.broadcastSnoop(chatType.getId(), name, text);
			}
		}
		activeChar.sendPacket(cs);
		activeChar.broadcastSnoop(chatType.getId(), name, text);
	}
}