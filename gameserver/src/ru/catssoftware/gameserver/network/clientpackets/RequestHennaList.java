package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.HennaEquipList;

public class RequestHennaList extends L2GameClientPacket
{
	private static final String	_C__BA_RequestHennaList	= "[C] ba RequestHennaList";

	@SuppressWarnings("unused")
	private int					_unknown;

	@Override
	protected void readImpl()
	{
		_unknown = readD(); // ??
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		activeChar.sendPacket(new HennaEquipList(activeChar));
	}

	@Override
	public String getType()
	{
		return _C__BA_RequestHennaList;
	}
}
