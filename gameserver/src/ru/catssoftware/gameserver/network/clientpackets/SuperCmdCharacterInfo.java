package ru.catssoftware.gameserver.network.clientpackets;

public final class SuperCmdCharacterInfo extends L2GameClientPacket
{
	private static final String	_C__39_00_SUPERCMDCHARACTERINFO	= "[C] 39:00 SuperCmdCharacterInfo";
	@SuppressWarnings("unused")
	private String				_characterName;

	@Override
	protected void readImpl()
	{
		_characterName = readS();
	}

	@Override
	protected void runImpl()
	{
	}

	@Override
	public String getType()
	{
		return _C__39_00_SUPERCMDCHARACTERINFO;
	}
}
