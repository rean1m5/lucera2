package ru.catssoftware.gameserver.network.clientpackets;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.ClanWarehouse;
import ru.catssoftware.gameserver.model.itemcontainer.ItemContainer;
import ru.catssoftware.gameserver.model.itemcontainer.PcFreight;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;

public class SendWareHouseDepositList extends L2GameClientPacket
{
	private static final String	_C__31_SENDWAREHOUSEDEPOSITLIST	= "[C] 31 SendWareHouseDepositList";
	private int					_count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_count = readD();

		// check packet list size
			if (_count < 0 || _count * 8 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
				_count = 0;

		_items = new int[_count * 2];
		for (int i = 0; i < _count; i++)
		{
			int objectId = readD();
			_items[(i * 2)] = objectId;
			long cnt = readD();
			if (cnt >= Integer.MAX_VALUE || cnt < 0)
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
		
		player._bbsMultisell = 0;
		ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;
		L2FolkInstance manager = player.getLastFolkNPC();
		if(player.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE) {
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		if(player.getTarget()!=player.getLastFolkNPC()) {
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		if ((manager == null || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		if (player.getActiveEnchantItem() != null)
		{
			Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытается использовать баг с точкой!", IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
		{
			ActionFailed();
			return;
		}

		// Freight price from config or normal price per item slot (30)
		int fee = _count * 30;
		int currentAdena = player.getAdena();
		int slots = 0;

		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[(i * 2)];
			int count = _items[i * 2 + 1];

			// Check validity of requested item
			L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
			if (item == null)
			{
				_items[i * 2 + 0] = 0;
				_items[i * 2 + 1] = 0;
				continue;
			}

			if (Config.ALT_STRICT_HERO_SYSTEM)
			{
				if (item.isHeroItem())
					continue;
			}

			if ((warehouse instanceof ClanWarehouse || warehouse instanceof PcFreight) && !item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
			{
				ActionFailed();
				return;
			}
			int id = item.getItemId();
			if (id == 10612 || id == 10280 || id == 10281 || id == 10282 || id == 10283 || id == 10284 || id == 10285 || id == 10286 || id == 10287
			|| id == 10288 || id == 10289 || id == 10290 || id == 10291 || id == 10292 || id == 10293 || id == 10294 || id == 13002
			|| id == 13046 || id == 13047 || id == 13042 || id == 13043 || id == 13044)
			{
				player.sendMessage("Эту вещь нельзя положить на склад.");
				ActionFailed();
				return;
			}
			// Calculate needed adena and slots
			if (item.getItemId() == 57)
				currentAdena -= count;
			if (!item.isStackable())
				slots += count;
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
				slots++;
		}

		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(SystemMessageId.WAREHOUSE_FULL);
			return;
		}

		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getLastFolkNPC(), false))
		{
			sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}

		// Proceed to the transfer
		InventoryUpdate playerIU =  new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[(i * 2)];
			int count = _items[i * 2 + 1];

			// check for an invalid item
			if (objectId == 0 && count == 0)
				continue;

			L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);

			if (oldItem == null)
			{
				_log.info("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				continue;
			}

			if (Config.ALT_STRICT_HERO_SYSTEM)
			{
				if (oldItem.isHeroItem())
					continue;
			}

			
			L2ItemInstance newItem = player.getInventory().transferItem((warehouse instanceof ClanWarehouse) ? "ClanWarehouse" : "Warehouse", objectId, count, warehouse, player, player.getLastFolkNPC());
			if (newItem == null)
			{
				_log.info("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}

			if (playerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
					playerIU.addModifiedItem(oldItem);
				else
					playerIU.addRemovedItem(oldItem);
			}
		}

		// Send updated item list to the player
		if (playerIU != null)
			player.sendPacket(playerIU);
		else
			player.sendPacket(new ItemList(player, false));

		// Update current load status on player
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new InventoryUpdate());
		player.broadcastFullInfo();
		warehouse.updateDatabase();
		player.store(true);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__31_SENDWAREHOUSEDEPOSITLIST;
	}
}