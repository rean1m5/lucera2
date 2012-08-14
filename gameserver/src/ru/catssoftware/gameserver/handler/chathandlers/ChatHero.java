package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class ChatHero implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Hero };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		boolean canSpeak = activeChar.isGM();

		if (!canSpeak)
		{
			if ( activeChar.isHero() || (activeChar.getPvpKills() >= Config.PVP_COUNT_TO_CHAT) && Config.ALT_HERO_CHAT_SYSTEM)
			{
				if (FloodProtector.tryPerformAction(activeChar, Protected.HEROVOICE))
					canSpeak = true;
				else
					activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_HERO_CHAT_ONCE_PER_TEN_SEC));
			}
		}
		if (canSpeak)
		{
			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName(), text);
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
				player.sendPacket(cs);
		}
	}
}