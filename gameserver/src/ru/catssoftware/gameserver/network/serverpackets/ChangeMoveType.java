package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2Character;

public class ChangeMoveType extends L2GameServerPacket
{
	private static final String	_S__28_CHANGEMOVETYPE	= "[S] 28 ChangeMoveType [ddd]";
	public static final int		WALK					= 0;
	public static final int		RUN						= 1;

	private int					_chaObjId;
	private boolean				_running;

	public ChangeMoveType(L2Character character)
	{
		_chaObjId = character.getObjectId();
		_running = character.isRunning();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2e);
		writeD(_chaObjId);
		writeD(_running ? RUN : WALK);
		writeD(0x00);
	}

	@Override
	public String getType()
	{
		return _S__28_CHANGEMOVETYPE;
	}
}
