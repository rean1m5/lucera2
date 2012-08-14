package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PledgeInfo;

public class RequestPledgeInfo extends L2GameClientPacket
{
	private static final String	_C__66_REQUESTPLEDGEINFO	= "[C] 66 RequestPledgeInfo";
	private int					_clanId;

	@Override
	protected void readImpl()
	{
		_clanId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Clan clan = ClanTable.getInstance().getClan(_clanId);

		if (clan == null)
			return;

		PledgeInfo pc = new PledgeInfo(clan);
		activeChar.sendPacket(pc);
	}

	@Override
	public String getType()
	{
		return _C__66_REQUESTPLEDGEINFO;
	}
}
