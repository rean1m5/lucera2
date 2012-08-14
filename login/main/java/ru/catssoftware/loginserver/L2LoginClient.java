package ru.catssoftware.loginserver;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.interfaces.RSAPrivateKey;



import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.loginserver.crypt.LoginCrypt;
import ru.catssoftware.loginserver.manager.LoginManager;
import ru.catssoftware.loginserver.mmocore.ISocket;
import ru.catssoftware.loginserver.mmocore.MMOConnection;
import ru.catssoftware.loginserver.mmocore.SelectorThread;
import ru.catssoftware.loginserver.model.Account;
import ru.catssoftware.loginserver.model.SessionKey;
import ru.catssoftware.loginserver.network.serverpackets.LoginFail;
import ru.catssoftware.loginserver.network.serverpackets.LoginFailReason;
import ru.catssoftware.loginserver.network.serverpackets.PlayFail;
import ru.catssoftware.loginserver.network.serverpackets.PlayFailReason;
import ru.catssoftware.tools.math.ScrambledKeyPair;
import ru.catssoftware.tools.random.Rnd;

/**
 * Represents a client connected into the LoginServer
 *
 * @author  KenM
 */
public class L2LoginClient extends MMOConnection<L2LoginClient>
{
	private static final Logger _log	= Logger.getLogger(L2LoginClient.class);

	public static enum LoginClientState
	{
		CONNECTED,
		AUTHED_GG,
		AUTHED_CARD,
		AUTHED_LOGIN;
	}

	private LoginClientState _state = LoginClientState.CONNECTED;

	// Crypt
	private LoginCrypt _loginCrypt;
	private final ScrambledKeyPair _scrambledPair;
	private final byte[] _blowfishKey;

	private String _account = null;
	private int _accessLevel;
	private int _lastServerId;
	private SessionKey _sessionKey;
	private final int _sessionId = Rnd.nextInt(Integer.MAX_VALUE);
	private boolean _joinedGS;
	private final String _ip;
	public boolean checkOK = false;

	public L2LoginClient(SelectorThread<L2LoginClient> selectorThread, ISocket socket, SelectionKey key)
	{
		super(selectorThread, socket, key);

		_ip = getSocket().getInetAddress().getHostAddress();

		_scrambledPair = LoginManager.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginManager.getInstance().getBlowfishKey();
		if(Config.DDOS_PROTECTION_ENABLED) {
			L2LoginPacketHandler.getInstance().addClient(this);
		} else
			checkOK = true;
	}

	private LoginCrypt getLoginCrypt()
	{
		if (_loginCrypt == null)
		{
			_loginCrypt = new LoginCrypt();
			_loginCrypt.setKey(_blowfishKey);
		}

		return _loginCrypt;
	}

	public String getIp()
	{
		return _ip;
	}

	/**
	 * @see com.l2jserver.mmocore.interfaces.MMOClient#decrypt(java.nio.ByteBuffer, int)
	 */
	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		boolean ret = false;
		try
		{
			ret = getLoginCrypt().decrypt(buf.array(), buf.position(), size);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			closeNow();
			return false;
		}

		if (!ret)
		{
			byte[] dump = new byte[size];
			System.arraycopy(buf.array(), buf.position(), dump, 0, size);
			_log.warn("Wrong checksum from client: " + toString());
			closeNow();
		}

		return ret;
	}

	/**
	 * @see com.l2jserver.mmocore.interfaces.MMOClient#encrypt(java.nio.ByteBuffer, int)
	 */
	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		final int offset = buf.position();
		try
		{
			size = getLoginCrypt().encrypt(buf.array(), offset, size);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}

		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState()
	{
		return _state;
	}

	public void setState(LoginClientState state)
	{
		_state = state;
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}

	public byte[] getScrambledModulus()
	{
		return _scrambledPair.getScrambledModulus();
	}

	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair.getPair().getPrivate();
	}

	public String getAccount()
	{
		return _account;
	}

	public byte _CardNo;
	public Account _accInfo; 
	public void setAccount(String account)
	{
		_account = account;
		if(account!=null)
			_accInfo = LoginManager.getInstance().getAccount(_account);
	}

	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}

	public int getAccessLevel()
	{
		return _accessLevel;
	}

	public void setLastServerId(int lastServerId)
	{
		_lastServerId = lastServerId;
	}

	public int getLastServerId()
	{
		return _lastServerId;
	}

	public int getSessionId()
	{
		return _sessionId;
	}

	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}

	public boolean hasJoinedGS()
	{
		return _joinedGS;
	}

	public void setJoinedGS(boolean val)
	{
		_joinedGS = val;
	}

	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}


	public void close(LoginFailReason reason)
	{
		close(new LoginFail(reason));
	}

	public void close(PlayFailReason reason)
	{
		close(new PlayFail(reason));
	}

	public InetAddress getInetAddress()
	{
		return getSocket().getInetAddress();
	}

	@Override
	public void onDisconnection()
	{
		// If player was not on GS, don't forget to remove it from authed login on LS
		if (getState() == LoginClientState.AUTHED_LOGIN && !hasJoinedGS())
			LoginManager.getInstance().removeAuthedLoginClient(getAccount());
	}

	@Override
	protected void onForcedDisconnection(){}

	@Override
	public String toString()
	{
		InetAddress address = getSocket().getInetAddress();
		if (getState() == LoginClientState.AUTHED_LOGIN)
			return "[" + getAccount() + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
		else
			return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
	}
}