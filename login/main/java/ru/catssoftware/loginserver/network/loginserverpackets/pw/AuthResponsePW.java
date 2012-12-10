package ru.catssoftware.loginserver.network.loginserverpackets.pw;

import ru.catssoftware.loginserver.manager.GameServerManager;
import ru.catssoftware.loginserver.network.serverpackets.ServerBasePacket;

import java.io.IOException;

public class AuthResponsePW extends ServerBasePacket
{
	public AuthResponsePW(int serverId)
	{
		writeC(6);
		writeC(123);
		writeC(213);
		writeC(serverId);
		writeS(GameServerManager.getInstance().getServerNameById(serverId));
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}
