package ru.catssoftware.gameserver.network.gameserverpackets;

import java.io.IOException;

public class RestartLogin extends GameServerBasePacket
{
	public RestartLogin(String res)
	{
		writeC(0x8);
		writeS(res);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}