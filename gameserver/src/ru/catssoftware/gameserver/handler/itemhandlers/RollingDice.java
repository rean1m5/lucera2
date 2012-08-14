package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.Dice;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.Broadcast;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.tools.random.Rnd;

public class RollingDice implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 4625, 4626, 4627, 4628 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if (itemId == 4625 || itemId == 4626 || itemId == 4627 || itemId == 4628)
		{
			int number = rollDice(activeChar);
			if (number == 0)
			{
				activeChar.sendPacket(SystemMessageId.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIME_TRY_AGAIN_LATER);
				return;
			}

			Dice d = new Dice(activeChar.getObjectId(), item.getItemId(), number, activeChar.getX() - 30, activeChar.getY() - 30, activeChar.getZ());
			Broadcast.toSelfAndKnownPlayers(activeChar, d);

			SystemMessage sm = new SystemMessage(SystemMessageId.S1_ROLLED_S2);
			sm.addString(activeChar.getName());
			sm.addNumber(number);

			activeChar.sendPacket(sm);
			if (!TownManager.getInstance().checkIfInZone(activeChar))
				Broadcast.toKnownPlayers(activeChar, sm);
			else if (activeChar.isInParty())
				activeChar.getParty().broadcastToPartyMembers(activeChar, sm);
		}
	}

	private int rollDice(L2PcInstance player)
	{
		// Check if the dice is ready
		if (!FloodProtector.tryPerformAction(player, Protected.ROLLDICE))
			return 0;

		return Rnd.get(1, 6);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}