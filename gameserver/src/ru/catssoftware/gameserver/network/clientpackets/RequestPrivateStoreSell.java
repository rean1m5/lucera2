package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.ItemRequest;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.taskmanager.OfflineManager;
import ru.catssoftware.gameserver.util.Util;

public class RequestPrivateStoreSell extends L2GameClientPacket
{
	private static final String	_C__96_REQUESTPRIVATESTORESELL	= "[C] 96 RequestPrivateStoreSell";
	private int					_storePlayerId, _count, _price;
	private ItemRequest[]		_items;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		_count = readD();
		if (_count < 0 || _count * 20 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
			_count = 0;
		_items = new ItemRequest[_count];

		long priceTotal = 0;
		for (int i = 0; i < _count; i++)
		{
			int objectId = readD();
			int itemId = readD();
			readH();
			readH();
			long count = readD();
			int price = readD();

			if (count >= Integer.MAX_VALUE || count < 0)
			{
				String msgErr = "[RequestPrivateStoreSell] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				_count = 0;
				_items = null;
				return;
			}
			_items[i] = new ItemRequest(objectId, itemId, (int) count, price);
			priceTotal += price * count;
		}

		if (priceTotal < 0 || priceTotal >= Integer.MAX_VALUE)
		{
			String msgErr = "[RequestPrivateStoreSell] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
			_count = 0;
			_items = null;
			return;
		}

		_price = (int) priceTotal;
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

		if (storePlayer.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_BUY)
			return;

		TradeList storeList = storePlayer.getBuyList();
		if (storeList == null)
			return;

		if (storePlayer.getAdena() < _price)
		{
			sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			ActionFailed();
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
			return;
		}

		if (!storeList.privateStoreSell(player, _items))
		{
			ActionFailed();
			//_log.warn("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
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
		return _C__96_REQUESTPRIVATESTORESELL;
	}
}