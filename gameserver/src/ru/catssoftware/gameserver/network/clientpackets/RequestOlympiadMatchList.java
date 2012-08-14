package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;

public class RequestOlympiadMatchList extends L2GameClientPacket
{
	private static final String	_C__D0_13_REQUESTOLYMPIADMATCHLIST	= "[C] D0:13 RequestOlympiadMatchList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (activeChar.inObserverMode())
			Olympiad.sendMatchList(activeChar);
	}

	@Override
	public String getType()
	{
		return _C__D0_13_REQUESTOLYMPIADMATCHLIST;
	}
}