package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestAskJoinPartyRoom extends L2GameClientPacket
{
    private static final String _C__D0_2F_REQUESTASKJOINPARTYROOM = "[C] D0:2F RequestAskJoinPartyRoom";

    private String _name;

    @Override
    protected void readImpl()
    {
    	_name = readS();
    }

    @Override
    protected void runImpl()
    {
    	L2PcInstance activeChar = getActiveChar();
    	if (activeChar == null)
    		return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
        {
        	sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
        	ActionFailed();
            return;
        }

    	L2PartyRoom room = activeChar.getPartyRoom();
    	L2PcInstance target = L2World.getInstance().getPlayer(_name);
    	if (target == null || target == activeChar || room == null)
    	{
    		ActionFailed();
    		return;
    	}
    	else if (target.getPartyRoom() != null)
    	{
    		sendPacket(new SystemMessage(SystemMessageId.S1_NOT_MEET_CONDITIONS_FOR_PARTY_ROOM).addString(_name));
    		ActionFailed();
    		return;
    	}
    	else if (activeChar != room.getLeader())
    	{
    		sendPacket(SystemMessageId.ONLY_ROOM_LEADER_CAN_INVITE);
    		ActionFailed();
    		return;
    	}
    	else if (room.getMemberCount() >= room.getMaxMembers())
    	{
    		sendPacket(SystemMessageId.PARTY_ROOM_FULL);
    		ActionFailed();
    		return;
    	}
    	else if (activeChar.isProcessingRequest())
    	{
    		sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
    		ActionFailed();
    		return;
    	}
    	else if (target.isProcessingRequest())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(_name));
    		ActionFailed();
    		return;
    	}

    	activeChar.onTransactionRequest(target);
		target.sendPacket(new SystemMessage(SystemMessageId.S1_INVITED_YOU_TO_PARTY_ROOM).addPcName(activeChar));
    	target.sendPacket(new ExAskJoinPartyRoom(activeChar.getName()));

    	ActionFailed();
    }

    @Override
    public String getType()
    {
        return _C__D0_2F_REQUESTASKJOINPARTYROOM;
    }
}