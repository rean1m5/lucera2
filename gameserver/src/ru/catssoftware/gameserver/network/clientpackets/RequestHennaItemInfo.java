package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.HennaTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.HennaItemInfo;
import ru.catssoftware.gameserver.templates.item.L2Henna;

public class RequestHennaItemInfo extends L2GameClientPacket
{
	private static final String	_C__BB_RequestHennaItemInfo	= "[C] bb RequestHennaItemInfo";
	private int					_symbolId;

	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
		if (template == null)
			return;

		activeChar.sendPacket(new HennaItemInfo(template, activeChar));
	}

	@Override
	public String getType()
	{
		return _C__BB_RequestHennaItemInfo;
	}
}
