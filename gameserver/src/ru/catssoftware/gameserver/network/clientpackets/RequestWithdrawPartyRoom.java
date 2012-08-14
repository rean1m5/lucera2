package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestWithdrawPartyRoom extends L2GameClientPacket
{
	private static final String	_C__D0_0B_REQUESTWITHDRAWPARTYROOM	= "[C] D0:0B RequestWithdrawPartyRoom";

	private int					_roomId;
	@SuppressWarnings("unused")
	private int					_data2;

	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_data2 = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
			return;

		L2Party party = activeChar.getParty();
		if (party != null && party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(activeChar))
		{
			sendPacket(SystemMessageId.COULD_NOT_LEAVE_PARTY);
			ActionFailed();
			return;
		}

		L2PartyRoom room = activeChar.getPartyRoom();
		if (room != null && room.getId() == _roomId)
		{
			if (room.getLeader() == activeChar)
				PartyRoomManager.getInstance().removeRoom(_roomId);
			else if (party != null)
				party.removePartyMember(activeChar, false);
			else
				room.removeMember(activeChar, false);
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_0B_REQUESTWITHDRAWPARTYROOM;
	}
}
