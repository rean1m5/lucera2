package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.GmListTable;
import ru.catssoftware.gameserver.instancemanager.PetitionManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestPetitionCancel extends L2GameClientPacket
{
	private static final String	_C__80_REQUEST_PETITIONCANCEL	= "[C] 80 RequestPetitionCancel";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			if (activeChar.isGM())
				PetitionManager.getInstance().endActivePetition(activeChar);
			else
				activeChar.sendPacket(SystemMessageId.PETITION_UNDER_PROCESS);
		}
		else
		{
			if (PetitionManager.getInstance().isPlayerPetitionPending(activeChar))
			{
				if (PetitionManager.getInstance().cancelActivePetition(activeChar))
				{
					int numRemaining = Config.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar);

					SystemMessage sm = new SystemMessage(SystemMessageId.PETITION_CANCELED_SUBMIT_S1_MORE_TODAY);
					sm.addString(String.valueOf(numRemaining));
					activeChar.sendPacket(sm);
					sm = null;

					// Notify all GMs that the player's pending petition has been cancelled.
					String msgContent = activeChar.getName() + " отменяет петицию.";
					GmListTable.broadcastToGMs(new CreatureSay(activeChar.getObjectId(), SystemChatChannelId.Chat_Hero, "Petition System", msgContent));
				}
				else
					activeChar.sendPacket(SystemMessageId.FAILED_CANCEL_PETITION_TRY_LATER);
			}
			else
				activeChar.sendPacket(SystemMessageId.PETITION_NOT_SUBMITTED);
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__80_REQUEST_PETITIONCANCEL;
	}
}