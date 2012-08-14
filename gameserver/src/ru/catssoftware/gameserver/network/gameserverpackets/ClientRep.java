package ru.catssoftware.gameserver.network.gameserverpackets;

import java.io.IOException;

import ru.catssoftware.Config;


public class ClientRep extends GameServerBasePacket
{
	public ClientRep()
	{
		writeC(0x11);
		writeD(Config.PORT_GAME);
	}
	
	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}	
}