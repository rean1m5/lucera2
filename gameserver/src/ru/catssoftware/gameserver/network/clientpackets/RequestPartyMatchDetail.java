package ru.catssoftware.gameserver.network.clientpackets;

import java.util.List;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;


public class RequestPartyMatchDetail extends L2GameClientPacket
{
	private static final String _C__81_REQUESTPARTYMATCHDETAIL = "[C] 81 RequestPartyMatchDetail";

	// manual join
	private int		_roomId;
	// auto join
	private int		_region;
	//private int		_data2;

	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_region = readD();
		/*_data2 = */readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.getPartyRoom() != null || activeChar.getParty() != null)
		{
			sendPacket(SystemMessageId.PARTY_ROOM_FORBIDDEN);
			ActionFailed();
			return;
		}

		activeChar.setPartyMatchingRegion(_region);
		activeChar.setPartyMatchingShowClass(false);

		if (_roomId > 0)
		{
			L2PartyRoom room = PartyRoomManager.getInstance().getPartyRoom(_roomId);
			L2PartyRoom.tryJoin(activeChar, room, false);
		}
		else
		{
			List<L2PartyRoom> list = PartyRoomManager.getInstance().getRooms(activeChar);
			for (L2PartyRoom room : list)
			{
				if (room.canJoin(activeChar))
				{
					room.addMember(activeChar);
					break;
				}
			}
		}

		ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__81_REQUESTPARTYMATCHDETAIL;
	}
}
