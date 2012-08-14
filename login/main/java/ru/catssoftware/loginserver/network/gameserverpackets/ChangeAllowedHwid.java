package ru.catssoftware.loginserver.network.gameserverpackets;

import ru.catssoftware.loginserver.clientpackets.ClientBasePacket;

public class ChangeAllowedHwid extends ClientBasePacket
{

	private String	_login;
	private String	_hwid;

	public ChangeAllowedHwid(byte[] decrypt)
	{
		super(decrypt);
		_login = readS();
		_hwid = readS();
	}

	public String getLogin()
	{
		return _login;
	}

	public String getHWid()
	{
		return _hwid;
	}
}
