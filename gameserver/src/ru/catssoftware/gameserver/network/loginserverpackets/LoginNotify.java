package ru.catssoftware.gameserver.network.loginserverpackets;

import ru.catssoftware.gameserver.network.IOFloodManager;

public class LoginNotify extends LoginServerBasePacket {
	
	private String _IP; 
	public LoginNotify(byte[] decrypt) {
		super(decrypt);
		_IP = readS();
		IOFloodManager.getInstance().addIp(_IP);
	}

}
