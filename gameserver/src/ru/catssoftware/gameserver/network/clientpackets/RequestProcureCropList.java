package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Manor;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2ManorManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;


public class RequestProcureCropList extends L2GameClientPacket
{
	private static final String	_C__D0_09_REQUESTPROCURECROPLIST	= "[C] D0:09 RequestProcureCropList";
	private int					_size;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_size = readD();
			if (_size * 16 > getByteBuffer().remaining() || _size > 500)
			{
				_size = 0;
				return;
			}

		_items = new int[_size * 4];
		for (int i = 0; i < _size; i++)
		{
			int objId = readD();
			_items[(i * 4)] = objId;
			int itemId = readD();
			_items[i * 4 + 1] = itemId;
			int manorId = readD();
			_items[i * 4 + 2] = manorId;
			long count = readD();
			if (count > Integer.MAX_VALUE)
				count = Integer.MAX_VALUE;
			_items[i * 4 + 3] = (int) count;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		L2Object target = player.getTarget();

		if (!(target instanceof L2ManorManagerInstance))
			target = player.getLastFolkNPC();

		if (!player.isGM() && (target == null || !(target instanceof L2ManorManagerInstance) || !player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false)))
			return;

		if (_size < 1)
		{
			ActionFailed();
			return;
		}
		L2ManorManagerInstance manorManager = (L2ManorManagerInstance) target;

		int currentManorId = manorManager.getCastle().getCastleId();

		// Calculate summary values
		int slots = 0;
		int weight = 0;

		for (int i = 0; i < _size; i++)
		{
			int itemId = _items[i * 4 + 1];
			int manorId = _items[i * 4 + 2];
			int count = _items[i * 4 + 3];

			if (itemId == 0 || manorId == 0 || count == 0)
				continue;
			if (count < 1)
				continue;

			if (count > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName()
						+ " tried to purchase over " + Integer.MAX_VALUE + " items at the same time.", Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				return;
			}

			Castle castle = CastleManager.getInstance().getCastleById(manorId);
			if (castle == null)
				continue;

			CropProcure crop = castle.getCrop(itemId, CastleManorManager.PERIOD_CURRENT);

			if (crop == null)
				continue;

			int rewardItemId = L2Manor.getInstance().getRewardItem(itemId,crop.getReward());

			L2Item template = ItemTable.getInstance().getTemplate(rewardItemId);

			if (template == null)
				continue;

			weight += count * template.getWeight();

			if (!template.isStackable())
				slots += count;
			else if (player.getInventory().getItemByItemId(itemId) == null)
				slots++;
		}

		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}

		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		// Proceed the purchase
		InventoryUpdate playerIU = new InventoryUpdate();

		for (int i = 0; i < _size; i++)
		{
			int objId = _items[(i * 4)];
			int cropId = _items[i * 4 + 1];
			int manorId = _items[i * 4 + 2];
			int count = _items[i * 4 + 3];

			if (objId == 0 || cropId == 0 || manorId == 0 || count == 0)
				continue;

			if (count < 1)
				continue;

			CropProcure crop = null;

			Castle castle = CastleManager.getInstance().getCastleById(manorId);
			if (castle == null)
				continue;

			crop = castle.getCrop(cropId, CastleManorManager.PERIOD_CURRENT);

			if (crop == null || crop.getId() == 0 || crop.getPrice() == 0)
				continue;

			int fee = 0; // fee for selling to other manors

			int rewardItem = L2Manor.getInstance().getRewardItem(cropId, crop.getReward());

			if (count > crop.getAmount())
				continue;

			int sellPrice = (count * crop.getPrice());
			int rewardPrice = ItemTable.getInstance().getTemplate(rewardItem).getReferencePrice();

			if (rewardPrice == 0)
				continue;

			int rewardItemCount = sellPrice / rewardPrice;
			if (rewardItemCount < 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				continue;
			}

			if (manorId != currentManorId)
				fee = sellPrice * 5 / 100; // 5% fee for selling to other manor

			if (player.getInventory().getAdena() < fee)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				continue;
			}

			// Add item to Inventory and adjust update packet
			L2ItemInstance itemDel = null;
			L2ItemInstance itemAdd = null;
			if (player.getInventory().getItemByObjectId(objId) != null)
			{
				// check if player have correct items count
				L2ItemInstance item = player.getInventory().getItemByObjectId(objId);
				if (item.getCount() < count)
					continue;

				itemDel = player.getInventory().destroyItem("Manor", objId, count, player, manorManager);
				if (itemDel == null)
					continue;

				if (fee > 0)
					player.getInventory().reduceAdena("Manor", fee, player, manorManager);

				crop.setAmount(crop.getAmount() - count);
				if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
					CastleManager.getInstance().getCastleById(manorId).updateCrop(crop.getId(), crop.getAmount(), CastleManorManager.PERIOD_CURRENT);

				itemAdd = player.getInventory().addItem("Manor", rewardItem, rewardItemCount, player, manorManager);
			}
			else
				continue;

			if (itemAdd == null)
				continue;

			playerIU.addRemovedItem(itemDel);
			if (itemAdd.getCount() > rewardItemCount)
				playerIU.addModifiedItem(itemAdd);
			else
				playerIU.addNewItem(itemAdd);

			// Send System Messages
			SystemMessage sm = new SystemMessage(SystemMessageId.TRADED_S2_OF_CROP_S1);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if (fee > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_ADENA_HAS_BEEN_WITHDRAWN_TO_PAY_FOR_PURCHASING_FEES);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if (fee > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(rewardItem);
			sm.addNumber(rewardItemCount);
			player.sendPacket(sm);
		}

		// Send update packets
		player.sendPacket(playerIU);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	@Override
	public String getType()
	{
		return _C__D0_09_REQUESTPROCURECROPLIST;
	}
}