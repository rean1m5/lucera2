package ru.catssoftware.gameserver;

import javolution.util.FastMap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.L2GameClient.GameClientState;
import ru.catssoftware.gameserver.network.gameserverpackets.*;
import ru.catssoftware.gameserver.network.loginserverpackets.*;
import ru.catssoftware.gameserver.network.serverpackets.CharSelectionInfo;
import ru.catssoftware.gameserver.network.serverpackets.LoginFail;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.tools.security.BlowfishEngine;
import ru.catssoftware.tools.security.NewCrypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

public class LoginServerThread extends Thread
{
	protected static Logger				_log		= Logger.getLogger(LoginServerThread.class.getName());
	protected static Logger				_debug		= Logger.getLogger("logdebug");

	private static LoginServerThread		_instance;
	private static final int				 []REVISION	= {0x0102,0x104};
	private RSAPublicKey					_publicKey;
	private String							_hostname;
	private int								_port;
	private int								_gamePort;
	private Socket 							_connection;
	private BufferedInputStream				_in;
	private BufferedOutputStream			_out;
	private NewCrypt						_blowfish;
	private byte[]							_blowfishKey;
	private byte[]							_hexID;
	private boolean							_acceptAlternate;
	private int								_requestID;
	private int								_serverID;
	private int								_maxPlayer;
	private final Map<String, WaitingClient> _waitingClients;
	private final Map<String, L2GameClient> _accountsInGameServer;
	private int								_status;
	private String							_serverName;
	private String							_gameExternalHost;													// External host for old login server
	private String							_gameInternalHost;													// Internal host for old login server

	public LoginServerThread()
	{
		super("LoginServerThread");
		_port = Config.GAME_SERVER_LOGIN_PORT;
		_gamePort = Config.PORT_GAME;
		_hostname = Config.GAME_SERVER_LOGIN_HOST;
		_hexID = Config.HEX_ID;
		if (_hexID == null)
		{
			_requestID = Config.REQUEST_ID;
			_hexID = generateHex(16);
		}
		else
			_requestID = Config.SERVER_ID;

		_acceptAlternate = Config.ACCEPT_ALTERNATE_ID;
		_gameExternalHost = Config.EXTERNAL_HOSTNAME;
		_gameInternalHost = Config.INTERNAL_HOSTNAME;
		_waitingClients = new FastMap<String, WaitingClient>().setShared(true);
		_accountsInGameServer = new FastMap<String, L2GameClient>().setShared(true);
		_maxPlayer = Config.MAXIMUM_ONLINE_USERS;

		if (Config.SUBNETWORKS != null && Config.SUBNETWORKS.length() > 0)
		{
			_gameExternalHost = Config.SUBNETWORKS;
			_gameInternalHost = "";
		}

	}

	public static LoginServerThread getInstance()
	{
		if (_instance == null)
			_instance = new LoginServerThread();
		return _instance;
	}

	@Override
	public void run()
	{
		while (true)
		{
			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			try
			{
				// Connection
				_log.info("Connecting to login on " + _hostname + ":" + _port);
				
				_connection = new Socket(_hostname, _port);
				_in = new BufferedInputStream(_connection.getInputStream());
				_out = new BufferedOutputStream(_connection.getOutputStream());
				// init Blowfish
				_blowfishKey = generateHex(40);
				_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
				while (true)
				{
					lengthLo = _in.read();
					lengthHi = _in.read();
					length = lengthHi * 256 + lengthLo;

					if (lengthHi < 0)
					{
						_log.debug("LoginServerThread: Login terminated the connection.");
						break;
					}

					byte[] incoming = new byte[length];
					incoming[0] = (byte) lengthLo;
					incoming[1] = (byte) lengthHi;

					int receivedBytes = 0;
					int newBytes = 0;
					while (newBytes != -1 && receivedBytes < length - 2)
					{
						newBytes = _in.read(incoming, 2, length - 2);
						receivedBytes = receivedBytes + newBytes;
					}

					if (receivedBytes != length - 2)
					{
						_log.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
						break;
					}

					byte[] decrypt = new byte[length - 2];
					System.arraycopy(incoming, 2, decrypt, 0, decrypt.length);
					decrypt = _blowfish.decrypt(decrypt);
					checksumOk = NewCrypt.verifyChecksum(decrypt);

					if (!checksumOk)
					{
						_log.warn("Incorrect packet checksum, ignoring packet (LS)");
						break;
					}

					int packetType = decrypt[0] & 0xff;
					switch (packetType)
					{
						case 00:
							InitLS init = new InitLS(decrypt);
							boolean isOk = false;
							for(int rev : REVISION)
								if (init.getRevision() == rev)
								{
									
									isOk = true;
									break;
								}
							if(!isOk) {
								_log.warn("/!\\ Revision mismatch between LS and GS /!\\");
								break;
							}
							try {
								KeyFactory kfac = KeyFactory.getInstance("RSA");
								BigInteger modulus = new BigInteger(init.getRSAKey());
								RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4);
								_publicKey = (RSAPublicKey) kfac.generatePublic(kspec1);
							}
							catch (GeneralSecurityException e)
							{
								_log.warn("Troubles while init the public key send by login");
								break;
							}
							// send the blowfish key through the rsa encryption
							BlowfishEngine.prepareBlocks();
							BlowFishKey bfk = new BlowFishKey(_blowfishKey, _publicKey);
							sendPacket(bfk);
							// now, only accept paket with the new encryption
							_blowfish = new NewCrypt(_blowfishKey);
							sendPacket(new AuthRequest(_requestID, _acceptAlternate, _hexID, _gameExternalHost, _gameInternalHost, _gamePort,_maxPlayer));
							break;
						case 01:
							LoginServerFail lsf = new LoginServerFail(decrypt);
							_log.info("Damn! Registeration Failed: " + lsf.getReasonString());
							break;
						case 02:
							AuthResponse aresp = new AuthResponse(decrypt);
							_serverID = aresp.getServerId();
							_serverName = aresp.getServerName();
							Config.saveHexid(_serverID, hexToString(_hexID));
							_log.info("Registered on login as Server " + _serverID + " : " + _serverName);
							ServerStatus st = new ServerStatus();
							if (Config.SERVER_LIST_BRACKET)
								st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.ON);
							else
								st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.OFF);
							if (Config.SERVER_LIST_CLOCK)
								st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.ON);
							else
								st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.OFF);
							if (Config.SERVER_LIST_TESTSERVER)
								st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.ON);
							else
								st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.OFF);
							if (Config.SERVER_GMONLY)
								st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
							else
								st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
							if(Config.ENABLE_DDOS_PROTECTION)
								st.addAttribute(ServerStatus.SERVER_DDOS_ENABLED, ServerStatus.ON);
							else
								st.addAttribute(ServerStatus.SERVER_DDOS_ENABLED, ServerStatus.OFF);
							sendPacket(st);
							break;
						case 03:
							PlayerAuthResponse par = new PlayerAuthResponse(decrypt);
							WaitingClient waiting = _waitingClients.remove(par.getAccount());

							if (waiting != null)
							{
								final L2GameClient client = waiting.gameClient;
								if (par.isAuthed())
								{
									sendPacket(new PlayerInGame(par.getAccount(), (int) (L2World.getInstance().getAllPlayersCount() * Config.ONLINE_PLAYERS_MULTIPLIER)));
									client.setState(GameClientState.AUTHED);
									client.setSessionId(waiting.session);
									client.setHostAddress(par.getHost());
									client.sendPacket(new CharSelectionInfo(par.getAccount(), client.getSessionId().playOkID1));
									_accountsInGameServer.put(client.getAccountName(), client);
								}
								else
								{
									client.sendPacket(new LoginFail(1));
									client.closeNow();
								}
							}
							break;
						case 04:
							doKickPlayer(new KickPlayer(decrypt).getAccount());
							break;
						case 05:
							new LoginNotify(decrypt);
							break;	
					}
				}
			}
			catch (ConnectException e)
			{
				_log.info(e);
			}
			catch (IOException e)
			{
				_log.warn("", e);
			}
			catch (RuntimeException e)
			{
				_log.warn("", e);
			}
			finally
			{
				close();
				_log.info("Disconnected from login, trying to reconnect!");
			}
			
			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException e)
			{
				_log.warn("", e);
			}
		}
	}
	
	protected final void close()
	{
		try
		{
			IOUtils.closeQuietly(_in);
			IOUtils.closeQuietly(_out);
			if (_connection != null)
				_connection.close();
		}
		catch (IOException e)
		{
		}
		
		_in = null;
		_out = null;
		_connection = null;
	}

	public void addWaitingClientAndSendRequest(String acc, L2GameClient client, SessionKey key)
	{
		_waitingClients.put(acc, new WaitingClient(client, key));
		sendPacketQuietly(new PlayerAuthRequest(acc, key));
	}

	public void sendLogout(String account)
	{
		if (account == null || account.isEmpty())
			return;
		
		_waitingClients.remove(account);
		_accountsInGameServer.remove(account);
		
		sendPacketQuietly(new PlayerLogout(account));
	}


	public void sendAccessLevel(String account, int level)
	{
		ChangeAccessLevel cal = new ChangeAccessLevel(account, level);
		try
		{
			sendPacket(cal);
		}
		catch (IOException e)
		{
		}
	}

	public void sendAllowedIP(String account, String host)
	{
		ChangeIpAddres cia = new ChangeIpAddres(account, host);
		try
		{
			sendPacket(cia);
		}
		catch (IOException e)
		{
		}
	}

	public void sendAllowedHwid(String account, String hwid)
	{
		ChangeHwidAddres cha = new ChangeHwidAddres(account, hwid);
		try
		{
			sendPacket(cha);
		}
		catch (IOException e)
		{
		}
	}
	
	public void sendRestartLogin(String mode)
	{
		_log.info("TaskManager: Restarting login server.....");
		RestartLogin res = new RestartLogin(mode);
		try
		{
			sendPacket(res);
		}
		catch (IOException e)
		{
		}
	}

	private String hexToString(byte[] hex)
	{
		return new BigInteger(hex).toString(16);
	}

	public void doKickPlayer(String account)
	{
		if (_accountsInGameServer.get(account) != null)
			_accountsInGameServer.get(account).closeNow();
		else
		{
			try
			{
				sendPacket(new PlayerLogout(account));
			}
			catch (IOException e)
			{
				_log.warn("Error while sending logout packet to login", e);
			}
		}
	}

	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	public final void sendPacket(GameServerBasePacket sl) throws IOException
	{
		byte[] data = sl.getContent();
		NewCrypt.appendChecksum(data);
		data = _blowfish.crypt(data);
		int len = data.length + 2;
		synchronized (_out) // avoids tow threads writing in the mean time
		{
				_out.write(len & 0xff);
				_out.write(len >> 8 & 0xff);
				_out.write(data);
				_out.flush();	
		}
	}
	
	protected final void sendPacketQuietly(GameServerBasePacket sl)
	{
		try
		{
			sendPacket(sl);
		}
		catch (IOException e)
		{
			_log.info("Error on Send Packet [LST]", e);
		}
	}

	public void setMaxPlayer(int maxPlayer)
	{
		sendServerStatus(ServerStatus.MAX_PLAYERS, maxPlayer);
		_maxPlayer = maxPlayer;
	}

	public int getMaxPlayer()
	{
		return _maxPlayer;
	}

	/**
	 * @param id
	 * @param value
	 */
	public void sendServerStatus(int id, int value)
	{
		ServerStatus ss = new ServerStatus();
		ss.addAttribute(id, value);
		try
		{
			sendPacket(ss);
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * @return
	 */
	public String getStatusString()
	{
		return ServerStatus.STATUS_STRING[_status];
	}

	/**
	 * @return
	 */
	public boolean isClockShown()
	{
		return Config.SERVER_LIST_CLOCK;
	}

	/**
	 * @return
	 */
	public boolean isBracketShown()
	{
		return Config.SERVER_LIST_BRACKET;
	}

	/**
	 * @return Returns the serverName.
	 */
	public String getServerName()
	{
		return _serverName;
	}

	public void setServerStatus(int status)
	{
		switch (status)
		{
			case ServerStatus.STATUS_AUTO:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
				_status = status;
				break;
			case ServerStatus.STATUS_DOWN:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_DOWN);
				_status = status;
				break;
			case ServerStatus.STATUS_FULL:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_FULL);
				_status = status;
				break;
			case ServerStatus.STATUS_GM_ONLY:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
				_status = status;
				break;
			case ServerStatus.STATUS_GOOD:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GOOD);
				_status = status;
				break;
			case ServerStatus.STATUS_NORMAL:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_NORMAL);
				_status = status;
				break;
			default:
				throw new IllegalArgumentException("Status does not exists:" + status);
		}
	}
	public static class SessionKey
	{
		public int	playOkID1;
		public int	playOkID2;
		public int	loginOkID1;
		public int	loginOkID2;

		public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2)
		{
			playOkID1 = playOK1;
			playOkID2 = playOK2;
			loginOkID1 = loginOK1;
			loginOkID2 = loginOK2;
		}

		@Override
		public String toString()
		{
			return "PlayOk: " + playOkID1 + " " + playOkID2 + " LoginOk:" + loginOkID1 + " " + loginOkID2;
		}
	}

	private class WaitingClient
	{
		public final L2GameClient gameClient;
		public final SessionKey session;

		public WaitingClient(L2GameClient client, SessionKey key)
		{
			gameClient = client;
			session = key;
		}
	}

}