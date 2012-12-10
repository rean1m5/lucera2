package ru.catssoftware.loginserver.network.gameserverpackets.pw;

import ru.catssoftware.loginserver.network.gameserverpackets.GameServerAuthAbstract;

public class GameServerAuthPW extends GameServerAuthAbstract
{
	public GameServerAuthPW(byte[] decrypt) {
		super(decrypt);
		_desiredId = readC();
		_acceptAlternativeId = readC() != 0;
		_hostReserved = readC() != 0;
		_port = readH();
		_maxPlayers = readD();
		_key = readS();
		int size = readD();
		_hexId = readB(size);
		_gsNetConfig1 = readS();
		_gsNetConfig2 = readS();
	}
}
