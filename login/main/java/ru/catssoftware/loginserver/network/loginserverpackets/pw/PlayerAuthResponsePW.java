package ru.catssoftware.loginserver.network.loginserverpackets.pw;

import ru.catssoftware.loginserver.network.serverpackets.ServerBasePacket;

import java.io.IOException;

public class PlayerAuthResponsePW extends ServerBasePacket
{
	public PlayerAuthResponsePW(String account, boolean response, String hwid, boolean email)
	{
		writeC(0x03);
		writeS(account);
		writeC(response ? 1 : 0);
		writeS("none");
		writeC(email ? 1 : 0);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}
