package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestReplyStartPledgeWar extends L2GameClientPacket
{
	private static final String	_C__4e_REQUESTREPLYSTARTPLEDGEWAR	= "[C] 4e RequestReplyStartPledgeWar";

	private int					_answer;

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
		if (activeChar == null)
			return;
		L2PcInstance requestor = activeChar.getActiveRequester();
		if (requestor == null)
			return;

		if (_answer == 1)
		{
			ClanTable.getInstance().storeclanswars(requestor.getClanId(), activeChar.getClanId());
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_WAR_PROCLAMATION_HAS_BEEN_REFUSED).addString(activeChar.getClan().getName()));
		}
		activeChar.setActiveRequester(null);
		requestor.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__4e_REQUESTREPLYSTARTPLEDGEWAR;
	}
}
