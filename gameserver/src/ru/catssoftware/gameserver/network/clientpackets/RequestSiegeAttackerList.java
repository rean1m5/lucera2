package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.serverpackets.SiegeAttackerList;

public class RequestSiegeAttackerList extends L2GameClientPacket
{
	private static final String	_C__A2_RequestSiegeAttackerList	= "[C] a2 RequestSiegeAttackerList";

	private int					_castleId;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
	}

	@Override
	protected void runImpl()
	{
		Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle != null)
			sendPacket(new SiegeAttackerList(castle,null));
		else
		{
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_castleId);
			if (clanHall!=null)
				sendPacket(new SiegeAttackerList(null,clanHall));
		}
	}

	@Override
	public String getType()
	{
		return _C__A2_RequestSiegeAttackerList;
	}
}