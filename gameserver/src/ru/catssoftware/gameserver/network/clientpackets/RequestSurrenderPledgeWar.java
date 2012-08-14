package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestSurrenderPledgeWar extends L2GameClientPacket
{
	private static final String	_C__51_REQUESTSURRENDERPLEDGEWAR	= "[C] 51 RequestSurrenderPledgeWar";
	String						_pledgeName;
	L2Clan						_clan;
	L2PcInstance				activeChar;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		activeChar = getClient().getActiveChar();
		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		_clan = activeChar.getClan();
	
		if (activeChar == null)
			return;
		if (!activeChar.isClanLeader())
		{
			activeChar.sendMessage("Вы не клан лидер!");
			return;
		}
		if (_clan == null)
			return;
		if (clan == null)
		{
			activeChar.sendMessage("Нет такого клана.");
			ActionFailed();
			return;
		}
		if (!_clan.isAtWarWith(clan.getClanId()))
		{
			activeChar.sendMessage("У вас нет войны с этим кланом.");
			ActionFailed();
			return;
		}
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
		msg.addString(_pledgeName);
		activeChar.sendPacket(msg);
		msg = null;
		activeChar.deathPenalty(false, false);
		ClanTable.getInstance().deleteclanswars(_clan.getClanId(), clan.getClanId());
	}

	@Override
	public String getType()
	{
		return _C__51_REQUESTSURRENDERPLEDGEWAR;
	}
}