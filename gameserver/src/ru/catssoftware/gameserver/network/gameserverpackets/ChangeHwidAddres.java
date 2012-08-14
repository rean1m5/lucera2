package ru.catssoftware.gameserver.network.gameserverpackets;

import java.io.IOException;

public class ChangeHwidAddres extends GameServerBasePacket
{
	public ChangeHwidAddres(String login, String hwid)
	{
		writeC(0x9);
		writeS(login);
		writeS(hwid);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}