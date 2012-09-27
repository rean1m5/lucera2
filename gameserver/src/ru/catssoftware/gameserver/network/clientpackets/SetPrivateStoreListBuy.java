package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import ru.catssoftware.gameserver.network.serverpackets.PrivateStoreMsgBuy;


public class SetPrivateStoreListBuy extends L2GameClientPacket
{
	private static final String	_C__91_SETPRIVATESTORELISTBUY	= "[C] 91 SetPrivateStoreListBuy";

	private int					_count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_count = readD();
			if (_count <= 0 || _count * 12 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
			{
				_count = 0;
				_items = null;
				return;
			}
		_items = new int[_count * 3];
		for (int x = 0; x < _count; x++)
		{
			int itemId = readD(); _items[(x * 3)] = itemId;
			readH();//TODO: analyse this
			readH();//TODO: analyse this
			long cnt = readD();
			if (cnt >= Integer.MAX_VALUE || cnt < 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[x * 3 + 1] = (int) cnt;
			int price = readD();
			_items[x * 3 + 2] = price;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (player.isOfflineTrade())
			return;
		
		player.stopMove();
		if(player.getObservMode()!=0 || Olympiad.getInstance().isRegistered(player) || Olympiad.getInstance().isRegisteredInComp(player)) {
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		
		if(player.isCastingNow()) {
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		if ( Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}
		for(L2PcInstance pc: player.getKnownList().getKnownPlayersInRadius(player.getTemplate().getCollisionRadius()*2)) {
			if(pc.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE) {
				player.sendMessage("Вы слишком близко от другого торговца");
				ActionFailed();
				return;
				
			}
		}

		TradeList tradeList = player.getBuyList();
		tradeList.clear();

		int cost = 0;
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[(i * 3)];
			int count = _items[i * 3 + 1];
			int price = _items[i * 3 + 2];

			tradeList.addItemByItemId(itemId, count, price);
			cost += count * price;
		}

		if (_count <= 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo(true);
			return;
		}

		// Check maximum number of allowed slots for pvt shops
		if (_count > player.getPrivateBuyStoreLimit())
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}

		// Check for available funds
		if (cost > player.getAdena() || cost <= 0)
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY);
			return;
		}
		
		if (Config.CHECK_ZONE_ON_PVT && !player.isInsideZone(L2Zone.FLAG_TRADE))
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isInsideZone(L2Zone.FLAG_NOSTORE))
		{
			player.sendPacket(new PrivateStoreManageListBuy(player));
			sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.getParty()!=null)
			player.getParty().removePartyMember(player);
		player.sitDown();
		player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY);
		player.broadcastUserInfo(true);
		player.broadcastPacket(new PrivateStoreMsgBuy(player));
	}

	@Override
	public String getType()
	{
		return _C__91_SETPRIVATESTORELISTBUY;
	}
}