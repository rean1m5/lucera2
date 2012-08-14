package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class AnswerJoinPartyRoom extends L2GameClientPacket
{
    private static final String _C__D0_30_ANSWERJOINPARTYROOM = "[C] D0:30 AnswerJoinPartyRoom";

    private int _response;

    @Override
    protected void readImpl()
    {
        _response = readD();
    }

    @Override
    protected void runImpl()
    {
    	L2PcInstance activeChar = getActiveChar();
		if (activeChar == null) 
			return;
		if(activeChar.isInOlympiadMode()) {
        	sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
    		ActionFailed();
    		return;
		}
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
        {
        	sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
        	ActionFailed();
            return;
        }

		L2PcInstance requester = activeChar.getActiveRequester();
		if (requester == null)
		{
			ActionFailed();
			return;
		}

		if (_response == 1) // takes care of everything
			L2PartyRoom.tryJoin(activeChar, requester.getPartyRoom(), true);
		else
			sendPacket(SystemMessageId.PARTY_MATCHING_REQUEST_NO_RESPONSE);

		// Clears requesting status
		activeChar.setActiveRequester(null);
		requester.onTransactionResponse();

		ActionFailed();
    }

    @Override
    public String getType()
    {
        return _C__D0_30_ANSWERJOINPARTYROOM;
    }
}
