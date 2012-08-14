package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.TradeOtherAdd;
import ru.catssoftware.gameserver.network.serverpackets.TradeOwnAdd;
import ru.catssoftware.gameserver.network.serverpackets.TradeUpdate;

public class AddTradeItem extends L2GameClientPacket
{
	private static final String	_C__16_ADDTRADEITEM	= "[C] 16 AddTradeItem";

	private int					_tradeId;
	private int					_objectId;
	private int					_count;

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			_log.info("Player: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
			return;
		}

		if (trade.getPartner() == null || L2World.getInstance().getPlayer(trade.getPartner().getObjectId()) == null)
		{
			// Trade partner not found, cancel trade
			if (trade.getPartner() != null)
				_log.info("Player:" + player.getName() + " requested invalid trade object: " + _objectId);
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.cancelActiveTrade();
			return;
		}
		
		TradeList partnerTrade = trade.getPartner().getActiveTradeList();
		if (partnerTrade == null)
			return;

		if (trade.isConfirmed() || partnerTrade.isConfirmed())
		{
			player.sendPacket(SystemMessageId.ONCE_THE_TRADE_IS_CONFIRMED_THE_ITEM_CANNOT_BE_MOVED_AGAIN);
			return;
		}
		if (!player.validateItemManipulation(_objectId, "trade") && !player.isGM())
		{
			player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}

		TradeList.TradeItem item = trade.addItem(_objectId, _count);

		if (item != null && trade.getPartner().getTrading() && player.getTrading())
		{
			player.sendPacket(new TradeOwnAdd(item));
			player.sendPacket(new TradeUpdate(trade,player));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
	}

	@Override
	public String getType()
	{
		return _C__16_ADDTRADEITEM;
	}
}