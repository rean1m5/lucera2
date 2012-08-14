package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestShortCutDel extends L2GameClientPacket
{
	private static final String	_C__35_REQUESTSHORTCUTDEL	= "[C] 35 RequestShortCutDel";
	private int					_slot, _page;

	@Override
	protected void readImpl()
	{
		int id = readD();
		_slot = id % 12;
		_page = id / 12;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		activeChar.deleteShortCut(_slot, _page);
	}

	@Override
	public String getType()
	{
		return _C__35_REQUESTSHORTCUTDEL;
	}
}
