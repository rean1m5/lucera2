package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.ClanWarehouse;
import ru.catssoftware.gameserver.model.itemcontainer.ItemContainer;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;

public class SendWareHouseWithDrawList extends L2GameClientPacket
{
	private static final String	_C__32_SENDWAREHOUSEWITHDRAWLIST	= "[C] 32 SendWareHouseWithDrawList";
	private int					_count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_count = readD();
			if (_count < 0 || _count * 8 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
			{
				_count = 0;
				_items = null;
				return;
			}
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
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		if(player.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE) {
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;

		if (warehouse instanceof ClanWarehouse && player.getClanId()!=warehouse.getOwnerId())
			return;
		
		L2FolkInstance manager = player.getLastFolkNPC();
		if (manager!=player.getTarget() || (manager == null || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
			return;

		if (Config.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH)
		{
			if (warehouse instanceof ClanWarehouse && !((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
				return;
		}
		else
		{
			if (warehouse instanceof ClanWarehouse && !player.isClanLeader())
			{
				// this msg is for depositing but maybe good to send some msg?
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE);
				return;
			}

		}

		int weight = 0;
		int slots = 0;

		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[(i * 2)];
			int count = _items[i * 2 + 1];

			// Calculate needed slots
			L2ItemInstance item = warehouse.getItemByObjectId(objectId);
			if (item == null)
				continue;
			weight += count * item.getItem().getWeight();
			if (!item.isStackable())
				slots += count;
			else if (player.getInventory().getItemByItemId(item.getItemId()) == null)
				slots++;
		}

		// Item Max Limit Check 
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		// Weight limit Check 
		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}

		// Proceed to the transfer
		InventoryUpdate playerIU = new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[(i * 2)];
			int count = _items[i * 2 + 1];

			L2ItemInstance oldItem = warehouse.getItemByObjectId(objectId);
			if (oldItem == null || oldItem.getCount() < count) {
				player.sendMessage("Can't withdraw requested item" + (count > 1 ? "s" : ""));
				continue;
			}
			L2ItemInstance newItem = warehouse.transferItem((warehouse instanceof ClanWarehouse) ? "ClanWarehouse" : "Warehouse", objectId, count, player.getInventory(), player, player.getLastFolkNPC());
			if (newItem == null)
			{
				_log.info("Error withdrawing a warehouse object for char " + player.getName());
				continue;
			}

			if (playerIU != null)
			{
				if (newItem.getCount() > count)
					playerIU.addModifiedItem(newItem);
				else
					playerIU.addNewItem(newItem);
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
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__32_SENDWAREHOUSEWITHDRAWLIST;
	}
}