package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.CharDeleteFail;
import ru.catssoftware.gameserver.network.serverpackets.CharDeleteSuccess;
import ru.catssoftware.gameserver.network.serverpackets.CharSelectionInfo;

public class CharacterDelete extends L2GameClientPacket
{
	private static final String	_C__0C_CHARACTERDELETE	= "[C] 0C CharacterDelete";
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
			byte answer = getClient().markToDeleteChar(_charSlot);

			switch(answer)
			{
				default:
				case -1:
					break;
				case 0:
					sendPacket(new CharDeleteSuccess());
					break;
				case 1:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
					break;
				case 2:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
					break;
			}
		}
		catch (Exception e)
		{
			_log.fatal( "Error:", e);
		}
		CharSelectionInfo cl = new CharSelectionInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1, 0);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}

	@Override
	public String getType()
	{
		return _C__0C_CHARACTERDELETE;
	}
}
