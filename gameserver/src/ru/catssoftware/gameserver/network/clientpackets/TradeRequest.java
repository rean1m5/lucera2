package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SendTradeRequest;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class TradeRequest extends L2GameClientPacket
{
	private static final String	TRADEREQUEST__C__15	= "[C] 15 TradeRequest";

	private int					_objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;

		player._bbsMultisell = 0;
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		L2Object obj = null;

		// Get object from target
		if (player.getTargetId() == _objectId)
			obj = player.getTarget();

		// Get object from world
		if (obj == null)
			obj = L2World.getInstance().getPlayer(_objectId);

		if (!(obj instanceof L2PcInstance) || obj.getObjectId() == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}
		L2PcInstance partner = (L2PcInstance) obj;

		if (partner.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendMessage("На олимпиаде трэйд запрещен");
			return;
		}
		if(partner.isCastingNow() || partner.isTeleporting()) {
			player.sendMessage("Игрок занят");
			return;
		}

		if (BlockList.isBlocked(partner, player))
		{
			player.sendMessage("Игрок игнорирует вас");
			return;
		}

		if (player.getDistanceSq(partner) > 22500) // 150
		{
			player.sendPacket(SystemMessageId.TARGET_TOO_FAR);
			return;
		}

		if (!player.canSee(partner))
		{
			player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			return;
		}

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || partner.getKarma() > 0))
		{
			player.sendMessage("Игрок ПК не может принять трэйд.");
			return;
		}

		if (player.getPrivateStoreType() != 0 || partner.getPrivateStoreType() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}

		if (partner.isProcessingRequest() || partner.isProcessingTransaction())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName());
			player.sendPacket(sm);
			return;
		}

		if (partner.getTradeRefusal())
		{
			player.sendMessage("Цель не может принять трэйд.");
			return;
		}

		if (partner.isInCombat())
		{
			player.sendMessage("Цель находится в бою.");
			return;
		}
		
		player.onTransactionRequest(partner);
		partner.sendPacket(new SendTradeRequest(player.getObjectId()));
		SystemMessage sm = new SystemMessage(SystemMessageId.REQUEST_S1_FOR_TRADE);
		sm.addString(partner.getName());
		player.sendPacket(sm);
	}

	@Override
	public String getType()
	{
		return TRADEREQUEST__C__15;
	}
}