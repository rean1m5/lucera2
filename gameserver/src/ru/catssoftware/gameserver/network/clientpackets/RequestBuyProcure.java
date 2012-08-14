package ru.catssoftware.gameserver.network.clientpackets;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Manor;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2ManorManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;

import javolution.util.FastList;

@SuppressWarnings("unused")
public class RequestBuyProcure extends L2GameClientPacket
{
	private static final String	_C__C3_REQUESTBUYPROCURE	= "[C] C3 RequestBuyProcure";
	private int					_listId, _count;
	private int[]				_items;
	private List<CropProcure>	_procureList				= new FastList<CropProcure>();

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
		if (_count > 500) // protect server
		{
			_count = 0;
			return;
		}

		_items = new int[_count * 2];
		for (int i = 0; i < _count; i++)
		{
			long servise = readD();
			int itemId = readD(); _items[(i * 2)] = itemId;
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
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		player._bbsMultisell = 0;
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;

		L2Object target = player.getTarget();

		if (_count < 1)
		{
			ActionFailed();
			return;
		}

		long subTotal = 0;
		int tax = 0;

		int slots = 0;
		int weight = 0;
		L2ManorManagerInstance manor = (target instanceof L2ManorManagerInstance) ? (L2ManorManagerInstance) target : null;
		if(player.getInventoryLimit()-player.getInventory().getSize()<=_count) {
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[(i * 2)];
			int count = _items[i * 2 + 1];
			int price = 0;

			if (count >= Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " хотел купить завышеное кол-во итемов. Макс: " + 
					Integer.MAX_VALUE + ".", Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(
					L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward()));
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
		_procureList = manor.getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT);

		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[(i * 2)];
			int count = _items[i * 2 + 1];
			if (count < 0)
				count = 0;

			int rewardItemId = L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward());

			int rewardItemCount = 1;

			rewardItemCount = count / rewardItemCount;

			// Add item to Inventory and adjust update packet
			L2ItemInstance item = player.getInventory().addItem("Manor", rewardItemId, rewardItemCount, player, manor);
			L2ItemInstance iteme = player.getInventory().destroyItemByItemId("Manor", itemId, count, player, manor);

			if (item == null || iteme == null)
				continue;

			playerIU.addRemovedItem(iteme);
			if (item.getCount() > rewardItemCount)
				playerIU.addModifiedItem(item);
			else
				playerIU.addNewItem(item);

			// Send Char Buy Messages
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(item);
			sm.addNumber(rewardItemCount);
			player.sendPacket(sm);
			sm = null;
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
		return _C__C3_REQUESTBUYPROCURE;
	}
}