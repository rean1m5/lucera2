package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2FishermanInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MerchantInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetManagerInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.Util;

public class RequestSellItem extends L2GameClientPacket
{
	private static final String	_C__1E_REQUESTSELLITEM	= "[C] 1E RequestSellItem";

	private int					_listId, _count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
		if (_count <= 0 || _count * 12 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
			{
				_count = 0;
				_items = null;
				return;
			}
		_items = new int[_count * 3];
		for (int i = 0; i < _count; i++)
		{
			int objectId = readD(); _items[(i * 3)] = objectId;
			int itemId = readD(); _items[i * 3 + 1] = itemId;
			long cnt = readD();
			if (cnt > Integer.MAX_VALUE || cnt <= 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 3 + 2] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		this.processSell();
	}

	protected void processSell()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if(player.isProcessingTransaction()) {
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			player.cancelActiveTrade();
			return;
		}

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;

		L2Object target = player.getTarget();
		if (!player.isGM() && (target == null // No target (ie GM Shop)
				|| !(target instanceof L2MerchantInstance) // Target not a merchant and not mercmanager
				|| !player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false))) // Distance is too far
			return;

		boolean ok = true;
		String htmlFolder = "";

		if (target != null)
		{
			if (target instanceof L2MerchantInstance)
				htmlFolder = "merchant";
			else if (target instanceof L2FishermanInstance)
				htmlFolder = "fisherman";
			else if (target instanceof L2PetManagerInstance)
				htmlFolder = "petmanager";
			else
				ok = false;
		}
		else
			ok = false;

		L2NpcInstance merchant = null;

		if (ok)
			merchant = (L2NpcInstance) target;

		if (merchant != null && _listId > 1000000) // lease
		{
			if (merchant.getTemplate().getNpcId() != _listId - 1000000)
			{
				ActionFailed();
				return;
			}
		}

		long totalPrice = 0;
		// Proceed the sell
		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[(i * 3)];
			@SuppressWarnings("unused")
			int itemId = _items[i * 3 + 1];
			int count = _items[i * 3 + 2];

			if (count < 0)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName()
						+ " tried to purchase over " + Integer.MAX_VALUE + " items at the same time.", Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				sm = null;
				return;
			}

			L2ItemInstance item = player.checkItemManipulation(objectId, count, "sell");
			if (item == null || !item.isSellable())
				continue;

			totalPrice += item.getReferencePrice() * count / 2;
			if (totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}

			item = player.getInventory().destroyItem("Sell", objectId, count, player, null);
		}
		player.addAdena("Sell", (int) totalPrice, merchant, true);

		if (merchant != null)
		{
			String html = HtmCache.getInstance().getHtm("data/html/" + htmlFolder + "/" + merchant.getNpcId() + "-sold.htm",player);

			if (html != null)
			{
				NpcHtmlMessage soldMsg = new NpcHtmlMessage(merchant.getObjectId());
				soldMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())));
				player.sendPacket(soldMsg);
			}
		}

		// Update current load as well
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
		player.getInventory().updateDatabase();
	}

	@Override
	public String getType()
	{
		return _C__1E_REQUESTSELLITEM;
	}
}