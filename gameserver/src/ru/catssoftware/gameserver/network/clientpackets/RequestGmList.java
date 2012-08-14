package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.GmListTable;

public class RequestGmList extends L2GameClientPacket
{
	private static final String	_C__81_REQUESTGMLIST	= "[C] 81 RequestGmList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() == null)
			return;
		GmListTable.getInstance().sendListToPlayer(getClient().getActiveChar());
	}

	@Override
	public String getType()
	{
		return _C__81_REQUESTGMLIST;
	}
}
