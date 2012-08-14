package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.CharSelectionInfo;

public class CharacterRestore extends L2GameClientPacket
{
	private static final String	_C__62_CHARACTERRESTORE	= "[C] 62 CharacterRestore";

	private int					_charSlot;

	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}

	@Override
	protected void runImpl()
	{
		try
		{
			getClient().markRestoredChar(_charSlot);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		CharSelectionInfo cl = new CharSelectionInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}

	@Override
	public String getType()
	{
		return _C__62_CHARACTERRESTORE;
	}
}
