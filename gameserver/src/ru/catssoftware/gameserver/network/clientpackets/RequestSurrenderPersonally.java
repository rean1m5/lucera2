package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestSurrenderPersonally extends L2GameClientPacket
{
	private static final String	_C__69_REQUESTSURRENDERPERSONALLY	= "[C] 69 RequestSurrenderPersonally";
	String						_pledgeName;
	L2Clan						_clan;
	L2PcInstance				_activeChar;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		_activeChar = getClient().getActiveChar();
		if (_activeChar == null)
			return;
		_log.info("RequestSurrenderPersonally by " + getClient().getActiveChar().getName() + " with " + _pledgeName);
		_clan = getClient().getActiveChar().getClan();
		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if (_clan == null)
			return;
		if (clan == null)
		{
			_activeChar.sendMessage("Нет такого клана.");
			ActionFailed();
			return;
		}
		if (!_clan.isAtWarWith(clan.getClanId()) || _activeChar.getWantsPeace() == 1)
		{
			_activeChar.sendMessage("У вас нет войны с этим кланом.");
			ActionFailed();
			return;
		}

		_activeChar.setWantsPeace(1);
		_activeChar.deathPenalty(false, false);
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_PERSONALLY_SURRENDERED_TO_THE_S1_CLAN);
		msg.addString(_pledgeName);
		_activeChar.sendPacket(msg);
		msg = null;
		ClanTable.getInstance().checkSurrender(_clan, clan);
	}

	@Override
	public String getType()
	{
		return _C__69_REQUESTSURRENDERPERSONALLY;
	}
}
