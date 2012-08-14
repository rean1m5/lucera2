package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.serverpackets.SiegeDefenderList;

public class RequestConfirmSiegeWaitingList extends L2GameClientPacket
{
	private static final String	_C__A5_RequestConfirmSiegeWaitingList	= "[C] a5 RequestConfirmSiegeWaitingList";
	private int					_approved, _castleId, _clanId;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_clanId = readD();
		_approved = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		L2Clan clan = ClanTable.getInstance().getClan(_clanId);

		if (activeChar == null)
			return;
		// Check if the player has a clan
		if (activeChar.getClan() == null)
			return;
		if (castle == null)
			return;
		// Check if leader of the clan who owns the castle?
		if ((castle.getOwnerId() != activeChar.getClanId()) || (!activeChar.isClanLeader()))
			return;	
		if (clan == null)
			return;
		if (!castle.getSiege().getIsRegistrationOver())
		{
			if (_approved == 1)
			{
				if (castle.getSiege().checkIsDefenderWaiting(clan))
					castle.getSiege().approveSiegeDefenderClan(_clanId);
				else
					return;
			}
			else
			{
				if ((castle.getSiege().checkIsDefenderWaiting(clan)) || (castle.getSiege().checkIsDefender(clan)))
					castle.getSiege().removeSiegeClan(_clanId);
			}
		}
		activeChar.sendPacket(new SiegeDefenderList(castle));
	}

	@Override
	public String getType()
	{
		return _C__A5_RequestConfirmSiegeWaitingList;
	}
}
