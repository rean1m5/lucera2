package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2CastleChamberlainInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FishermanInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MercManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MerchantInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetManagerInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;
import javolution.util.FastList;


public class RequestBuyItem extends L2GameClientPacket
{
	private static final String	_C__1F_REQUESTBUYITEM	= "[C] 1F RequestBuyItem";
	private int					_listId, _count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 2 < 0 || _count * 8 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
			_count = 0;

		_items = new int[_count * 2];
		for (int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i * 2] = itemId;
			long cnt = readD();
			if ((cnt >= Integer.MAX_VALUE) || (cnt < 0))
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 2 + 1] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if(player.isProcessingTransaction()) {
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}
			
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (player.getKarma() > 0) && !player.isGM())
			return;

		L2Object target = player.getTarget();
		if (target == null)
			return;

		if (!player.isGM() && // Player not GM
				(!(target instanceof L2MerchantInstance || // Target not a merchant, fisherman or mercmanager
						target instanceof L2FishermanInstance || target instanceof L2MercManagerInstance || target instanceof L2ClanHallManagerInstance || target instanceof L2CastleChamberlainInstance) ||
						!player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false))) // Distance is too far
			return;

		String htmlFolder = "";

		if (target instanceof L2MerchantInstance)
			htmlFolder = "merchant";
		else if (target instanceof L2FishermanInstance)
			htmlFolder = "fisherman";
		else if (target instanceof L2PetManagerInstance)
			htmlFolder = "petmanager";
		else if (!(target instanceof L2MercManagerInstance || target instanceof L2ClanHallManagerInstance || target instanceof L2CastleChamberlainInstance))
		{
			if (!player.isGM())
				return;
		}

		L2NpcInstance merchant = null;
		if (target instanceof L2NpcInstance)
			merchant = (L2NpcInstance) target;

		L2TradeList list = null;

		if (merchant != null && !player.isGM())
		{
			FastList<L2TradeList> lists = TradeListTable.getInstance().getBuyListByNpcId(merchant.getNpcId());

			if (!player.isGM())
			{
				if (lists == null)
				{
					Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается открыть несуществующий торговый лист.", Config.DEFAULT_PUNISH);
					return;
				}

				for (L2TradeList tradeList : lists)
				{
					if (tradeList.getListId() == _listId)
						list = tradeList;
				}
			}
			else
				list = TradeListTable.getInstance().getBuyList(_listId);
		}
		else
			list = TradeListTable.getInstance().getBuyList(_listId);

		if (list == null)
		{
			if (!player.isGM())
			{
				Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается открыть несуществующий торговый лист.", Config.DEFAULT_PUNISH);
				return;
			}
			player.sendMessage("Торговый лист " + _listId + " пуст или несуществует.");
			ActionFailed();
			return;
		}

		if (list.isGm() && !player.isGM())
		{
			Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " делает попытку доступа к гм шопу.", Config.DEFAULT_PUNISH);
			return;
		}

		_listId = list.getListId();

		if (_listId > 1000000) // lease
		{
			if (merchant != null && merchant.getTemplate().getNpcId() != _listId / 100)
			{
				ActionFailed();
				return;
			}
		}

		if (_count < 1)
		{
			ActionFailed();
			return;
		}

		double taxRate = 1.0;
		double lordTaxRate = 1.0;

		if (merchant instanceof L2MerchantInstance)
		{
			if (merchant.getIsInTown())
			{
				taxRate = merchant.getCastle().getTaxRate();
				lordTaxRate = merchant.getCastle().getTaxRate();
			}
			taxRate = taxRate + ((L2MerchantInstance) merchant).getMpc().getBaseTaxRate();
		}
		long lordTaxTotal = 0;
		//L2EMU_EDIT

		long taxedPriceTotal = 0;
		long taxTotal = 0;

		// Check for buylist validity and calculates summary values
		long slots = 0;
		long weight = 0;

		if(player.getInventoryLimit()-player.getInventory().getSize()<=_count) {
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i * 2];
			int count = _items[i * 2 + 1];
			int price = -1;

			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается открыть несуществующий торговый лист.", Config.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(itemId);

			if (template == null)
				continue;

			if ((!template.isStackable() && count > 1))
			{
				Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается приобрести недопустимый в это время итем.", Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				sm = null;

				return;
			}

			price = list.getPriceForItemId(itemId);

			if (_listId < 1000000)
			{
				if ((itemId >= 3960) && (itemId <= 4026))
					price *= Config.RATE_SIEGE_GUARDS_PRICE;
			}

			if (price < 0)
			{
				_log.warn("ERROR, no price found .. wrong buylist ??");
				ActionFailed();
				return;
			}

			if ((price == 0) && !player.isGM())
			{
				Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается купить вещь за 0 аден.", Config.DEFAULT_PUNISH);
				return;
			}

			long stackPrice = (long)price * count;
			long taxedPrice = (long) Math.round(stackPrice * taxRate);
			long lordTaxPrice = (long) Math.round(stackPrice * lordTaxRate);
			long lordTax = lordTaxPrice - stackPrice;
			lordTaxTotal += lordTax;
			long tax = taxedPrice - stackPrice;
			if (taxedPrice >= Integer.MAX_VALUE)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				return;
			}
			taxedPriceTotal += taxedPrice;
			taxTotal += tax;

			weight += (long) count * template.getWeight();
			if (!template.isStackable())
				slots += count;
			else if (player.getInventory().getItemByItemId(itemId) == null)
				slots++;
		}

		if ((weight >= Integer.MAX_VALUE) || (weight < 0) || !player.getInventory().validateWeight((int) weight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}

		if ((slots >= Integer.MAX_VALUE) || (slots < 0) || !player.getInventory().validateCapacity((int) slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		if (!player.isGM())
		{
			if ((taxedPriceTotal < 0) || (taxedPriceTotal >= Integer.MAX_VALUE) || !player.reduceAdena("Buy", (int) taxedPriceTotal, merchant, false))
			{
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				return;
			}
		}

		if (!player.isGM())
		{
			if (merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
				merchant.getCastle().addToTreasury((int) lordTaxTotal);
		}

		// Proceed the purchase
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[(i * 2)];
			int count = _items[i * 2 + 1];
			if (count < 0)
				count = 0;

			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Внимание! Персонаж: " + player.getName() + ", аккаунт: " + player.getAccountName() + " пытается открыть несуществующий торговый лист.", Config.DEFAULT_PUNISH);
				return;
			}

			if (list.countDecrease(itemId))
			{
				if (!list.decreaseCount(itemId, count))
				{
					Util.handleIllegalPlayerAction(player, "Попытка обойти лимит покупки.", Config.DEFAULT_PUNISH);
					return;
				}
			}

			player.getInventory().addItem(list.isGm() ? "GMShop" : "Buy", itemId, count, player, merchant);
		}

		if (merchant != null)
		{
			String html = "";
			if (merchant instanceof L2ClanHallManagerInstance)
				html = HtmCache.getInstance().getHtm("data/html/" + htmlFolder + "/clanHall-bought.htm",player);
			else
				html = HtmCache.getInstance().getHtm("data/html/" + htmlFolder + "/" + merchant.getNpcId() + "-bought.htm",player);

			if (html != null)
			{
				NpcHtmlMessage boughtMsg = new NpcHtmlMessage(merchant.getObjectId());
				boughtMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())).replaceAll("%npcId%", String.valueOf(merchant.getNpcId())));
				player.sendPacket(boughtMsg);
			}
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}

	@Override
	public String getType()
	{
		return _C__1F_REQUESTBUYITEM;
	}
}