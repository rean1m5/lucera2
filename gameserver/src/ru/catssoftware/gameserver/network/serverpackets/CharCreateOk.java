package ru.catssoftware.gameserver.network.serverpackets;

public class CharCreateOk extends L2GameServerPacket
{
	private static final String	_S__0F_CHARCREATEOK	= "[S] 0f CharCreateOk [d]";

	@Override
	protected final void writeImpl()
	{
		writeC(0x19);
		writeD(0x01);
	}

	@Override
	public String getType()
	{
		return _S__0F_CHARCREATEOK;
	}
}
