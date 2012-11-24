package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.TradeDone;

public class AnswerTradeRequest extends L2GameClientPacket
{
	private static final String	_C__40_ANSWERTRADEREQUEST	= "[C] 40 AnswerTradeRequest";

	private int					_response;

	@Override
	protected void readImpl()
	{
		_response = readD();
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

		L2PcInstance partner = player.getActiveRequester();
		if (partner == null || L2World.getInstance().getPlayer(partner.getObjectId()) == null)
		{
			// Trade partner not found, cancel trade
			player.sendPacket(new TradeDone(0));
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.setActiveRequester(null);
			ActionFailed();
			return;
		}

		if(!player.isInsideRadius(partner, 200, false, false)) {
			player.sendPacket(new TradeDone(0));
			player.setActiveRequester(null);
			ActionFailed();
			return;
		}
		
		if (_response == 1 && !partner.isRequestExpired() && getClient().checkKeyProtection())
			player.startTrade(partner);
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.S1_DENIED_TRADE_REQUEST);
			msg.addString(player.getName());
			partner.sendPacket(msg);
			ActionFailed();
		}

		// Clears requesting status
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__40_ANSWERTRADEREQUEST;
	}
}