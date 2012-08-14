package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;

public class ChatWhisper implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Tell };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		L2PcInstance receiver = L2World.getInstance().getPlayer(target);

		if (receiver != null  && !BlockList.isBlocked(receiver, activeChar))
		{
			if (!receiver.getMessageRefusal() || activeChar.isGM())
			{
				if (receiver.isOfflineTrade())
				{
					activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAR_BUSY_TRY_LATER));
					return;
				}
				if (receiver.isAway())
				{
					activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAR_BUSY_TRY_LATER));
					return;
				}
				if (receiver.isInJail() && !activeChar.isGM())
				{
					activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAR_BUSY_TRY_LATER));
					return;
				}
				receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName(), text));
				receiver.broadcastSnoop(chatType.getId(), activeChar.getName(), text);
				activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), chatType, "->" + receiver.getName(), text));
				activeChar.broadcastSnoop(chatType.getId(), "->" + receiver.getName(), text);
			}
			else
				activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
		}
		else
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
	}
}