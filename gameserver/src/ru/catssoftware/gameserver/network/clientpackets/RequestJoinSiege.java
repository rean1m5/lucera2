package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestJoinSiege extends L2GameClientPacket
{
	private static final String	_C__A4_RequestJoinSiege	= "[C] a4 RequestJoinSiege";

	private int					_castleId;
	private int					_isAttacker;
	private int					_isJoining;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_isAttacker = readD();
		_isJoining = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (!activeChar.isClanLeader())
			return;

		Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle == null && _castleId!=34 && _castleId!=64)
			return;
		if (castle==null && _isAttacker == 0) // ClanHall have no defender clans
			return;
		if (_isJoining == 1)
		{
			if (System.currentTimeMillis() < activeChar.getClan().getDissolvingExpiryTime())
			{
				activeChar.sendPacket(SystemMessageId.CANT_PARTICIPATE_IN_SIEGE_WHILE_DISSOLUTION_IN_PROGRESS);
				return;
			}
			if (_isAttacker == 1)
				if (_castleId==34)
					DevastatedCastleSiege.getInstance().registerClan(activeChar);
				else if (_castleId==64)
					FortressOfDeadSiege.getInstance().registerClan(activeChar);
				else
					castle.getSiege().registerAttacker(activeChar);
			else
				if (castle!=null)
					castle.getSiege().registerDefender(activeChar);
		}
		else
			if (_castleId==34)
				DevastatedCastleSiege.getInstance().removeSiegeClan(activeChar);
			else if (_castleId==64)
				FortressOfDeadSiege.getInstance().removeSiegeClan(activeChar);
			else
				castle.getSiege().removeSiegeClan(activeChar);
		if (_castleId==34)
			DevastatedCastleSiege.getInstance().listRegisterClan(activeChar);
		else if (_castleId==64)
			FortressOfDeadSiege.getInstance().listRegisterClan(activeChar);
		else
			castle.getSiege().listRegisterClan(activeChar);
	}

	@Override
	public String getType()
	{
		return _C__A4_RequestJoinSiege;
	}
}