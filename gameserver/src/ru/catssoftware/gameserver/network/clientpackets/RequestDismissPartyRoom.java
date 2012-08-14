package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestDismissPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_0A_REQUESTDISMISSPARTYROOM = "[C] D0:0A RequestDismissPartyRoom";

	private int _roomId;
	//private int _data2;

    @Override
    protected void readImpl()
    {
		_roomId = readD();
		/*_data2 = */readD();
	}

	@Override
    protected void runImpl()
	{
		L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
			return;

		L2PartyRoom room = activeChar.getPartyRoom();
		if (room != null && room.getId() == _roomId && room.getLeader() == activeChar)
			PartyRoomManager.getInstance().removeRoom(_roomId);

		ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__D0_0A_REQUESTDISMISSPARTYROOM;
	}
}
