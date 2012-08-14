package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class ChairSit extends L2GameServerPacket
{
	private static final String	_S__ED_CHAIRSIT	= "[S] ed ChairSit [dd]";

	private L2PcInstance		_activeChar;
	private int					_staticObjectId;

	public ChairSit(L2PcInstance player, int staticObjectId)
	{
		_activeChar = player;
		_staticObjectId = staticObjectId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe1);
		writeD(_activeChar.getObjectId());
		writeD(_staticObjectId);
	}

	@Override
	public String getType()
	{
		return _S__ED_CHAIRSIT;
	}
}
