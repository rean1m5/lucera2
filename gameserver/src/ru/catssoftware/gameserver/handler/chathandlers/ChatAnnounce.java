package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatAnnounce implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Announce, SystemChatChannelId.Chat_Critical_Announce };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		String charName = "";
		int charObjId = 0;

		if (activeChar != null)
		{
			charName = activeChar.getName();
			charObjId = activeChar.getObjectId();

			if (!activeChar.isGM())
				return;
		}

		if (chatType == SystemChatChannelId.Chat_Critical_Announce)
			text = "** " + text;

		CreatureSay cs = new CreatureSay(charObjId, chatType, charName, text);

		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player != null)
				player.sendPacket(cs);
		}
	}
}