package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.LoginServerThread;
import ru.catssoftware.gameserver.LoginServerThread.SessionKey;
import ru.catssoftware.gameserver.network.L2GameClient;

public class AuthLogin extends L2GameClientPacket
{
	private static final String	_C__08_AUTHLOGIN	= "[C] 08 AuthLogin";

	private String				_loginName;
	private int					_playKey1, _playKey2, _loginKey1, _loginKey2;

	@Override
	protected void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
	}

	@Override
	protected void runImpl()
	{
		if (!getClient().isProtocolOk())
			return;

		SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);

		L2GameClient client = getClient();

		if (client.getAccountName() == null)
		{
			client.setAccountName(_loginName);
			LoginServerThread.getInstance().addWaitingClientAndSendRequest(_loginName, client, key);
		}
		client = null;
	}

	@Override
	public String getType()
	{
		return _C__08_AUTHLOGIN;
	}
}
