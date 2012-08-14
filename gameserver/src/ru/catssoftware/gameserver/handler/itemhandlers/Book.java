package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

public class Book implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5588, 6317, 7561, 7063, 7064,
		7065, 7066, 7082, 7083, 7084,
		7085, 7086, 7087, 7088, 7089,
		7090, 7091, 7092, 7093, 7094,
		7095, 7096, 7097, 7098, 7099,
		7100, 7101, 7102, 7103, 7104,
		7105, 7106, 7107, 7108, 7109,
		7110, 7111, 7112, 8059
	};
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();

		String filename = "data/html/help/" + item.getItemId() + ".htm";
		String content = HtmCache.getInstance().getHtm(filename,activeChar);

		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>Текст отсутствует:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(5, itemId);
			itemReply.setHtml(content);
			activeChar.sendPacket(itemReply);
		}

		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}