package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.network.InvalidPacketException;

public class RequestCommunityBoard extends L2GameClientPacket {

	private final static String REQUESTBOARD__C__5E = "REQUESTBOQRD__C__5E";
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	protected void readImpl() {
		_unknown = readD();
	}

	@Override
	protected void runImpl() throws InvalidPacketException
	{
		if (getClient().checkKeyProtection())
			CommunityBoard.getInstance().handleCommands(getClient(), "_bbshome");

	}
	@Override
	public String getType()
	{
		return REQUESTBOARD__C__5E;
	}

}
