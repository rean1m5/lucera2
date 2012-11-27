package ru.catssoftware.loginserver.thread;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.loginserver.L2LoginServer;
import ru.catssoftware.loginserver.manager.GameServerManager;
import ru.catssoftware.loginserver.manager.LoginManager;
import ru.catssoftware.loginserver.model.GameServerInfo;
import ru.catssoftware.loginserver.model.SessionKey;
import ru.catssoftware.loginserver.network.gameserverpackets.*;
import ru.catssoftware.loginserver.network.loginserverpackets.*;
import ru.catssoftware.loginserver.network.serverpackets.ServerBasePacket;
import ru.catssoftware.tools.network.SubNetHost;
import ru.catssoftware.tools.security.NewCrypt;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

/**
 * @author -Wooden-
 */
public class GameServerThread extends Thread
{
	private static final Logger _log					= Logger.getLogger(GameServerThread.class.getName());
	private Socket				_connection;
	private InputStream			_in;
	private OutputStream		_out;

	private RSAPublicKey		_publicKey;
	private RSAPrivateKey		_privateKey;
	private NewCrypt			_blowfish;
	private byte[]				_blowfishKey;

	private String				_connectionIp;

	private GameServerInfo		_gsi;
	private List<SubNetHost>	_gameserverSubnets		= new FastList<SubNetHost>();

	private long				_lastIpUpdate;

	/** Authed Clients on a GameServer*/
	private Set<String>			_accountsOnGameServer	= new FastSet<String>();

	private int _playerCount = 0;

	private String				_connectionIpAddress;

	@Override
	public void run()
	{
		_connectionIpAddress = _connection.getInetAddress().getHostAddress();
		if (GameServerThread.isBannedGameserverIP(_connectionIpAddress))
		{
			_log.info("GameServerRegistration: IP Address " + _connectionIpAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			// ensure no further processing for this connection
			return;
		}

		InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(startPacket);

			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			for (;;)
			{
				lengthLo = _in.read();
				lengthHi = _in.read();
				length = lengthHi * 256 + lengthLo;

				if (lengthHi < 0 || _connection.isClosed())
				{
					_log.info("LoginServerThread: Login terminated the connection.");
					break;
				}

				byte[] data = new byte[length - 2];

				int receivedBytes = 0;
				int newBytes = 0;
				while (newBytes != -1 && receivedBytes < length - 2)
				{
					newBytes = _in.read(data, 0, length - 2);
					receivedBytes = receivedBytes + newBytes;
				}

				if (receivedBytes != length - 2)
				{
					_log.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}

				// decrypt if we have a key
				data = _blowfish.decrypt(data);
				checksumOk = NewCrypt.verifyChecksum(data);
				if (!checksumOk)
				{
					_log.warn("Incorrect packet checksum, closing connection (LS)");
					forceClose(LoginServerFail.NOT_AUTHED);
					return;
				}

				int packetType = data[0] & 0xff;
				switch (packetType)
				{
					case 00:
						onReceiveBlowfishKey(data);
						break;
					case 01:
						onGameServerAuth(data);
						break;
					case 02:
						onReceivePlayerInGame(data);
						break;
					case 03:
						onReceivePlayerLogOut(data);
						break;
					case 04:
						onReceiveChangeAccessLevel(data);
						break;
					case 05:
						onReceivePlayerAuthRequest(data);
						break;
					case 06:
						 onReceiveServerStatus(data);
						break;
					case 07:
						onReceiveChangeAllowedIP(data);
						break;
					case 8:
						onRestartRequest(data);
						break;
					default:
						_log.warn("Unknown Opcode (" + Integer.toHexString(packetType).toUpperCase() + ") from GameServer, closing connection.");
						forceClose(LoginServerFail.NOT_AUTHED);
				}
			}
		}
		catch (IOException e)
		{
			String serverName = (getServerId() != -1 ? "[" + getServerId() + "] " + GameServerManager.getInstance().getServerNameById(getServerId()) : "(" + _connectionIpAddress + ")");
			String msg = "GameServer " + serverName + ": Connection lost: " + e.getMessage();
			_log.info(msg);
		}
		finally
		{
			if (isAuthed())
			{
				_gsi.setDown();
				_log.info("Server [" + getServerId() + "] " + GameServerManager.getInstance().getServerNameById(getServerId()) + " is now set as disconnected");
			}
			L2LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			L2LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}



	private void onGameServerAuth(byte[] data) throws IOException
	{
		GameServerAuth gsa = new GameServerAuth(data);
		handleRegProcess(gsa);
		if (isAuthed())
		{
			AuthResponse ar = new AuthResponse(getGameServerInfo().getId());
			sendPacket(ar);
		}
		
	}

	private void onReceivePlayerInGame(byte[] data)
	{
		if (isAuthed())
		{
			PlayerInGame pig = new PlayerInGame(data);
			String account = pig.getAccount();
			_playerCount = pig.getOnline();
			_accountsOnGameServer.add(account);

		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void onReceivePlayerLogOut(byte[] data)
	{
		if (isAuthed())
		{
			PlayerLogout plo = new PlayerLogout(data);
			_accountsOnGameServer.remove(plo.getAccount());
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void onReceiveChangeAccessLevel(byte[] data)
	{
		if (isAuthed())
		{
			ChangeAccessLevel cal = new ChangeAccessLevel(data);
			try
			{
				LoginManager.getInstance().changeAccountLevel(cal.getAccount(), cal.getLevel());
				_log.info("Changed " + cal.getAccount() + " access level to " + cal.getLevel());
			}
			catch (Exception e)
			{
				_log.warn("Access level could not be changed. Reason: " + e.getMessage());
			}
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void onReceiveChangeAllowedIP(byte[] data)
	{
		if (isAuthed())
		{
			ChangeIpAddres cia = new ChangeIpAddres(data);
			try
			{
				LoginManager.getInstance().changeAllowedIP(cia.getAccount(), cia.getIpHost());
			}
			catch (Exception e)
			{
				_log.warn("Can't change allowedIP. Reason: " + e.getMessage());
			}
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}
	
	private void onReceiveBlowfishKey(byte[] data)
	{
		BlowFishKey bfk = new BlowFishKey(data, _privateKey);
		_blowfishKey = bfk.getKey();
		_blowfish = new NewCrypt(_blowfishKey);
	}

	private void onRestartRequest(byte[] data)
	{
		if (isAuthed())
		{
			_log.info("Login Server has been restarted! Conection IP: " + _connectionIpAddress + ".");
			System.exit(2);
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void onReceivePlayerAuthRequest(byte[] data) throws IOException
	{
		if (isAuthed())
		{
			PlayerAuthRequest par = new PlayerAuthRequest(data);
			PlayerAuthResponse authResponse;
			String accName = par.getAccount();

			SessionKey key = LoginManager.getInstance().getKeyForAccount(accName);
			String host = LoginManager.getInstance().getHostForAccount(accName);
			if (key != null && key.equals(par.getKey()))
			{
				LoginManager.getInstance().removeAuthedLoginClient(par.getAccount());
				authResponse = new PlayerAuthResponse(par.getAccount(), true, host);
			}
			else
				authResponse = new PlayerAuthResponse(par.getAccount(), false, host);
			sendPacket(authResponse);
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void onReceiveServerStatus(byte[] data)
	{
		if (isAuthed())
		{
			@SuppressWarnings("unused")
			ServerStatus ss = new ServerStatus(data, getServerId()); //will do the actions by itself
		}
		else
			forceClose(LoginServerFail.NOT_AUTHED);
	}

	private void forceClose(int reason)
	{
		LoginServerFail lsf = new LoginServerFail(reason);
		sendPacket(lsf);

		try
		{
			_connection.close();
		}
		catch (IOException e)
		{
			_log.info("GameServerThread: Failed disconnecting banned server, server already disconnected.");
		}
	}

	private void handleRegProcess(GameServerAuth gameServerAuth)
	{
		GameServerManager gameServerTable = GameServerManager.getInstance();

		int id = gameServerAuth.getDesiredID();
		byte[] hexId = gameServerAuth.getHexID();

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);
		// is there a gameserver registered with this id?
		if (gsi != null)
		{
			// does the hex id match?
			if (Arrays.equals(gsi.getHexId(), hexId))
			{
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if (gsi.isAuthed())
						forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					else
						attachGameServerInfo(gsi, gameServerAuth);
				}
			}
			else
			{
				// there is already a server registered with the desired id and different hex id
				// try to register this one with an alternative id
				if (Config.ACCEPT_NEW_GAMESERVER && gameServerAuth.acceptAlternateID())
				{
					gsi = new GameServerInfo(id, hexId, this);
					if (gameServerTable.registerWithFirstAvailableId(gsi))
					{
						attachGameServerInfo(gsi, gameServerAuth);
						gameServerTable.registerServerOnDB(gsi);
					}
					else
						forceClose(LoginServerFail.REASON_NO_FREE_ID);
				}
				else
					forceClose(LoginServerFail.REASON_WRONG_HEXID); // server id is already taken, and we cant get a new one for you
			}
		}
		else
		{
			// can we register on this id?
			if (Config.ACCEPT_NEW_GAMESERVER)
			{
				gsi = new GameServerInfo(id, hexId, this);
				if (gameServerTable.register(id, gsi))
				{
					attachGameServerInfo(gsi, gameServerAuth);
					gameServerTable.registerServerOnDB(gsi);
				}
				else
					forceClose(LoginServerFail.REASON_ID_RESERVED); // some one took this ID meanwhile
			}
			else
				forceClose(LoginServerFail.REASON_WRONG_HEXID);
		}
	}

	/**
	 * Attachs a GameServerInfo to this Thread
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * @param gsi The GameServerInfo to be attached.
	 * @param gameServerAuth The server info.
	 */
	private void attachGameServerInfo(GameServerInfo gsi, GameServerAuth gameServerAuth)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(gameServerAuth.getPort());
		setNetConfig(gameServerAuth.getNetConfig());
		gsi.setIp(_connectionIp);

		gsi.setMaxPlayers(gameServerAuth.getMaxPlayers());
		gsi.setAuthed(true);
	}

	/**
	 * @param ipAddress
	 * @return
	 */
	public static boolean isBannedGameserverIP(String ipAddress)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public GameServerThread(Socket con)
	{
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		try
		{
			_in = _connection.getInputStream();
			_out = new BufferedOutputStream(_connection.getOutputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		KeyPair pair = GameServerManager.getInstance().getKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		start();
	}

	/**
	 * @param sl
	 * @throws IOException
	 */
	public void sendPacket(ServerBasePacket sl) 
	{
		try {
			byte[] data = sl.getContent();
			NewCrypt.appendChecksum(data);
			data = _blowfish.crypt(data);
			int len = data.length + 2;
			synchronized (_out)
			{
				_out.write(len & 0xff);
				_out.write(len >> 8 & 0xff);
				_out.write(data);
				_out.flush();
			}
		} catch(IOException e) {
			_log.error("Error while sending packet "+sl.getClass().getSimpleName()+" to server "+_gsi.getId(),e);
		}
	}

	public void kickPlayer(String account)
	{
		KickPlayer kp = new KickPlayer(account);
		sendPacket(kp);
	}

	public void removeAcc(String account)
	{
		_accountsOnGameServer.remove(account);
	}

	public boolean hasAccountOnGameServer(String account)
	{
		return _accountsOnGameServer.contains(account);
	}

	public int getPlayerCount()
	{
		return _playerCount;
	}

	public void setNetConfig(String netConfig)
	{
		if (_gameserverSubnets.size() == 0)
		{
			StringTokenizer hostNets = new StringTokenizer(netConfig.trim(), ";");

			while (hostNets.hasMoreTokens())
			{
				String hostNet = hostNets.nextToken();

				StringTokenizer addresses = new StringTokenizer(hostNet.trim(), ",");

				String _host = addresses.nextToken();

				SubNetHost _subNetHost = new SubNetHost(_host);

				if (addresses.hasMoreTokens())
				{
					while (addresses.hasMoreTokens())
					{
						try
						{
							StringTokenizer netmask = new StringTokenizer(addresses.nextToken().trim(), "/");
							String _net = netmask.nextToken();
							String _mask = netmask.nextToken();

							_subNetHost.addSubNet(_net, _mask);
						}
						catch (NoSuchElementException c)
						{
							// Silence of the Lambs =)
						}
					}
				}
				else
					_subNetHost.addSubNet("0.0.0.0", "0");

				_gameserverSubnets.add(_subNetHost);
			}
		}

		updateIPs();
	}

	public void updateIPs()
	{

		_lastIpUpdate = System.currentTimeMillis();

		if (_gameserverSubnets.size() > 0)
		{
			_log.info("Updated Gameserver [" + getServerId() + "] " + GameServerManager.getInstance().getServerNameById(getServerId()) + " IP's:");

			for (SubNetHost _netConfig : _gameserverSubnets)
			{
				String _hostName = _netConfig.getHostname();
				try
				{
					String _hostAddress = InetAddress.getByName(_hostName).getHostAddress();
					_netConfig.setIp(_hostAddress);
					_log.info(!_hostName.equals(_hostAddress) ? _hostName + " (" + _hostAddress + ")" : _hostAddress);
				}
				catch (UnknownHostException e)
				{
					_log.warn("Couldn't resolve hostname \"" + _hostName + "\"");
				}
			}
		}
	}

	public String getIp(String ip)
	{
		String _host = null;

		if (Config.IP_UPDATE_TIME > 0 && (System.currentTimeMillis() > (_lastIpUpdate + Config.IP_UPDATE_TIME)))
			updateIPs();

		for (SubNetHost _netConfig : _gameserverSubnets)
		{
			if (_netConfig.isInSubnet(ip))
			{
				_host = _netConfig.getIp();
				break;
			}
		}
		if (_host == null)
			_host = ip;

		return _host;
	}

	/**
	 * @return Returns the isAuthed.
	 */
	public boolean isAuthed()
	{
		if (getGameServerInfo() == null)
			return false;

		return getGameServerInfo().isAuthed();
	}

	public void setGameServerInfo(GameServerInfo gsi)
	{
		_gsi = gsi;
	}

	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}

	/**
	 * @return Returns the connectionIpAddress.
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIpAddress;
	}

	private int getServerId()
	{
		if (getGameServerInfo() != null)
			return getGameServerInfo().getId();

		return -1;
	}
}