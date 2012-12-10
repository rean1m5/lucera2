package ru.catssoftware.loginserver.manager;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.loginserver.L2LoginClient;
import ru.catssoftware.loginserver.model.Account;
import ru.catssoftware.loginserver.model.GameServerInfo;
import ru.catssoftware.loginserver.model.SessionKey;
import ru.catssoftware.loginserver.network.gameserverpackets.ServerStatus;
import ru.catssoftware.loginserver.services.exception.AccountBannedException;
import ru.catssoftware.loginserver.services.exception.AccountModificationException;
import ru.catssoftware.loginserver.services.exception.AccountWrongPasswordException;
import ru.catssoftware.loginserver.thread.GameServerThread;
import ru.catssoftware.tools.codec.Base64;
import ru.catssoftware.tools.math.ScrambledKeyPair;
import ru.catssoftware.tools.random.Rnd;

import javax.crypto.Cipher;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginManager
{
	private static final Logger 					_log				= Logger.getLogger(LoginManager.class);
	private static final Logger 					_logLogin			= Logger.getLogger("login");
	private static final Logger 					_logLoginFailed		= Logger.getLogger("fail");

	private static LoginManager						_instance;

	/** Authed Clients on LoginServer*/
	protected Map<String, L2LoginClient>			_loginServerClients	= new FastMap<String, L2LoginClient>();

	/** Keep trace of login attempt for an inetadress*/

	/** Clients that are on the LS but arent assocated with a account yet*/
	protected Set<L2LoginClient>					_clients			= new FastSet<L2LoginClient>();

	private ScrambledKeyPair[]						_keyPairs;

	protected byte[][]								_blowfishKeys;

	private static final int						BLOWFISH_KEYS		= 20;
	private static Map<String,Integer>				_registredAccounts = new FastMap<String, Integer>();
	
	// private final Map<String, Account>				_accountCache = new FastMap<String, Account>();

	private  Connection con;
	public static enum AuthLoginResult
	{
		INVALID_PASSWORD, ACCOUNT_BANNED, ALREADY_ON_LS, ALREADY_ON_GS, AUTH_SUCCESS, SYSTEM_ERROR, TCARD_REQUIRED
	}

	public static void load()
	{
		if (_instance == null)
			_instance = new LoginManager();
		else
			throw new IllegalStateException("LoginManager can only be loaded a single time.");
	}

	private class ConnectionCheck extends Thread {
		public void run() {
			for(;;) {
				try {
					PreparedStatement stm = con.prepareStatement("SELECT 1");
					stm.executeQuery().close();
					stm.close();
				} catch(SQLException e) { 
					return;
				}
				try { Thread.sleep(120000); } catch(InterruptedException ie) { return; }
			}
		}
	}
	private LoginManager()
	{
		try
		{
			_log.info("LoginManager initiating");

			
			 con = L2DatabaseFactory.getInstance().getConnection();
			 new ConnectionCheck().start();
			_keyPairs = new ScrambledKeyPair[10];


			KeyPairGenerator keygen = null;

			try
			{
				keygen = KeyPairGenerator.getInstance("RSA");
				RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
				keygen.initialize(spec);
			}
			catch (GeneralSecurityException e)
			{
				_log.fatal("Error in RSA setup:" + e);
				_log.info("Server shutting down now");
				System.exit(1);
			}

			//generate the initial set of keys
			for (int i = 0; i < 10; i++)
				_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());

			_log.info("Cached 10 KeyPairs for RSA communication");

			testCipher((RSAPrivateKey) _keyPairs[0].getPair().getPrivate());

			// Store keys for blowfish communication
			generateBlowFishKeys();
		}
		catch (Exception e)
		{
			_log.fatal("FATAL: Failed initializing LoginManager. Reason: " + e.getMessage(), e);
			System.exit(1);
		}

	}

	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for (int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for (int j = 0; j < _blowfishKeys[i].length; j++)
				_blowfishKeys[i][j] = (byte) (Rnd.nextInt(255) + 1);
		}
		_log.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication");
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}

	public static LoginManager getInstance()
	{
		return _instance;
	}

	public SessionKey assignSessionKeyToLogin(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE));
		_loginServerClients.put(account, client);
		return key;
	}

	public void removeAuthedLoginClient(String account)
	{
		_loginServerClients.remove(account);
	}

	public boolean isAccountInLoginServer(String account)
	{
		return _loginServerClients.containsKey(account);
	}

	public synchronized SessionKey assignSessionKeyToClient(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE), Rnd.nextInt(Integer.MAX_VALUE));
		try {
			_loginServerClients.put(account, client);
		} catch(NullPointerException e) {
			if(_loginServerClients==null) {
				_loginServerClients = new FastMap<String, L2LoginClient>();
				_loginServerClients.put(account, client);
			}
			if(account==null)
				return key;
			if(client==null)
				return null;
		}
		return key;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerManager.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
				return gsi;
		}
		return null;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerManager.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
				return true;
		}
		return false;
	}

	public AuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client) throws AccountBannedException, AccountWrongPasswordException
	{
		AuthLoginResult ret = AuthLoginResult.INVALID_PASSWORD;

		try
		{
			
			String allowedIP = getAllowedIP(account);
			// check ip
			if (!allowedIP.contains("*") && client.getIp().compareTo(allowedIP) != 0)
			{
				
					Config.debug(account+ " IP mismatch: Await "+allowedIP+", got "+client.getIp());
				ret = AuthLoginResult.SYSTEM_ERROR;
				return ret;
			}

			// check auth
			if (this.loginValid(account, password, client))
			{
				// login was successful, verify presence on Gameservers
				ret = AuthLoginResult.ALREADY_ON_GS;

				if (!isAccountInAnyGameServer(account))
				{
					// account isnt on any GS, verify LS itself
					ret = AuthLoginResult.ALREADY_ON_LS;
					Config.debug(account+ " Already on GS");
					
					// dont allow 2 simultaneous login
					synchronized (_loginServerClients)
					{
						if (!_loginServerClients.containsKey(account))
						{
							_loginServerClients.put(account, client);
							ret = AuthLoginResult.AUTH_SUCCESS;
						}
					}
					Account acc = getAccount(account);
					
					// keep access level in the L2LoginClient
					client.setAccessLevel(acc.getAccessLevel());
					// keep last server choice
					client.setLastServerId(acc.getLastServerId());
				} else {
					
				}
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			_log.error("could not check password:" + e);
			ret = AuthLoginResult.SYSTEM_ERROR;
		}
		catch (UnsupportedEncodingException e)
		{
			_log.error("could not check password:" + e);
			ret = AuthLoginResult.SYSTEM_ERROR;
		}
		catch (AccountModificationException e)
		{
			_log.warn("could not check password:" + e);
			ret = AuthLoginResult.SYSTEM_ERROR;
		}
		return ret;  
	}


	public L2LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}

	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client = _loginServerClients.get(account);
		if (client != null)
			return client.getSessionKey();

		return null;
	}

	public String getHostForAccount(String account)
	{
		L2LoginClient client = getAuthedClient(account);

		return client != null ? client.getIp() : "-1";
	}
	

	/**
	 * Login is possible if number of player < max player for this GS
	 * and the status of the GS != STATUS_GM_ONLY
	 * All those conditions are not applied if the player is a GM
	 * @return
	 */
	public boolean isLoginPossible(int access, int serverId)
	{
		GameServerInfo gsi = GameServerManager.getInstance().getRegisteredGameServerById(serverId);
		if (gsi != null && gsi.isAuthed())
			return ((gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY) || access >= Config.GM_MIN);

		return false;
	}

	/**
	 *
	 * @param ServerID
	 * @return online player count for a server
	 */
	public int getOnlinePlayerCount(int serverId)
	{
		GameServerInfo gsi = GameServerManager.getInstance().getRegisteredGameServerById(serverId);
		if (gsi != null && gsi.isAuthed())
			return gsi.getCurrentPlayerCount();

		return 0;
	}

	/***
	 *
	 * @param ServerID
	 * @return max allowed online player for a server
	 */
	public int getMaxAllowedOnlinePlayers(int id)
	{
		GameServerInfo gsi = GameServerManager.getInstance().getRegisteredGameServerById(id);
		if (gsi != null)
			return gsi.getMaxPlayers();

		return 0;
	}

	/**
	*
	* @param user
	* @param lastServerId
	*/
	public void setAccountLastServerId(String account, int lastServerId)
	{
		Account acc = getAccount(account);
		if(acc!=null) {
			acc.setLastServerId(lastServerId);
			addOrUpdateAccount(acc);
		}
	}

	/**
	 *
	 * @param user
	 * @return true if a user is a GM account
	 */
	public boolean isGM(Account acc)
	{
		if (acc != null)
			return acc.getAccessLevel() >= Config.GM_MIN;
		else
			return false;
	}

	/**
	 *
	 * @param user
	 * @return account if exist, null if not
	 */
	public Account getAccount(String user)
	{
		Account result = null;
		
		synchronized (con) {
			try {
//				result = _accountCache.get(user);
//				if(result!=null)
//					return result;
				
				PreparedStatement stm = con.prepareStatement("select * from accounts where login=?");
				stm.setString(1, user);
				ResultSet rs = stm.executeQuery();
				if(rs.next()) {
					result = new Account(user, rs.getString("password"),rs.getLong("lastactive"),rs.getInt("accessLevel"),rs.getInt("lastServerId"),rs.getString("lastIP"));
/*					if(_accountCache.size()>500) {
						for(Account acc: _accountCache.values()) {
							if(System.currentTimeMillis()/1000- acc.getLastactive() > 6000) {
								_accountCache.remove(acc.getLogin());
								break;
							}
						}
					}
					_accountCache.put(user, result); */;
				}
				rs.close();
				stm.close();
			} catch(SQLException e) {
				_log.warn("LoginManager: Unable to retrive account",e);
			}
			
		}
		return result;
	}

	/**
	 * <p>This method returns one of the 10 {@link ScrambledKeyPair}.</p>
	 * <p>One of them the re-newed asynchronously using a {@link UpdateKeyPairTask} if necessary.</p>
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}

	/**
	 * user name is not case sensitive any more
	 * @param user
	 * @param password
	 * @param address
	 * @return true if all operations succeed
	 * @throws NoSuchAlgorithmException if SHA is not supported
	 * @throws UnsupportedEncodingException if UTF-8 is not supported
	 * @throws AccountModificationException  if we were unable to modify the account
	 * @throws AccountBannedException  if account is banned
	 * @throws AccountWrongPasswordException if the password is wrong
	 */
	public boolean loginValid(String user, String password, L2LoginClient client) throws NoSuchAlgorithmException, UnsupportedEncodingException, AccountModificationException, AccountBannedException, AccountWrongPasswordException
	{
		InetAddress address = client.getInetAddress();
		// player disconnected meanwhile
		if (address == null)
			return false;

		return loginValid(user, password, address);
	}

	/**
	 * user name is not case sensitive any more
	 * @param user
	 * @param password
	 * @param address
	 * @return true if all operations succeed
	 * @throws NoSuchAlgorithmException if SHA is not supported
	 * @throws UnsupportedEncodingException if UTF-8 is not supported
	 * @throws AccountModificationException  if we were unable to modify the account
	 * @throws AccountBannedException  if account is banned
	 * @throws AccountWrongPasswordException if the password is wrong
	 */
	public boolean loginValid(String user, String password, InetAddress address) throws NoSuchAlgorithmException, UnsupportedEncodingException, AccountModificationException, AccountBannedException, AccountWrongPasswordException
	{
		
		// o Convert password in utf8 byte array
		// ----------------------------------
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] raw = password.getBytes("UTF-8");
		byte[] hash = md.digest(raw);

		// o find Account
		// -------------
		Account acc = getAccount(user);

		// If account is not found
		// try to create it if AUTO_CREATE_ACCOUNTS is activated
		// or return false
		// ------------------------------------------------------
		if (acc == null)
			return handleAccountNotFound(user, address, hash);

		// If account is found
		// check ban state
		// check password and update last ip/last active
		// ---------------------------------------------
		else
		{
			// check the account is not ban
			if (acc.getAccessLevel() < 0)
				throw new AccountBannedException(user);
			try
			{
				checkPassword(hash, acc);
				handleGoodLogin(user, address);
			}
			// If password are different
			// -------------------------
			catch (AccountWrongPasswordException e)
			{
				handleBadLogin(user, password, address);
				return false;
			}
		}
		_logLogin.info("User "+user+" connected from "+address.getHostAddress());
		return true;
	}

	/**
	 * @param user
	 * @param address
	 */
	private void handleGoodLogin(String user, InetAddress address)
	{
	}

	/**
	 *
	 * If login are different, increment hackProtection counter. It's maybe a hacking attempt
	 *
	 * @param user
	 * @param password
	 * @param address
	 */
	private void handleBadLogin(String user, String password, InetAddress address)
	{
		_logLoginFailed.info("login failed for user : '" + user + "' " + (address == null ? "null" : address.getHostAddress()));
	}

	/**
	 * @param hash
	 * @param acc
	 * @throws AccountWrongPasswordException if password is wrong
	 */
	private void checkPassword(byte[] hash, Account acc) throws AccountWrongPasswordException
	{
		byte[] expected = Base64.decode(acc.getPassword());

		for (int i = 0; i < expected.length; i++)
		{
			if (hash[i] != expected[i])
				throw new AccountWrongPasswordException(acc.getLogin());
		}
	}

	/**
	 * @param user
	 * @param address
	 * @param hash
	 * @return true if accounts was successfully created or false is AUTO_CREATE_ACCOUNTS = false or creation failed
	 * @throws AccountModificationException
	 */
	private boolean handleAccountNotFound(String user, InetAddress address, byte[] hash) throws AccountModificationException
	{
		Account acc;
		if (Config.AUTO_CREATE_ACCOUNTS)
		{
			if ((user.length() >= 2) && (user.length() <= 14))
			{
				int numTryes = 0;
				if (_registredAccounts.containsKey(address.getHostAddress()))
					numTryes = _registredAccounts.get(address.getHostAddress());
				numTryes++;
				if(numTryes>Config.LOGIN_MAX_ACC_REG) {
					_logLogin.info("Address "+address.getHostAddress()+" banned");
					BanManager.getInstance().addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN * 1000);
					return false;
				}
				_registredAccounts.put(address.getHostAddress(),numTryes );
				acc = new Account(user, Base64.encodeBytes(hash), System.currentTimeMillis()/1000, 0, 0, (address == null ? "null" : address.getHostAddress()));
				addOrUpdateAccount(acc);
				_logLogin.info("created new account for " + user);
				return true;
			}
			_logLogin.warn("Invalid username creation/use attempt: " + user);
			return false;
		}
		_logLogin.warn("account missing for user " + user+" from ip "+address.getHostAddress());
		return false;
	}

	public synchronized void addOrUpdateAccount(Account acc) {
		PreparedStatement stm = null;
		try {
			stm = con.prepareStatement("update accounts set password=?,lastactive=?,accessLevel=?,lastIP=?,lastServerId=? where login=?");
			stm.setString(1, acc.getPassword());
			stm.setLong(2, acc.getLastactive());
			stm.setInt(3, acc.getAccessLevel());
			stm.setString(4, acc.getLastIp());
			stm.setInt(5,acc.getLastServerId());
			stm.setString(6, acc.getLogin());
			if(stm.executeUpdate()==0) {
				stm.close();
				stm = con.prepareStatement("insert into accounts (login,password,accessLevel,lastactive) values(?,?,?,0)");
				stm.setString(1, acc.getLogin());
				stm.setString(2, acc.getPassword());
				stm.setInt(3, acc.getAccessLevel());
				if(stm.executeUpdate()!=1)
					return;
			}
		} catch(SQLException e ) {
			_log.error("LoginManager: Unable to modify account",e);
		}
		finally {
			try {
				if(stm!=null)
					stm.close();
			} catch(SQLException e) { }
			}
	}

	private String getAllowedIP(String login)
	{
		String _iphost = "*";
		try
		{
			PreparedStatement statement = con.prepareStatement("SELECT allowed_ip FROM accounts WHERE login=?");
			statement.setString(1, login);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				_iphost = rset.getString(1);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not load allowedIp:", e);
		}
		return _iphost;
	}

	public void changeAllowedIP(String login, String host)
	{
		try
		{
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET allowed_ip=? WHERE login=?");
			statement.setString(1, host);
			statement.setString(2, login);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("ChangeAllowedIP: Could not write data. Reason: " + e);
		}
	}
	

	public void addOrUpdateAccount(String _uname, String _pass, int _level)  throws AccountModificationException {
		Account acc = getAccount(_uname);
		if(acc==null) {
			MessageDigest md;
			byte[] newpass;
			try
			{
				md = MessageDigest.getInstance("SHA");
				newpass = _pass.getBytes("UTF-8");
				newpass = md.digest(newpass);
			}
			catch (NoSuchAlgorithmException e1)
			{
				throw new AccountModificationException("No algorithm to encode password.", e1);
			}
			catch (UnsupportedEncodingException e1)
			{
				throw new AccountModificationException("Unsupported encoding.", e1);
			}
			
			acc = new Account(_uname,Base64.encodeBytes(newpass),_level);
			acc.setAccessLevel(_level);
		}
		addOrUpdateAccount(acc);
	}

	public void changeAccountLevel(String _uname, int _level) throws AccountModificationException {
		Account acc = getAccount(_uname);
		if(acc==null)
			throw new AccountModificationException();
		acc.setAccessLevel(_level);
		addOrUpdateAccount(acc);
	}

	public void deleteAccount(String _uname) throws AccountModificationException {
		PreparedStatement stm = null;
		try {
			stm = con.prepareStatement("delete from accounts where login=?");
			stm.setString(1, _uname);
			if(stm.executeUpdate()==0)
				throw new AccountModificationException();
		} catch(SQLException e ) {
			_log.error("LoginManager: Unable to delete account",e);
		}
		finally {
			try {
				if(stm!=null)
					stm.close();
			} catch(SQLException e) { }
		}
	}

	public List<Account> getAccountsInfo() {
		FastList<Account> result = new FastList<Account>();
		try {
			PreparedStatement stm = con.prepareStatement("select login,accessLevel from accounts");
			ResultSet rs = stm.executeQuery();
			while(rs.next()) 
				result.add(new Account(rs.getString(1),"",rs.getInt(2)));
			rs.close();
			stm.close();
		} catch(SQLException e) {
			_log.warn("LoginManager: Unable to read accounts",e);
		}
		return result;
	}
}
