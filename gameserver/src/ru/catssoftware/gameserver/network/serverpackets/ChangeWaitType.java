package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2Character;

public class ChangeWaitType extends L2GameServerPacket
{
	private static final String	_S__29_CHANGEWAITTYPE	= "[S] 29 ChangeWaitType [ddddd]";
	private int					_charObjId;
	private int					_moveType;
	private int					_x, _y, _z;

	public static final int		WT_SITTING				= 0;
	public static final int		WT_STANDING				= 1;
	public static final int		WT_START_FAKEDEATH		= 2;
	public static final int		WT_STOP_FAKEDEATH		= 3;

	public ChangeWaitType(L2Character character, int newMoveType)
	{
		_charObjId = character.getObjectId();
		_moveType = newMoveType;

		_x = character.getX();
		_y = character.getY();
		_z = character.getZ();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2f);
		writeD(_charObjId);
		writeD(_moveType);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	@Override
	public String getType()
	{
		return _S__29_CHANGEWAITTYPE;
	}
}
