package ru.catssoftware.gameserver.network.serverpackets;

public class AutoAttackStart extends L2GameServerPacket
{
	private static final String	_S__25_AUTOATTACKSTART	= "[S] 25 AutoAttackStart [d]";
	private int					_targetObjId;

	public AutoAttackStart(int targetObjId)
	{
		_targetObjId = targetObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2b);
		writeD(_targetObjId);
	}

	@Override
	public String getType()
	{
		return _S__25_AUTOATTACKSTART;
	}
}
