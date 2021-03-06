package ru.catssoftware.gameserver.network.clientpackets;

import javolution.util.FastList;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.ItemContainer;
import ru.catssoftware.gameserver.model.itemcontainer.PcFreight;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.util.Util;

import java.util.List;

public final class RequestPackageSend extends L2GameClientPacket
{
	private static final String	_C_9F_REQUESTPACKAGESEND	= "[C] 9F RequestPackageSend";
	private List<Item>			_items						= new FastList<Item>();
	private int					_objectID, _count;

	@Override
	protected void readImpl()
	{
		try
		{
			_objectID = readD();
			_count = readD();
			if (_count < 0 || _count > 500)
			{
				_count = -1;
				return;
			}
			for (int i = 0; i < _count; i++)
			{
				int id = readD(); //this is some id sent in PackageSendableList
				int count = readD();
				_items.add(new Item(id, count));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void runImpl()
	{
		if (_count == -1)
			return;

		L2PcInstance player = getClient().getActiveChar();

		if (player == null || player.getObjectId() == _objectID)
			return;

		if (!getClient().haveCharOnAccount(_objectID))
		{
			String msgErr = "[RequestPackageSend] player " + player.getName() + " tried to send item freight to not his char, ban this player!";
			Util.handleIllegalPlayerAction(player, msgErr, Config.DEFAULT_PUNISH);
			return;
		}

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
			return;

		L2FolkInstance manager = player.getLastFolkNPC();
		if ((manager == null || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		L2PcInstance target = L2PcInstance.load(_objectID);
		PcFreight freight = target.getFreight();
		player.setActiveWarehouse(freight);
		ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;

		try
		{
			// Freight price from config or normal price per item slot (30)
			int fee = _count * Config.ALT_GAME_FREIGHT_PRICE;
			int currentAdena = player.getAdena();
			int slots = 0;

			for (Item i : _items)
			{
				int objectId = i.id;
				int count = i.count;

				// Check validity of requested item
				L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
				if (item == null)
				{
					_log.info("Error depositing a warehouse object for char " + player.getName() + ", item manipulation is null.");
					i.id = 0;
					i.count = 0;
					continue;
				}

				if (!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
					return;

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
			InventoryUpdate playerIU = new InventoryUpdate();
			for (Item i : _items)
			{
				int objectId = i.id;
				int count = i.count;

				// check for an invalid item
				if (objectId == 0 && count == 0)
					continue;

				L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);
				if (oldItem == null)
				{
					_log.info("Error depositing a warehouse object for char " + player.getName() + ".");
					continue;
				}

				if (oldItem.isHeroItem())
					continue;

				L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", objectId, count, warehouse, player, player.getLastFolkNPC());
				if (newItem == null)
				{
					_log.info("Error depositing a warehouse object for char " + player.getName() + ".");
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			target.deleteMe();

		}
	}

	private class Item
	{
		public int	id;
		public int	count;

		public Item(int i, int c)
		{
			id = i;
			count = c;
		}
	}

	@Override
	public String getType()
	{
		return _C_9F_REQUESTPACKAGESEND;
	}
}