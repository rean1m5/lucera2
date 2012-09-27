package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2MercManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MerchantInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.lang.RunnableImpl;

import java.util.List;
import java.util.concurrent.Future;


public class RequestWearItem extends L2GameClientPacket
{
	private static final String	_C__C6_REQUESTWEARITEM	= "[C] C6 RequestWearItem";
	protected Future<?>			_removeWearItemsTask;
	@SuppressWarnings("unused")
	private int					_unknow, _listId, _count;
	private int[]				_items;
	protected L2PcInstance		_activeChar;

	class RemoveWearItemsTask extends RunnableImpl
	{
		@Override
		public void runImpl()
		{
			try
			{
				_activeChar.destroyWearedItems("Wear", null, true);

			}
			catch (Throwable e)
			{
				_log.fatal("", e);
			}
		}
	}

	@Override
	protected void readImpl()
	{
		_activeChar = getClient().getActiveChar();
		_unknow = readD();
		_listId = readD(); // List of ItemID to Wear
		_count = readD(); // Number of Item to Wear

		if (_count < 0)
			_count = 0;
		if (_count > 100)
			_count = 0; // prevent too long lists

		_items = new int[_count];

		// Fill _items table with all ItemID to Wear
		for (int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i] = itemId;
		}
	}

	@Override
	protected void runImpl()
	{
		// Get the current player and return if null
		if (_activeChar == null)
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			_activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			_activeChar.cancelActiveTrade();
			return;
		}

		// If Alternate rule Karma punishment is set to true, forbid Wear to player with Karma
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && _activeChar.getKarma() > 0)
			return;

		// Check current target of the player and the INTERACTION_DISTANCE
		L2Object target = _activeChar.getTarget();
		if (!_activeChar.isGM() && (target == null // No target (ie GM Shop)
				|| !(target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance) // Target not a merchant and not mercmanager
		|| !_activeChar.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false) // Distance is too far
				))
			return;

		L2TradeList list = null;

		L2MerchantInstance merchant = (target instanceof L2MerchantInstance) ? (L2MerchantInstance) target : null;

		List<L2TradeList> lists = TradeListTable.getInstance().getBuyListByNpcId(merchant.getNpcId());

		if (lists == null)
		{
			Util.handleIllegalPlayerAction(_activeChar, "Warning!! Character " + _activeChar.getName() + " of account " + _activeChar.getAccountName()
					+ " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}
		for (L2TradeList tradeList : lists)
		{
			if (tradeList.getListId() == _listId)
			{
				list = tradeList;
			}
		}
		if (list == null)
		{
			Util.handleIllegalPlayerAction(_activeChar, "Warning!! Character " + _activeChar.getName() + " of account " + _activeChar.getAccountName()
					+ " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}

		_listId = list.getListId();

		if (_count < 1 || _listId < 1000000)
		{
			ActionFailed();
			return;
		}
		long totalPrice = 0;
		int slots = 0;
		int weight = 0;

		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(_activeChar, "Warning!! Character " + _activeChar.getName() + " of account " + _activeChar.getAccountName()
						+ " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(itemId);
			weight += template.getWeight();
			slots++;

			totalPrice += Config.WEAR_PRICE;
			if (totalPrice >= Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(_activeChar, "Warning!! Character " + _activeChar.getName() + " of account " + _activeChar.getAccountName()
						+ " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
		}
		if (!_activeChar.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		if (!_activeChar.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		if ((totalPrice < 0) || !_activeChar.reduceAdena("Wear", (int) totalPrice, _activeChar.getLastFolkNPC(), false))
		{
			sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		InventoryUpdate playerIU = new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(_activeChar, "Warning!! Character " + _activeChar.getName() + " of account " + _activeChar.getAccountName()
						+ " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}
			L2ItemInstance item = _activeChar.getInventory().addWearItem("Wear", itemId, _activeChar, merchant);
			_activeChar.getInventory().equipItemAndRecord(item);
			playerIU.addItem(item);
		}
		_activeChar.sendPacket(playerIU);
		StatusUpdate su = new StatusUpdate(_activeChar.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, _activeChar.getCurrentLoad());
		_activeChar.sendPacket(su);
		_activeChar.broadcastUserInfo(true);
		if (_removeWearItemsTask == null)
			_removeWearItemsTask = ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(), Config.WEAR_DELAY * 1000);
	}

	@Override
	public String getType()
	{
		return _C__C6_REQUESTWEARITEM;
	}
}