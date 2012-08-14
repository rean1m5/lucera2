package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.ItemRequest;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.TradeList.TradeItem;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.taskmanager.OfflineManager;
import ru.catssoftware.gameserver.util.Util;

public class RequestPrivateStoreBuy extends L2GameClientPacket
{
	private static final String	_C__79_REQUESTPRIVATESTOREBUY	= "[C] 79 RequestPrivateStoreBuy";

	private int					_storePlayerId;
	private int					_count;
	private ItemRequest[]		_items;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		_count = readD();
		if (_count < 0 || _count * 12 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
				_count = 0;
		_items = new ItemRequest[_count];

		for (int i = 0; i < _count; i++)
		{
			int objectId = readD();
			long count = readD();
			if (count > Integer.MAX_VALUE)
				count = Integer.MAX_VALUE;
			int price = readD();

			_items[i] = new ItemRequest(objectId, (int) count, price);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null || player.isCursedWeaponEquipped())
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		L2Object object = null;

		// Get object from target
		if (player.getTargetId() == _storePlayerId)
			object = player.getTarget();

		// Get object from world
		if (object == null)
			object = L2World.getInstance().getPlayer(_storePlayerId);

		if (!(object instanceof L2PcInstance))
			return;

		L2PcInstance storePlayer = (L2PcInstance) object;

		if (!(storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
			return;

		TradeList storeList = storePlayer.getSellList();
		if (storeList == null)
			return;

		if(player.getInventoryLimit()-player.getInventory().getSize()<=_items.length) {
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		long priceTotal = 0;
		for (ItemRequest ir : _items)
		{
			if (ir.getCount() < 0)
			{
				String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			TradeItem sellersItem = storeList.getItem(ir.getObjectId());
			if (sellersItem == null)
			{
				String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName()
						+ " tried to buy an item not sold in a private store (buy), ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			if (ir.getPrice() != sellersItem.getPrice())
			{
				String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName()
						+ " tried to change the seller's price in a private store (buy), ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			priceTotal += ir.getPrice() * ir.getCount();
		}

		if (priceTotal < 0 || priceTotal >= Integer.MAX_VALUE)
		{
			String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
			return;
		}

		if (player.getAdena() < priceTotal)
		{
			sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			ActionFailed();
			return;
		}

		if (storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
		{
			if (storeList.getItemCount() > _count)
			{
				String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried to buy less items then sold by package-sell, ban this player for bot-usage!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
		}

		if (!storeList.privateStoreBuy(player, _items, (int) priceTotal))
		{
			ActionFailed();
			return;
		}

		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();

			if(storePlayer.isOfflineTrade())
				OfflineManager.getInstance().removeTrader(storePlayer);
		}
	}

	@Override
	public String getType()
	{
		return _C__79_REQUESTPRIVATESTOREBUY;
	}
}