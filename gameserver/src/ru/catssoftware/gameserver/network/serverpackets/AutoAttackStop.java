package ru.catssoftware.gameserver.network.serverpackets;

public class AutoAttackStop extends L2GameServerPacket
{
	private static final String	_S__26_AUTOATTACKSTOP	= "[S] 26 AutoAttackStop [d]";
	private int					_targetObjId;

	public AutoAttackStop(int targetObjId)
	{
		_targetObjId = targetObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2c);
		writeD(_targetObjId);
	}

	@Override
	public String getType()
	{
		return _S__26_AUTOATTACKSTOP;
	}
}
