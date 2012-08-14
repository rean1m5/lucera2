package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestExitPartyMatchingWaitingRoom extends L2GameClientPacket
{
    private static final String _C__D0_25_REQUESTEXITPARTYMATCHINGWAITINGROOM = "[C] D0:25 RequestExitPartyMatchingWaitingRoom";

    @Override
    protected void readImpl()
    {
    	// trigger packet
    }

    @Override
    protected void runImpl()
    {
        L2PcInstance activeChar = getActiveChar();
        if (activeChar == null)
        	return;

        PartyRoomManager.getInstance().removeFromWaitingList(activeChar);

        ActionFailed();
    }

    @Override
    public String getType()
    {
        return _C__D0_25_REQUESTEXITPARTYMATCHINGWAITINGROOM;
    }
}
