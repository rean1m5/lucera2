package ru.catssoftware;

import ru.catssoftware.config.L2Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;



public class Config extends L2Config
{


	//=================================================================================
	public static final String	NETWORK_FILE	= "./config/main/network.properties";
	//=================================================================================
	public static String		DATABASE_DRIVER;
	public static String		DATABASE_URL;
	public static String		DATABASE_LOGIN;
	public static String		DATABASE_PASSWORD;
	public static boolean		IS_TELNET_ENABLED;
	public static String		LOGIN_SERVER_HOSTNAME;
	public static String		LOGIN_HOSTNAME;
	public static int			LOGIN_SERVER_PORT;
	public static int			LOGIN_PORT;
	public static int			IP_UPDATE_TIME;
	public static boolean		CARD_ENABLED;
	
	
	
	

	public static void loadNetworkConfig()
	{
		System.out.println("Loading: " + NETWORK_FILE + ".");
		try
		{
			L2Properties networkSettings = new L2Properties(NETWORK_FILE);

			IP_UPDATE_TIME = Integer.parseInt(networkSettings.getProperty("IpUpdateTime", "0")) * 60 * 1000;
			LOGIN_SERVER_PORT = Integer.parseInt(networkSettings.getProperty("LoginServerPort", "2106"));
			LOGIN_HOSTNAME = networkSettings.getProperty("LoginHostName", "127.0.0.1");
			LOGIN_SERVER_HOSTNAME = networkSettings.getProperty("LoginServerHostName", "0.0.0.0");
			LOGIN_PORT = Integer.parseInt(networkSettings.getProperty("LoginPort", "9014"));
			IS_TELNET_ENABLED = Boolean.valueOf(networkSettings.getProperty("EnableTelnet", "false"));
			DATABASE_DRIVER = networkSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = networkSettings.getProperty("URL", "jdbc:mysql://localhost/emurt_db");
			DATABASE_LOGIN = networkSettings.getProperty("Login", "root");
			DATABASE_PASSWORD = networkSettings.getProperty("Password", "root");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + NETWORK_FILE + " File.");
		}
	}

	//==========================================================================================
	public static final String	BAN_FILE	= "./config/main/ban.properties";
	//===========================================================================================
	public static int			LOGIN_TRY_BEFORE_BAN;
	public static int			LOGIN_BLOCK_AFTER_BAN;
	public static int			LOGIN_MAX_ACC_REG;
	public static int			INACTIVE_TIMEOUT;

	public static void loadBanConfig()
	{
		System.out.println("Loading: " + BAN_FILE + ".");
		try
		{
			Properties BanSettings = new Properties();
			InputStream is = new FileInputStream(new File(BAN_FILE));
			BanSettings.load(is);
			is.close();

			LOGIN_TRY_BEFORE_BAN = Integer.parseInt(BanSettings.getProperty("LoginTryBeforeBan", "3"));
			LOGIN_BLOCK_AFTER_BAN = Integer.parseInt(BanSettings.getProperty("LoginBlockAfterBan", "600"));
			LOGIN_MAX_ACC_REG = Integer.parseInt(BanSettings.getProperty("MaxAccountRegistration", "30"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + BAN_FILE + " File.");
		}
	}

	//==========================================================================================
	public static final String	SECURITY_FILE	= "./config/main/security.properties";
	//===========================================================================================
	public static int			FAST_CONNECTION_LIMIT;
	public static int			NORMAL_CONNECTION_TIME;
	public static int			FAST_CONNECTION_TIME;
	public static int			MAX_CONNECTION_PER_IP;
	public static boolean		FLOOD_PROTECTION;
	public static boolean		DEBUG;
	public static boolean		CRYPT_TOKEN = false;
	public static void loadSecurityConfig()
	{
		System.out.println("Loading: " + SECURITY_FILE + ".");
		try
		{
			Properties securitySettings = new Properties();
			InputStream is = new FileInputStream(new File(SECURITY_FILE));
			securitySettings.load(is);
			is.close();

			FLOOD_PROTECTION = Boolean.parseBoolean(securitySettings.getProperty("EnableFloodProtection", "true"));
			FAST_CONNECTION_LIMIT = Integer.parseInt(securitySettings.getProperty("FastConnectionLimit", "15"));
			NORMAL_CONNECTION_TIME = Integer.parseInt(securitySettings.getProperty("NormalConnectionTime", "700"));
			FAST_CONNECTION_TIME = Integer.parseInt(securitySettings.getProperty("FastConnectionTime", "350"));
			MAX_CONNECTION_PER_IP = Integer.parseInt(securitySettings.getProperty("MaxConnectionPerIP", "50"));
			INACTIVE_TIMEOUT = Integer.parseInt(securitySettings.getProperty("InactiveTimeOut", "3"));
			CARD_ENABLED = Boolean.parseBoolean(securitySettings.getProperty("EnableCardProtection","false"));
			if(securitySettings.containsKey("Debug"))
				DEBUG = Boolean.parseBoolean(securitySettings.getProperty("Debug"));
			if(securitySettings.containsKey("CryptToken"))
				CRYPT_TOKEN = Boolean.parseBoolean(securitySettings.getProperty("CryptToken"));

		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + SECURITY_FILE + " File.");
		}
	}

	//==========================================================================================
	public static final String	LOGIN_FILE	= "./config/main/loginserver.properties";
	//===========================================================================================

	public static enum OnSuccessLoginAction {
		COMMAND, NOTIFY
	};
	
	public static boolean		SHOW_LICENCE;
	public static boolean		ACCEPT_NEW_GAMESERVER;
	public static boolean		AUTO_CREATE_ACCOUNTS;
	public static int			GM_MIN;
	public static boolean		BRUT_PROTECTION_ENABLED;
	public static boolean		DDOS_PROTECTION_ENABLED;
	public static long			SESSION_TTL;
	public static int			MAX_SESSIONS;
	public static OnSuccessLoginAction ON_SUCCESS_LOGIN_ACTION;
	public static String		ON_SUCCESS_LOGIN_COMMAND;
	public static String		BRUTE_ACCOUNT_NAME;
	
	public static void loadLoginConfig()
	{
		System.out.println("Loading: " + LOGIN_FILE + ".");
		try
		{
			Properties serverSettings = new Properties();
			InputStream is = new FileInputStream(new File(LOGIN_FILE));
			serverSettings.load(is);
			is.close();
			ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer", "false"));
			GM_MIN = Integer.parseInt(serverSettings.getProperty("GMMinLevel", "1"));
			SHOW_LICENCE = Boolean.parseBoolean(serverSettings.getProperty("ShowLicence", "true"));
			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts", "false"));
			BRUT_PROTECTION_ENABLED = Boolean.parseBoolean(serverSettings.getProperty("BrutProtection","true"));
			DDOS_PROTECTION_ENABLED = Boolean.parseBoolean(serverSettings.getProperty("DDoSProtection","true"));
			SESSION_TTL = Long.parseLong(serverSettings.getProperty("SessionTTL", "10"))*1000;
			MAX_SESSIONS = Integer.parseInt(serverSettings.getProperty("MaxSessions", "100"));
			ON_SUCCESS_LOGIN_ACTION = OnSuccessLoginAction.valueOf(serverSettings.getProperty("OnSelectServer","NOTIFY").toUpperCase());
			ON_SUCCESS_LOGIN_COMMAND = serverSettings.getProperty("OnSelectServerCommand","");
			BRUTE_ACCOUNT_NAME=serverSettings.getProperty("BruteAccountName","");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + LOGIN_FILE + " File.");
		}
	}

	public static void debug(String msg) {
		if(DEBUG)
			System.out.println("!!!! "+msg);
	}
	public static void load()
	{
		Util.printSection("LoginServer Configuration");
		loadNetworkConfig();
		loadSecurityConfig();
		loadBanConfig();
		loadLoginConfig();
		Util.printSection("LoginServer DataBase Load");
	}
}
