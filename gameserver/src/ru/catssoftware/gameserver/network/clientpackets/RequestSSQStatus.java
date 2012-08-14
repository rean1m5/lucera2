package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.SSQStatus;

public class RequestSSQStatus extends L2GameClientPacket
{
	private static final String	_C__C7_RequestSSQStatus	= "[C] C7 RequestSSQStatus";

	private int					_page;

	@Override
	protected void readImpl()
	{
		_page = readC();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if ((SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod()) && _page == 4)
			return;

		SSQStatus ssqs = new SSQStatus(activeChar, _page);
		activeChar.sendPacket(ssqs);
	}

	@Override
	public String getType()
	{
		return _C__C7_RequestSSQStatus;
	}
}
