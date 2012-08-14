package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.MercTicketManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class MercTicket implements IItemHandler
{
	private static final String[]	MESSAGES	= { "To arms!", "I am ready to serve you my lord when the time comes.", "You summon me." };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		L2PcInstance activeChar = (L2PcInstance) playable;
		Castle castle = CastleManager.getInstance().getCastle(activeChar);
		int castleId = -1;
		if (castle != null)
			castleId = castle.getCastleId();

		//add check that certain tickets can only be placed in certain castles
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			if (castleId == -1)
			{
				// player is not in a castle
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE));
				return;
			}

			switch (castleId)
			{
				case 1:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Gludio"));
					return;
				case 2:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Dion"));
					return;
				case 3:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Giran"));
					return;
				case 4:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Oren"));
					return;
				case 5:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Aden"));
					return;
				case 6:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Heine"));
					return;
				case 7:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Goddard"));
					return;
				case 8:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Rune"));
					return;
				case 9:
					activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_MERCHANT_FUNC_ONLY_IN_CASTLE),"Schuttgart"));
					return;
				}
		}

		if ((activeChar.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) != L2Clan.CP_CS_MERCENARIES)
		{
			activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES);
			return;
		}

		if (castle != null && castle.getSiege().getIsInProgress())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_POSITION_MERCS_DURING_SIEGE);
			return;
		}

		//Checking Seven Signs Quest Period
		if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION)
		{
			activeChar.sendPacket(SystemMessageId.MERC_CAN_BE_ASSIGNED);
			return;
		}
		//Checking the Seal of Strife status
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_NULL:
				if (SevenSigns.getInstance().checkIsDawnPostingTicket(itemId))
				{
					activeChar.sendPacket(SystemMessageId.MERC_CAN_BE_ASSIGNED);
					return;
				}
				break;
			case SevenSigns.CABAL_DUSK:
				if (!SevenSigns.getInstance().checkIsRookiePostingTicket(itemId))
				{
					activeChar.sendPacket(SystemMessageId.MERC_CAN_BE_ASSIGNED);
					return;
				}
				break;
			case SevenSigns.CABAL_DAWN:
				break;
		}

		if (MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}
		if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}
		if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ()))
		{
			activeChar.sendPacket(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT);
			return;
		}

		MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, MESSAGES);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
	}

	// left in here for backward compatibility
	public int[] getItemIds()
	{
		return MercTicketManager.getInstance().getItemIds();
	}
}