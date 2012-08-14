package ru.catssoftware.gameserver.network.clientpackets;

public class RequestWriteHeroWords extends L2GameClientPacket
{
	private static final String	_C__FE_0C_REQUESTWRITEHEROWORDS	= "[C] D0:0C RequestWriteHeroWords";
	@SuppressWarnings("unused")
	private String				_heroWords;

	@Override
	protected void readImpl()
	{
		_heroWords = readS();
	}

	@Override
	protected void runImpl(){}

	@Override
	public String getType()
	{
		return _C__FE_0C_REQUESTWRITEHEROWORDS;
	}
}
