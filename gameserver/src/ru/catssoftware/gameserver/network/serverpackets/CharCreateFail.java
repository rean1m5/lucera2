package ru.catssoftware.gameserver.network.serverpackets;

public class CharCreateFail extends L2GameServerPacket
{
	private static final String	_S__10_CHARCREATEFAIL		= "[S] 10 CharCreateFail [d]";

	public static final int		REASON_CREATION_FAILED		= 0x00;
	public static final int		REASON_TOO_MANY_CHARACTERS	= 0x01;
	public static final int		REASON_NAME_ALREADY_EXISTS	= 0x02;
	public static final int		REASON_16_ENG_CHARS			= 0x03;

	private int					_error;

	public CharCreateFail(int errorCode)
	{
		_error = errorCode;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x1a);
		writeD(_error);
	}

	@Override
	public String getType()
	{
		return _S__10_CHARCREATEFAIL;
	}
}
