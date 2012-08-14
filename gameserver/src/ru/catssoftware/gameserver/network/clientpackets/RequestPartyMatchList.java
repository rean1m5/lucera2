package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;


public class RequestPartyMatchList extends L2GameClientPacket
{
	private static final String _C__80_REQUESTPARTYMATCHLIST = "[C] 80 RequestPartyMatchList";

	private int _lootDist;
	private int _maxMembers;
	private int _minLevel;
	private int _maxLevel;
	private int _roomId;
	private String _roomTitle;

	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_maxMembers = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_lootDist = readD();
		_roomTitle = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
			return;

		L2Party party = activeChar.getParty();
		if (party != null && !party.isLeader(activeChar))
		{
			ActionFailed();
			return;
		}

		L2PartyRoom room = activeChar.getPartyRoom();
		if (room == null)
		{
			PartyRoomManager.getInstance().createRoom(activeChar, _minLevel, _maxLevel, _maxMembers, _lootDist, _roomTitle);
			sendPacket(SystemMessageId.PARTY_ROOM_CREATED);
		}
		else if (room.getId() == _roomId)
		{
			room.setLootDist(_lootDist);
			room.setMaxMembers(_maxMembers);
			room.setMinLevel(_minLevel);
			room.setMaxLevel(_maxLevel);
			room.setTitle(_roomTitle);
			room.updateRoomStatus(false);
			room.broadcastPacket(SystemMessageId.PARTY_ROOM_REVISED.getSystemMessage());
		}

		ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__80_REQUESTPARTYMATCHLIST;
	}
}
