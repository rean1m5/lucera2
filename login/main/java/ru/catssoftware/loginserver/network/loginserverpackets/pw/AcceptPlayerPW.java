package ru.catssoftware.loginserver.network.loginserverpackets.pw;

import ru.catssoftware.loginserver.network.serverpackets.ServerBasePacket;

import java.io.IOException;

public class AcceptPlayerPW extends ServerBasePacket
{
	public AcceptPlayerPW(String ip)
	{
		writeC(7);
		writeS(ip);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}
