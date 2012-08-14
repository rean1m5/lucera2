package ru.catssoftware.gameserver.network.gameserverpackets;

import java.io.IOException;

public class ChangeIpAddres extends GameServerBasePacket
{
	public ChangeIpAddres(String player, String IP)
	{
		writeC(0x07);
		writeS(IP);
		writeS(player);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}