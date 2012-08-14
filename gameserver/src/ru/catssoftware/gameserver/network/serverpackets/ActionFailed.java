package ru.catssoftware.gameserver.network.serverpackets;

public final class ActionFailed extends L2GameServerPacket
{
	private static final String			_S__1f_ACTIONFAILED	= "[S] 1f ActionFailed []";
	public static final ActionFailed	STATIC_PACKET		= new ActionFailed();

	private ActionFailed()
	{
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x25);
	}

	@Override
	public String getType()
	{
		return _S__1f_ACTIONFAILED;
	}
}
