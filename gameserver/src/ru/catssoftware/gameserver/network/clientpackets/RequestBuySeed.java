package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2ManorManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;

/**
 * Format: cdd[dd]
 * c    // id (0xC4)
 *
 * d    // manor id
 * d    // seeds to buy
 * [
 * d    // seed id
 * d    // count
 * ]
 * @author l3x
 */
public class RequestBuySeed extends L2GameClientPacket
{
	private static final String	_C__C4_REQUESTBUYSEED	= "[C] C4 RequestBuySeed";

	private int					_count, _manorId;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_count = readD();
		if(_count > 500 || _count * 8 < _buf.remaining() || _count < 1) // check values
			{
				_count = 0;
				return;
			}

		_items = new int[_count * 2];

		for (int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[(i * 2)] = itemId;
			long cnt = readD();
			if (cnt >= Integer.MAX_VALUE || cnt < 1)
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
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (_count < 1)
		{
			ActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if (!(target instanceof L2ManorManagerInstance))
			target = player.getLastFolkNPC();

		if (!(target instanceof L2ManorManagerInstance))
			return;
		if(player.getInventoryLimit()-player.getInventory().getSize()<=_count) {
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		Castle castle = CastleManager.getInstance().getCastleById(_manorId);

		for (int i = 0; i < _count; i++)
		{
			int seedId = _items[(i * 2)];
			int count = _items[i * 2 + 1];
			int price = 0;
			int residual = 0;

			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			price = seed.getPrice();
			residual = seed.getCanProduce();

			if (price <= 0)
				return;

			if (residual < count)
				return;

			totalPrice += count * price;

			L2Item template = ItemTable.getInstance().getTemplate(seedId);
			totalWeight += count * template.getWeight();
			if (!template.isStackable())
				slots += count;
			else if (player.getInventory().getItemByItemId(seedId) == null)
				slots++;
		}

		if (totalPrice >= Integer.MAX_VALUE)
		{
			Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытается превысить лимит скупки за адену.", Config.DEFAULT_PUNISH);
			return;
		}
		if (!player.getInventory().validateWeight(totalWeight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		if ((totalPrice < 0) || !player.reduceAdena("Buy", (int) totalPrice, target, false))
		{
			sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}

		castle.addToTreasuryNoTax((int) totalPrice);

		InventoryUpdate playerIU = new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int seedId = _items[(i * 2)];
			int count = _items[i * 2 + 1];
			if (count < 0)
				count = 0;

			// Update Castle Seeds Amount
			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			seed.setCanProduce(seed.getCanProduce() - count);
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleById(_manorId).updateSeed(seed.getId(), seed.getCanProduce(), CastleManorManager.PERIOD_CURRENT);

			// Add item to Inventory and adjust update packet
			L2ItemInstance item = player.getInventory().addItem("Buy", seedId, count, player, target);

			if (item.getCount() > count)
				playerIU.addModifiedItem(item);
			else
				playerIU.addNewItem(item);

			// Send Char Buy Messages
			SystemMessage sm = null;
			sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(item);
			sm.addNumber(count);
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
		return _C__C4_REQUESTBUYSEED;
	}
}