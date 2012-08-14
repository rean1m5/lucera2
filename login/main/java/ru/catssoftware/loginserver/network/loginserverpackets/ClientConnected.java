package ru.catssoftware.loginserver.network.loginserverpackets;

import java.io.IOException;

import ru.catssoftware.loginserver.L2LoginClient;
import ru.catssoftware.loginserver.network.serverpackets.ServerBasePacket;


public class ClientConnected extends ServerBasePacket {

	public ClientConnected(L2LoginClient cl) {
		writeC(0x05);
		writeS(cl.getIp());
	}
	@Override
	public byte[] getContent() throws IOException {
		return getBytes();
	}

}
