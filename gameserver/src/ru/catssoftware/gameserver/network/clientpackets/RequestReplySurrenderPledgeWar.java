package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestReplySurrenderPledgeWar extends L2GameClientPacket
{
	private static final String	_C__52_REQUESTREPLYSURRENDERPLEDGEWAR	= "[C] 52 RequestReplySurrenderPledgeWar";
	int							_answer;

	@Override
	protected void readImpl()
	{
		@SuppressWarnings("unused")
		String _reqName = readS();
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2PcInstance requestor = activeChar.getActiveRequester();
		if (activeChar == null)
			return;
		if (requestor == null)
			return;
		if (_answer == 1)
		{
			requestor.deathPenalty(false, false);
			ClanTable.getInstance().deleteclanswars(requestor.getClanId(), activeChar.getClanId());
		}
		activeChar.onTransactionRequest(null);
	}

	@Override
	public String getType()
	{
		return _C__52_REQUESTREPLYSURRENDERPLEDGEWAR;
	}
}
