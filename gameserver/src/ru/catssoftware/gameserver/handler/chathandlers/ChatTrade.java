package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Config.ChatMode;
import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class ChatTrade implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_Market };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		if (!FloodProtector.tryPerformAction(activeChar, Protected.TRADE_CHAT) && !activeChar.isGM())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAT_FLOOD_PROTECT));
			return;
		}
		if(activeChar.getLevel()<Config.TRADE_CHAT_LEVEL) {
			activeChar.sendMessage("Торговый чат доступен с "+Config.TRADE_CHAT_LEVEL+" уровня");
			return;
		}

		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), chatType, activeChar.getName(), text);

		if (Config.DEFAULT_TRADE_CHAT == ChatMode.REGION)
		{
			L2MapRegion region = MapRegionManager.getInstance().getRegion(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (region == MapRegionManager.getInstance().getRegion(player.getX(), player.getY(), player.getZ()) && !(Config.REGION_CHAT_ALSO_BLOCKED && BlockList.isBlocked(player, activeChar)) && (player.getInstanceId() == activeChar.getInstanceId()))
				{
					player.sendPacket(cs);
					player.broadcastSnoop(chatType.getId(), activeChar.getName(), text);
				}
			}
		}
		else if (Config.DEFAULT_TRADE_CHAT == ChatMode.GLOBAL || Config.DEFAULT_TRADE_CHAT == ChatMode.GM && activeChar.isGM())
		{
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (!(Config.REGION_CHAT_ALSO_BLOCKED && BlockList.isBlocked(player, activeChar)) && (player.getInstanceId() == activeChar.getInstanceId()))
				{
					player.sendPacket(cs);
					player.broadcastSnoop(chatType.getId(), activeChar.getName(), text);
				}
			}
		}
	}
}