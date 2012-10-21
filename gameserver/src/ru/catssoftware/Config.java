package ru.catssoftware;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.GSConfig;
import ru.catssoftware.gameserver.datatables.ClassTreeTable;
import ru.catssoftware.gameserver.handler.IReloadHandler;
import ru.catssoftware.gameserver.handler.ReloadHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.item.L2Armor;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.util.Console;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Config extends L2Config
{
	protected static Logger		_log					= Logger.getLogger(Config.class.getName());
	//=======================================================================================================

	private static HashMap<String, GSConfig> _config = new HashMap<String, GSConfig>();

	public static void addConfig(GSConfig config)
	{
		_config.put(config.getName(), config);
	}

	public static void reload(String configName)
	{
		GSConfig config = _config.get(configName);
		if (config != null)
			config.reload();
	}
	// ************************************************************************

	//===================================================================================
	public static final String	DATETIME_FILE		= "./config/main/datetime.properties";
	//===================================================================================
	public static boolean		DATETIME_SAVECAL;
	public static int			DATETIME_SUNRISE;
	public static int			DATETIME_SUNSET;
	public static int			DATETIME_MULTI;
	public static int			DATETIME_MOVE_DELAY;
	public static String		TIME_ZONE; 
	// ***********************************************************************************
	public static void loadDateTimeConfig()
	{
		_log.info("Loading: " + DATETIME_FILE);
		try
		{
			Properties datetimeSettings = new L2Properties("./" + DATETIME_FILE);

			DATETIME_SAVECAL = Boolean.parseBoolean(datetimeSettings.getProperty("SaveDate", "false"));
			DATETIME_MULTI = Integer.parseInt(datetimeSettings.getProperty("TimeMulti", "10"));
			DATETIME_SUNSET = Integer.parseInt(datetimeSettings.getProperty("SunSet", "0"));
			DATETIME_SUNRISE = Integer.parseInt(datetimeSettings.getProperty("SunRise", "6"));
			DATETIME_MOVE_DELAY = Integer.parseInt(datetimeSettings.getProperty("MoveDelay", "200"));
			TIME_ZONE = datetimeSettings.getProperty("TimeZone", "Europe/Moscow");
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + DATETIME_FILE + " File.");
		}
	}
	//========================================================================
	public static final String	GEO_FILE = "./config/main/geodata.properties";
	//========================================================================
	public static boolean	GEODATA;
	public static boolean	PATHFINDING;
	public static boolean	FORCE_GEODATA = true;
	public static File		GEODATA_ROOT;
	public static int		PATH_LENGTH;
	public static int		Z_DENSITY;
	public static boolean DEBUG_PATH;
	public static String GEOENGINE;
	
	public static enum		CorrectSpawnsZ
	{
		TOWN, MONSTER, ALL, NONE
	}

	public static CorrectSpawnsZ	GEO_CORRECT_Z;

	// *******************************************************************************************
	public static void loadGeoConfig()
	{
		_log.info("Loading: " + GEO_FILE + ".");
		try
		{
			Properties geoSettings = new L2Properties(GEO_FILE);
			GEODATA = Boolean.parseBoolean(geoSettings.getProperty("EnableGeoData", "true"));
			PATHFINDING = Boolean.parseBoolean(geoSettings.getProperty("EnablePathFinding", "true"));
			String correctZ = geoSettings.getProperty("GeoCorrectZ", "All");
			GEO_CORRECT_Z = CorrectSpawnsZ.valueOf(correctZ.toUpperCase());
			GEODATA_ROOT = new File(geoSettings.getProperty("GeoDataRoot", ".")).getCanonicalFile();
			GEOENGINE = geoSettings.getProperty("GeoEngine", "lucera");
			PATH_LENGTH = Integer.parseInt(geoSettings.getProperty("MaxPathLength","3500"));
			Z_DENSITY = Integer.parseInt(geoSettings.getProperty("ZAxisDensity","12"));
			DEBUG_PATH = Boolean.parseBoolean(geoSettings.getProperty("EnableDebugPath", "false"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + GEO_FILE + " File.");
		}
	}
	
	//=========================================================================================
	public static final String	BUILD_DATE_FILE	= "./config/versionning/build-time.properties";
	//=========================================================================================

	//==========================================================================================
	public static final String	GAMESERVER_FILE			= "./config/main/gameserver.properties";
	//==========================================================================================
	public static int			REQUEST_ID;
	public static int			MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	public static int			MAXIMUM_ONLINE_USERS;
	public static int			MIN_PROTOCOL_REVISION;
	public static int			MAX_PROTOCOL_REVISION;
	public static boolean		ACCEPT_ALTERNATE_ID;
	public static boolean		SERVER_LIST_BRACKET;
	public static boolean		SERVER_LIST_CLOCK;
	public static boolean		SERVER_GMONLY;
	public static File			DATAPACK_ROOT;
	public static boolean		COMPILE_SCRIPTS;
	public static int			GENERAL_THREAD_POOL_SIZE;
	public static int			PACKET_THREAD_POOL_SIZE;
	public static int			EFFECT_THREAD_POOL_SIZE;
	public static int			AI_THREAD_POOL_SIZE;
	public static int			THREAD_POOL_SIZE;
	public static String		SERVER_NAME;
	public static int			MMO_MAX_SEND_PER_PASS;
	public static int			MMO_SELECTOR_SLEEP_TIME;
	public static boolean		ENABLE_DDOS_PROTECTION;
	public static boolean		USE_OFF_EMULATOR;
	//**********************************************************************************************
	public static void loadGsConfig()
	{
		_log.info("Loading: " + GAMESERVER_FILE);
		try
		{
			Properties gsSettings = new L2Properties("./" + GAMESERVER_FILE);

			// --------------- ThreadPoolManager config -------------- 
			int CpuCount = Runtime.getRuntime().availableProcessors();
			GENERAL_THREAD_POOL_SIZE	= CpuCount * Integer.parseInt(gsSettings.getProperty("GeneralThreadPoolSize", "10"));
			PACKET_THREAD_POOL_SIZE		= CpuCount * Integer.parseInt(gsSettings.getProperty("PacketThreadPoolSize", "5"));
			EFFECT_THREAD_POOL_SIZE		= CpuCount * Integer.parseInt(gsSettings.getProperty("EffectThreadPoolSize", "5"));
			AI_THREAD_POOL_SIZE			= CpuCount * Integer.parseInt(gsSettings.getProperty("AiThreadPoolSize", "10"));
			THREAD_POOL_SIZE			= GENERAL_THREAD_POOL_SIZE + PACKET_THREAD_POOL_SIZE + EFFECT_THREAD_POOL_SIZE + AI_THREAD_POOL_SIZE;
			USE_OFF_EMULATOR = Boolean.parseBoolean(gsSettings.getProperty("UseOffEmulation","true"));	
			// ------------------------ Others ----------------------- 
			REQUEST_ID = Integer.parseInt(gsSettings.getProperty("RequestServerId", "1"));
			// Check server ID
			if (REQUEST_ID <= 0)
				REQUEST_ID = 1;

			MAXIMUM_ONLINE_USERS = Integer.parseInt(gsSettings.getProperty("MaximumOnlineUsers", "1000"));
			// Check max players online
			if (MAXIMUM_ONLINE_USERS > 5000)
				MAXIMUM_ONLINE_USERS = 5000;

			MAX_CHARACTERS_NUMBER_PER_ACCOUNT = Integer.parseInt(gsSettings.getProperty("CharMaxNumber", "7"));
			// Check max char per account
			if (MAX_CHARACTERS_NUMBER_PER_ACCOUNT > 7)
				MAX_CHARACTERS_NUMBER_PER_ACCOUNT = 7;
	
			ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(gsSettings.getProperty("AcceptAlternateId", "true"));
			DATAPACK_ROOT = new File(gsSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();
			SERVER_NAME = gsSettings.getProperty("ServerName", "L2Emu-RusTeam Server");
			MIN_PROTOCOL_REVISION = Integer.parseInt(gsSettings.getProperty("MinProtocolRevision", "12"));
			MAX_PROTOCOL_REVISION = Integer.parseInt(gsSettings.getProperty("MaxProtocolRevision", "17"));
			// Check protocols
			if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
			{
				MIN_PROTOCOL_REVISION = 12;
				MAX_PROTOCOL_REVISION = 17;
				throw new Error("Protocol configuration setting set to default.");
			}
			MMO_MAX_SEND_PER_PASS = Integer.parseInt(gsSettings.getProperty("MaxSendPerPass", "12"));
			MMO_SELECTOR_SLEEP_TIME = Integer.parseInt(gsSettings.getProperty("SleepTime", "20"));
			ENABLE_DDOS_PROTECTION = Boolean.parseBoolean(gsSettings.getProperty("UseLoginProtection","true"));
			COMPILE_SCRIPTS = Boolean.parseBoolean(gsSettings.getProperty("CompileScripts","true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + GAMESERVER_FILE + " File.");
		}



		File f = new File("./libs/");
		String lname = "licman";

		if(System.getProperty("os.arch").contains("64"))
			lname+="64";
		if(System.getProperty("os.name").toLowerCase().contains("windows"))
			lname+=".dll";
		else
			lname+=".so";

		try
		{
			System.load(new File(f,lname).getAbsolutePath());
			checkConfig(Config.class.getClassLoader());
		} catch(Exception e)
		{
		}
	}

	//==========================================================================================
	public static final String	L2TOP_FILE			= "./config/main/l2top.properties";
	//==========================================================================================
	// Off эмулятор
	// Выдача награды
	public static boolean		L2TOPDEMON_ENABLED;
	public static int			L2TOPDEMON_POLLINTERVAL;
	public static int			L2TOPDEMON_SERVERID;
	public static String		L2TOPDEMON_KEY;
	public static String		DEAMON_NAMEALLOWED;
	public static int			L2TOPDEMON_MIN;
	public static int			L2TOPDEMON_MAX;
	public static int			L2TOPDEMON_ITEM;
	public static String		L2TOPDEMON_PREFIX;
	public static boolean		L2TOPDEMON_IGNOREFIRST;
	public static RewardMode	L2TOP_REW_MODE;
	public static enum			RewardMode
	{
		ALL, SMS, WEB
	}

	//**********************************************************************************************
	public static void loadL2topConfig()
	{
		_log.info("Loading: " + L2TOP_FILE);
		try
		{
			Properties l2topSettings = new L2Properties("./" + L2TOP_FILE);


			L2TOPDEMON_ENABLED = Boolean.parseBoolean(l2topSettings.getProperty("L2TopDeamonEnabled","false"));
			L2TOPDEMON_SERVERID = Integer.parseInt(l2topSettings.getProperty("L2TopDeamonServerID","0"));
			L2TOPDEMON_KEY = l2topSettings.getProperty("L2TopDeamonServerKey","");
			L2TOPDEMON_POLLINTERVAL = Integer.parseInt(l2topSettings.getProperty("L2TopDeamonPollInterval","10"));
			L2TOPDEMON_PREFIX = l2topSettings.getProperty("L2TopDeamonPrefix","");
			L2TOPDEMON_ITEM = Integer.parseInt(l2topSettings.getProperty("L2TopDeamonRewardItem","0"));
			L2TOPDEMON_MIN = Integer.parseInt(l2topSettings.getProperty("L2TopDeamonMin","1"));
			L2TOPDEMON_MAX = Integer.parseInt(l2topSettings.getProperty("L2TopDeamonMax","1"));
			DEAMON_NAMEALLOWED = l2topSettings.getProperty("AllowedNames",".+");
			L2TOPDEMON_IGNOREFIRST = Boolean.parseBoolean(l2topSettings.getProperty("L2TopDeamonDoNotRewardAtFirstTime","false"));
			L2TOP_REW_MODE = RewardMode.valueOf(l2topSettings.getProperty("L2TopRewardMode", "ALL"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + L2TOP_FILE + " File.");
		}
	}

	//==========================================================================================
	//=====================================================================
	public static final String	HEXID_FILE	= "./config/network/hexid.txt";
	//=====================================================================
	public static byte[]		HEX_ID;
	public static int			SERVER_ID;

	//*******************************************
	public static void loadHexidConfig()
	{
		_log.info("Loading: " + HEXID_FILE);
		try
		{
			Properties hexidSettings = new L2Properties("./" + HEXID_FILE);

			SERVER_ID = Integer.parseInt(hexidSettings.getProperty("ServerID"));
			HEX_ID = new BigInteger(hexidSettings.getProperty("HexID"), 16).toByteArray();
		}
		catch (Exception e)
		{
			_log.warn("Could Not load Hexid File (" + HEXID_FILE + "), Hopefully login will Give us one.");
		}
	}

	//=======================================================================================
	public static final String	SECURITY_FILE		= "./config/main/security.properties";
	//=======================================================================================
	public static int			DEFAULT_PUNISH	=2;
	public static int			SAFE_REBOOT_TIME	= 10;
	public static boolean		SAFE_REBOOT;
	public static boolean		SAFE_REBOOT_DISABLE_ENCHANT;
	public static boolean		SAFE_REBOOT_DISABLE_TELEPORT;
	public static boolean		SAFE_REBOOT_DISABLE_CREATEITEM;
	public static boolean		SAFE_REBOOT_DISABLE_TRANSACTION;
	public static boolean		SAFE_REBOOT_DISABLE_PC_ITERACTION;
	public static boolean		SAFE_REBOOT_DISABLE_NPC_ITERACTION;
	public static int			DISCONNECTED_UNKNOWN_PACKET;
	public static boolean		BAN_BOT_USERS;
	public static boolean		CheckSystemParam;
	public static boolean		Allow_Same_HWID_On_Olympiad;
	public static boolean		Allow_Same_HWID_On_Events;
	public static boolean		Allow_Same_IP_On_Events;
	public static int			ENCHAT_TIME;

	//*******************************************************
	public static void loadSecurityConfig()
	{
		_log.info("Loading: " + SECURITY_FILE);
		try
		{
			Properties securitySettings = new L2Properties("./" + SECURITY_FILE);

			SAFE_REBOOT = Boolean.parseBoolean(securitySettings.getProperty("SafeReboot", "true"));
			SAFE_REBOOT_TIME = Integer.parseInt(securitySettings.getProperty("SafeRebootTime", "10"));
			SAFE_REBOOT_DISABLE_ENCHANT = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisableEnchant", "false"));
			SAFE_REBOOT_DISABLE_TELEPORT = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisableTeleport", "false"));
			SAFE_REBOOT_DISABLE_CREATEITEM = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisableCreateItem", "false"));
			SAFE_REBOOT_DISABLE_TRANSACTION = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisableTransaction", "false"));
			SAFE_REBOOT_DISABLE_PC_ITERACTION = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisablePcIteraction", "false"));
			SAFE_REBOOT_DISABLE_NPC_ITERACTION = Boolean.parseBoolean(securitySettings.getProperty("SafeRebootDisableNpcIteraction", "false"));
			DISCONNECTED_UNKNOWN_PACKET = Integer.parseInt(securitySettings.getProperty("UnknownPacketPerSession", "10"));
			BAN_BOT_USERS = Boolean.parseBoolean(securitySettings.getProperty("EnableBotBan", "true"));
			CheckSystemParam = Boolean.parseBoolean(securitySettings.getProperty("CheckSystemParam", "true"));
			Allow_Same_HWID_On_Olympiad = Boolean.parseBoolean(securitySettings.getProperty("SameHWIDOnOlypmiad", "false"));
			Allow_Same_HWID_On_Events = Boolean.parseBoolean(securitySettings.getProperty("SameHWIDOnEvents", "false"));
			Allow_Same_IP_On_Events = Boolean.parseBoolean(securitySettings.getProperty("SameIPOnEvents", "true"));
			ENCHAT_TIME = Integer.parseInt(securitySettings.getProperty("EnchantProtectTime","3"));
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new Error("Failed to Load " + SECURITY_FILE + " File.");
		}
	}

	//==================================================================================
	public static final String	NETWORK_FILE	= "./config/network/network.properties";
	//==================================================================================

	public static String		SUBNETWORKS;
	public static int			GAME_SERVER_LOGIN_PORT;
	public static int			PORT_GAME;
	public static String		GAME_SERVER_LOGIN_HOST;
	public static String		INTERNAL_HOSTNAME;
	public static String		INTERNAL_NETWORKS;
	public static String		EXTERNAL_HOSTNAME;
	public static String		OPTIONAL_NETWORKS;
	public static String		GAMESERVER_HOSTNAME;
	public static int			DATABASE_MAX_CONNECTIONS;
	public static String		DATABASE_DRIVER;
	public static String		DATABASE_URL;
	public static String		DATABASE_LOGIN;
	public static String		DATABASE_PASSWORD;

	//**************************************************************
	public static void loadNetworkConfig()
	{
		_log.info("Loading: " + NETWORK_FILE);
		try
		{
			Properties networkSettings = new L2Properties("./" + NETWORK_FILE);

			EXTERNAL_HOSTNAME = networkSettings.getProperty("ExternalHostname", "*");
			INTERNAL_NETWORKS = networkSettings.getProperty("InternalNetworks", "");
			INTERNAL_HOSTNAME = networkSettings.getProperty("InternalHostname", "*");
			OPTIONAL_NETWORKS = networkSettings.getProperty("OptionalNetworks", "");
			GAME_SERVER_LOGIN_PORT = Integer.parseInt(networkSettings.getProperty("LoginPort", "9014"));
			GAME_SERVER_LOGIN_HOST = networkSettings.getProperty("LoginHost", "127.0.0.1");
			GAMESERVER_HOSTNAME = networkSettings.getProperty("GameServerHostName");
			PORT_GAME = Integer.parseInt(networkSettings.getProperty("GameServerPort", "7777"));
			DATABASE_DRIVER = networkSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = networkSettings.getProperty("URL", "jdbc:mysql://localhost/emurt_db?useUnicode=true&characterEncoding=utf-8");
			DATABASE_LOGIN = networkSettings.getProperty("Login", "root");
			DATABASE_PASSWORD = networkSettings.getProperty("Password", "root");
			DATABASE_MAX_CONNECTIONS = Integer.parseInt(networkSettings.getProperty("MaximumDbConnections", "10"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load: " + NETWORK_FILE + " File.");
		}
		LineNumberReader lnr = null;
		try
		{

			String subnet = null;
			String line = null;
			SUBNETWORKS = "";

			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(new File(NETWORK_FILE))));

			while ((line = lnr.readLine()) != null)
			{
				line = line.trim().toLowerCase().replace(" ", "");

				if (line.startsWith("subnet"))
				{
					if (SUBNETWORKS.length() > 0 && !SUBNETWORKS.endsWith(";"))
						SUBNETWORKS += ";";
					subnet = line.split("=")[1];
					SUBNETWORKS += subnet;
				}
			}

			SUBNETWORKS = SUBNETWORKS.toLowerCase().replaceAll("internal", Config.INTERNAL_HOSTNAME);
			SUBNETWORKS = SUBNETWORKS.toLowerCase().replaceAll("external", Config.EXTERNAL_HOSTNAME);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (lnr != null)
					lnr.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	//=========================================================================
	public static final String	NICKS_FILE	= "./config/main/nicks.properties";
	//=========================================================================
	public static Pattern			CNAME_PATTERN;
	public static Pattern			PET_NAME_PATTERN;
	public static Pattern			CLAN_ALLY_NAME_PATTERN;
	public static Pattern			TITLE_PATTERN;

	//***************************************************************************
	public static void loadNicksConfig()
	{
		_log.info("Loading: " + NICKS_FILE);
		try
		{
			Properties nickSettings = new L2Properties("./" + NICKS_FILE);

			try
			{
				CNAME_PATTERN = Pattern.compile(nickSettings.getProperty("CnameTemplate", "[A-Za-z0-9\\-]{3,16}"));
			}
			catch (PatternSyntaxException e)
			{
				_log.warn("GameServer: Character name pattern is wrong!", e);
				CNAME_PATTERN = Pattern.compile("[A-Za-z0-9\\-]{3,16}");
			}
			try
			{
				PET_NAME_PATTERN = Pattern.compile(nickSettings.getProperty("PetNameTemplate", "[A-Za-z0-9\\-]{3,16}"));
			}
			catch (PatternSyntaxException e)
			{
				_log.warn("GameServer: Pet name pattern is wrong!", e);
				PET_NAME_PATTERN = Pattern.compile("[A-Za-z0-9\\-]{3,16}");
			}
			try
			{
				CLAN_ALLY_NAME_PATTERN = Pattern.compile(nickSettings.getProperty("ClanAllyNameTemplate", "[A-Za-z0-9 \\-]{3,16}"));
			}
			catch (PatternSyntaxException e)
			{
				_log.warn("GameServer: Clan and ally name pattern is wrong!", e);
				CLAN_ALLY_NAME_PATTERN = Pattern.compile("[A-Za-z0-9 \\-]{3,16}");
			}
			try
			{
				TITLE_PATTERN = Pattern.compile(nickSettings.getProperty("TitleTemplate", "[A-Za-z0-9 \\\\[\\\\]\\(\\)\\<\\>\\|\\!]{3,16}"));
			}
			catch (PatternSyntaxException e)
			{
				_log.warn("GameServer: Character title pattern is wrong!", e);
				TITLE_PATTERN = Pattern.compile("[A-Za-z0-9 \\\\[\\\\]\\(\\)\\<\\>\\|\\!]{3,16}");
			}
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + NICKS_FILE + " File.");

		}
	}

	//========================================================================
	public static final String	CHAT_FILE	= "./config/chat/chat.properties";
	//========================================================================
	public static enum ChatMode
	{
		GLOBAL, REGION, GM, OFF
	}
	public static List<Pattern> FILTER_LIST = new ArrayList<Pattern>();
	public static ChatMode		DEFAULT_GLOBAL_CHAT;
	public static ChatMode		DEFAULT_TRADE_CHAT;
	public static boolean		REGION_CHAT_ALSO_BLOCKED;
	public static boolean		ALT_HERO_CHAT_SYSTEM;
	public static int			PVP_COUNT_TO_CHAT;
	public static int			CHAT_LENGTH;
	public static boolean		ALLOW_MULTILINE_CHAT;
	public static boolean		USE_SAY_FILTER;
	public static String		CHAT_FILTER_CHARS;
	public static int			KARMA_ON_OFFENSIVE;
	public static boolean		LOG_CHAT;
	public static int			GLOBAL_CHAT_TIME;
	public static int			TRADE_CHAT_TIME;
	public static int			HERO_CHAT_TIME;
	public static int			SHOUT_CHAT_LEVEL;
	public static int			TRADE_CHAT_LEVEL;

	//*********************************************************
	public static void loadChatConfiguration()
	{
		_log.info("Loading: " + CHAT_FILE);
		try
		{
			Properties chatSettings = new L2Properties("./" + CHAT_FILE);

			DEFAULT_GLOBAL_CHAT = ChatMode.valueOf(chatSettings.getProperty("GlobalChat", "REGION").toUpperCase());
			DEFAULT_TRADE_CHAT = ChatMode.valueOf(chatSettings.getProperty("TradeChat", "REGION").toUpperCase());
			REGION_CHAT_ALSO_BLOCKED = Boolean.parseBoolean(chatSettings.getProperty("RegionChatAlsoBlocked", "false"));
			ALT_HERO_CHAT_SYSTEM = Boolean.parseBoolean(chatSettings.getProperty("AllowAltHeroSystem", "false"));
			PVP_COUNT_TO_CHAT = Integer.parseInt(chatSettings.getProperty("PvPCountToChat", "5000"));
			ALLOW_MULTILINE_CHAT = Boolean.parseBoolean(chatSettings.getProperty("AllowMultiLineChat", "false"));
			USE_SAY_FILTER = Boolean.parseBoolean(chatSettings.getProperty("UseChatFilter", "true"));
			CHAT_LENGTH = Integer.parseInt(chatSettings.getProperty("ChatLength","120"));
			KARMA_ON_OFFENSIVE = Integer.parseInt(chatSettings.getProperty("ChatFilterKarma", "0"));
			LOG_CHAT = Boolean.parseBoolean(chatSettings.getProperty("LogChatOnFile", "false"));
			GLOBAL_CHAT_TIME = Integer.parseInt(chatSettings.getProperty("ShoutChatReuseDelay", "1"));
			TRADE_CHAT_TIME = Integer.parseInt(chatSettings.getProperty("TradeChatReuseDelay", "1"));
			HERO_CHAT_TIME = Integer.parseInt(chatSettings.getProperty("HeroChatReuseDelay", "1"));
			CHAT_FILTER_CHARS = chatSettings.getProperty("ChatFilterChars", "[Censored]");
			if (CHAT_LENGTH > 400)
				CHAT_LENGTH = 120;
			SHOUT_CHAT_LEVEL = Integer.parseInt(chatSettings.getProperty("ShoutChatLevel","1"));
			TRADE_CHAT_LEVEL = Integer.parseInt(chatSettings.getProperty("TradeChatLevel","1"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + CHAT_FILE + " File.");
		}
	}

	//========================================================================
	public static final String	JAIL_FILE	= "./config/mods/jail.properties";
	//=========================================================================
	
	public static int			REQUIRED_JAIL_POINTS;
	public static int			POINTS_PER_KILL;
	public static int			JAIL_POINT_CHANCE;
	public static int			POINTS_LOST_PER_DEATH;
	public static boolean		JAIL_IS_PVP;
	public static boolean		ALLOW_JAILMANAGER;
	public static boolean		REDUCE_JAIL_POINTS_ON_DEATH;
	public static boolean		NOTIY_ADMINS_OF_ILLEGAL_ACTION;
	public static boolean		JAIL_SPAWN_SYSTEM;

	//**********************************************************************************************
	public static void loadJailConfig()
	{
		_log.info("Loading: " + JAIL_FILE);
		try
		{
			Properties jailSettings = new L2Properties("./" + JAIL_FILE);

			JAIL_POINT_CHANCE = Integer.parseInt(jailSettings.getProperty("PointChance", "100"));
			POINTS_PER_KILL = Integer.parseInt(jailSettings.getProperty("PointsPerKill", "1"));
			POINTS_LOST_PER_DEATH = Integer.parseInt(jailSettings.getProperty("PointsLostPerDeath", "0"));
			REQUIRED_JAIL_POINTS = Integer.parseInt(jailSettings.getProperty("RequiredJailPoints", "20"));
			JAIL_SPAWN_SYSTEM = Boolean.parseBoolean(jailSettings.getProperty("EnableJailSpawnSystem", "false"));
			NOTIY_ADMINS_OF_ILLEGAL_ACTION = Boolean.parseBoolean(jailSettings.getProperty("NotifyAdminsOfIllegalAction", "false"));
			ALLOW_JAILMANAGER = Boolean.parseBoolean(jailSettings.getProperty("AllowJailManager", "true"));
			REDUCE_JAIL_POINTS_ON_DEATH = Boolean.parseBoolean(jailSettings.getProperty("ReduceJailPointsOnDeath", "true"));
			JAIL_IS_PVP = Boolean.parseBoolean(jailSettings.getProperty("JailIsPvpZone", "true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + JAIL_FILE + " File.");
		}
	}

	//========================================================================
	public static final String	BAN_FILE	= "./config/main/ban.properties";
	//=========================================================================

	public static boolean	BAN_CHAT_LOG;
	public static boolean	BAN_ACCOUNT_LOG;
	public static boolean	JAIL_LOG;
	public static boolean	BAN_CHAR_LOG;
	
	public static boolean	CLASSIC_ANNOUNCE_MODE;

	public static boolean	ANNOUNCE_BAN_CHAT;
	public static boolean	ANNOUNCE_UNBAN_CHAT;
	public static boolean	ANNOUNCE_BAN_ACCOUNT;
	public static boolean	ANNOUNCE_UNBAN_ACCOUNT;
	public static boolean	ANNOUNCE_JAIL;
	public static boolean	ANNOUNCE_UNJAIL;
	public static boolean	ANNOUNCE_BAN_CHAR;
	public static boolean	ANNOUNCE_UNBAN_CHAR;
	
	public static int		GLOBAL_BAN_CHAT_TIME;

	//**********************************************************************************************
	public static void loadBanConfig()
	{
		_log.info("Loading: " + BAN_FILE);
		try
		{
			Properties banSettings = new L2Properties("./" + BAN_FILE);
			
			BAN_CHAT_LOG = Boolean.parseBoolean(banSettings.getProperty("BanChatLog", "true"));
			BAN_ACCOUNT_LOG = Boolean.parseBoolean(banSettings.getProperty("BanAccountLog", "true"));
			JAIL_LOG = Boolean.parseBoolean(banSettings.getProperty("JailLog", "true"));
			BAN_CHAR_LOG = Boolean.parseBoolean(banSettings.getProperty("PlayerBanLog", "true"));
			
			CLASSIC_ANNOUNCE_MODE = Boolean.parseBoolean(banSettings.getProperty("AnnounceBanChat", "true"));

			ANNOUNCE_BAN_CHAT = Boolean.parseBoolean(banSettings.getProperty("AnnounceBanChat", "true"));
			ANNOUNCE_UNBAN_CHAT = Boolean.parseBoolean(banSettings.getProperty("AnnounceUnbanChat", "true"));
			ANNOUNCE_BAN_ACCOUNT = Boolean.parseBoolean(banSettings.getProperty("AnnounceBanAccount", "true"));
			ANNOUNCE_UNBAN_ACCOUNT = Boolean.parseBoolean(banSettings.getProperty("AnnounceUnBanAccount", "true"));
			ANNOUNCE_JAIL = Boolean.parseBoolean(banSettings.getProperty("AnnounceJail", "true"));
			ANNOUNCE_UNJAIL = Boolean.parseBoolean(banSettings.getProperty("AnnounceUnJail", "true"));
			
			GLOBAL_BAN_CHAT_TIME = Integer.parseInt(banSettings.getProperty("GlobalBanTime", "15"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + BAN_FILE + " File.");
		}
	}

	public static final String	ANNOUNCE_FILE = "./config/main/announces.properties";
	public static String WORLD_ANNOUNCES;
	public static String CLAN_ANNOUNCE;
	public static String ALLY_ANOUNCE;
	public static void loadAnnounces() {
		try {
			_log.info("Loading: " + ANNOUNCE_FILE);
			L2Properties p = new L2Properties(ANNOUNCE_FILE);
			ALT_ANNOUNCE_PK = Boolean.parseBoolean(p.getProperty("AnnouncePk", "false"));
			ALT_ANNOUNCE_PK_NORMAL_MESSAGE = Boolean.parseBoolean(p.getProperty("AnnouncePkNormalMessage", "false"));
			ONLINE_PLAYERS_AT_STARTUP = Boolean.parseBoolean(p.getProperty("ShowOnlinePlayersAtStartup", "false"));
			LOAD_AUTOANNOUNCE_AT_STARTUP  = Boolean.parseBoolean(p.getProperty("LoadAutoAnnounceAtStartup", "true"));
			ONLINE_PLAYERS_ANNOUNCE_INTERVAL = Integer.parseInt(p.getProperty("OnlinePlayersAnnounceInterval", "900000"));
            ONLINE_PLAYERS_MULTIPLIER = Double.parseDouble(p.getProperty("OnlinePlayersMultiplier", "1"));
			WORLD_ANNOUNCES = p.getProperty("AnnounceWorld", "");
			CLAN_ANNOUNCE = p.getProperty("AnnounceClan","");
			ALLY_ANOUNCE = p.getProperty("AnnounceAlly","");
		} catch(Exception e) {
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + BAN_FILE + " File.");
		}
	}
	//=========================================================================
	public static final String	CUSTOM_FILE	= "./config/main/custom.properties";
	//=========================================================================

	public static int			ONLINE_PLAYERS_ANNOUNCE_INTERVAL;
    public static double        ONLINE_PLAYERS_MULTIPLIER;
	public static boolean		ONLINE_PLAYERS_AT_STARTUP;
	public static boolean		LOAD_AUTOANNOUNCE_AT_STARTUP;

	public static int			WEAR_DELAY;
	public static int			WEAR_PRICE;

	public static boolean		ALT_ANNOUNCE_PK;
	public static boolean		ALT_ANNOUNCE_PK_NORMAL_MESSAGE;
	public static boolean		ALT_PLAYER_CAN_DROP_ADENA;
	public static double		ALT_WEIGHT_LIMIT;

	public static boolean		CHAR_VIP_SKIP_SKILLS_CHECK;
	public static boolean		SHOW_HTML_WELCOME;
	public static int			MAX_SUBCLASS;

	public static boolean		ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static boolean		ALT_DISABLE_RAIDBOSS_PETRIFICATION;
	public static int			MAX_LEVEL_RAID_CURSE;
	public static boolean		ALT_RECOMMEND;
	public static boolean		ALT_BLACKSMITH_USE_RECIPES;

	public static boolean		SUBCLASS_WITH_ITEM_AND_NO_QUEST;
	public static boolean		SUBCLASS_WITH_CUSTOM_ITEM;
	public static int			SUBCLASS_WITH_CUSTOM_ITEM_ID;
	public static int			SUBCLASS_WITH_CUSTOM_ITEM_COUNT;
	public static byte			SUBCLASS_INIT_LEVEL;
	public static byte			SUBCLASS_MAX_LEVEL_BYTE;
	public static int			SUBCLASS_MAX_LEVEL;
	
	public static boolean		ACUMULATIVE_SUBCLASS_SKILLS;
	public static boolean		LOAD_CUSTOM_TELEPORTS;
	public static boolean		LOAD_CUSTOM_MERCHANT_BUYLISTS;
	public static boolean		ALLOW_TELE_IN_SIEGE_TOWN;
	public static int			PET_TICKET_ID;
	public static int			SPECIAL_PET_TICKET_ID;

	public static int			PLAYER_RATE_DROP_ADENA;
	public static boolean		ALT_MANA_POTIONS;
	public static int			MANAHEAL_POWER;
	public static boolean		ALT_GAME_FREE_TELEPORT;
	public static boolean		FORCE_UPDATE_RAIDBOSS_ON_DB;

	public static int			COLOR_FOR_AMMOUNT1;
	public static int			COLOR_FOR_AMMOUNT2;
	public static int			COLOR_FOR_AMMOUNT3;
	public static int			COLOR_FOR_AMMOUNT4;
	public static int			COLOR_FOR_AMMOUNT5;
	public static int			PVP_AMMOUNT1;
	public static int			PVP_AMMOUNT2;
	public static int			PVP_AMMOUNT3;
	public static int			PVP_AMMOUNT4;
	public static int			PVP_AMMOUNT5;
	public static boolean		PVP_COLOR_SYSTEM;
	public static int 			PVP_COLOR_MODE;	
	public static int			TITLE_COLOR_FOR_AMMOUNT1;
	public static int			TITLE_COLOR_FOR_AMMOUNT2;
	public static int			TITLE_COLOR_FOR_AMMOUNT3;
	public static int			TITLE_COLOR_FOR_AMMOUNT4;
	public static int			TITLE_COLOR_FOR_AMMOUNT5;

	public static boolean		ALLOW_NEW_CHARACTER_TITLE;
	public static String		NEW_CHARACTER_TITLE;
	public static boolean		ALLOW_NEW_CHAR_CUSTOM_POSITION;
	public static int			NEW_CHAR_POSITION_X;
	public static int			NEW_CHAR_POSITION_Y;
	public static int			NEW_CHAR_POSITION_Z;

	public static boolean		NEW_CHAR_IS_NOBLE;
	public static boolean		ENABLE_STARTUP_LVL;
	public static int			ADD_LVL_NEWBIE;
	public static boolean		LEVEL_ADD_LOAD;
	public static int			STARTING_AA;
	public static int			STARTING_ADENA;

	public static boolean		ALLOW_SERVER_WELCOME_MESSAGE;
	public static String		SERVER_WELCOME_MESSAGE;

	public static boolean		ALLOW_PVP_REWARD;
	public static int			PVP_REWARD_ITEM_ID;
	public static int			PVP_REWARD_ITEM_AMMOUNT;
	public static int			PVP_REWARD_LEVEL;
	public static int			PVP_REWARD_TIME;
	public static boolean		PVP_CHECK_HWID;				

	public static boolean		SHOW_SKILL_SUCCESS_CHANCE;
	public static boolean		SHOW_DEBUFF_ONLY;

	public static boolean		ALLOW_MENU;
	public static String 		DISABLED_COMMANDS;
	public static boolean		ALLOW_CUSTOM_STARTER_ITEMS;
	public static List<int[]>	CUSTOM_STARTER_ITEMS = new FastList<int[]>();
	public static boolean		ALT_ENCHANT_HP_BONUS;
	public static int			AUCTION_ITEM_ID;
	public static final int		PVP_MODE_TITLE = 1;
	public static final int		PVP_MODE_NAME = 2;
	public static final int		PVP_MODE_BOTH = 3;
	public static int		DEAMON_MAX_VOTES;
	public static int		RAID_MIN_MP_TO_CAST;
	public static boolean 	GUARD_ATTACK_MOBS;
	public static boolean	EPIC_REQUIRE_QUEST;
        public static boolean   PET_FOOD;
	//*********************************************************
	public static void loadCustomConfig()
	{
		_log.info("Loading: " + CUSTOM_FILE);
		try
		{
			Properties customSettings = new L2Properties("./" + CUSTOM_FILE);

			AUCTION_ITEM_ID = Integer.parseInt(customSettings.getProperty("AuctionBidItemId","57"));
			PVP_COLOR_SYSTEM = Boolean.parseBoolean(customSettings.getProperty("PvPColorSystem", "false"));
			String mode = customSettings.getProperty("PvPColorMode","Title");
			if(mode.equalsIgnoreCase("title"))
				PVP_COLOR_MODE = PVP_MODE_TITLE;
			else  if(mode.equalsIgnoreCase("name"))
				PVP_COLOR_MODE = PVP_MODE_NAME;
			else 
				PVP_COLOR_MODE  = PVP_MODE_TITLE | PVP_MODE_NAME;
			COLOR_FOR_AMMOUNT1 = Integer.decode("0x" + customSettings.getProperty("ColorForAmmount1", "00FF00"));
			COLOR_FOR_AMMOUNT2 = Integer.decode("0x" + customSettings.getProperty("ColorForAmmount2", "00FF00"));
			COLOR_FOR_AMMOUNT3 = Integer.decode("0x" + customSettings.getProperty("ColorForAmmount3", "00FF00"));
			COLOR_FOR_AMMOUNT4 = Integer.decode("0x" + customSettings.getProperty("ColorForAmmount4", "00FF00"));
			COLOR_FOR_AMMOUNT5 = Integer.decode("0x" + customSettings.getProperty("ColorForAmmount4", "00FF00"));
			TITLE_COLOR_FOR_AMMOUNT1 = Integer.decode("0x" + customSettings.getProperty("TitleForAmmount1", "00FF00"));
			TITLE_COLOR_FOR_AMMOUNT2 = Integer.decode("0x" + customSettings.getProperty("TitleForAmmount2", "00FF00"));
			TITLE_COLOR_FOR_AMMOUNT3 = Integer.decode("0x" + customSettings.getProperty("TitleForAmmount3", "00FF00"));
			TITLE_COLOR_FOR_AMMOUNT4 = Integer.decode("0x" + customSettings.getProperty("TitleForAmmount4", "00FF00"));
			TITLE_COLOR_FOR_AMMOUNT5 = Integer.decode("0x" + customSettings.getProperty("TitleForAmmount5", "00FF00"));
			PVP_AMMOUNT1 = Integer.parseInt(customSettings.getProperty("PvpAmmount1", "50"));
			PVP_AMMOUNT2 = Integer.parseInt(customSettings.getProperty("PvpAmmount2", "100"));
			PVP_AMMOUNT3 = Integer.parseInt(customSettings.getProperty("PvpAmmount3", "150"));
			PVP_AMMOUNT4 = Integer.parseInt(customSettings.getProperty("PvpAmmount4", "250"));
			PVP_AMMOUNT5 = Integer.parseInt(customSettings.getProperty("PvpAmmount5", "500"));
			PVP_CHECK_HWID = Boolean.parseBoolean(customSettings.getProperty("PvpCheckHWID","false"));
			PET_TICKET_ID = Integer.parseInt(customSettings.getProperty("PetTicketID", "13273"));
			PVP_REWARD_TIME = Integer.parseInt(customSettings.getProperty("PvpRewardTime", "30"));
			SPECIAL_PET_TICKET_ID = Integer.parseInt(customSettings.getProperty("SpecialPetTicketID", "0"));
			LOAD_CUSTOM_TELEPORTS = Boolean.parseBoolean(customSettings.getProperty("LoadCustomTeleports", "false"));
			LOAD_CUSTOM_MERCHANT_BUYLISTS = Boolean.parseBoolean(customSettings.getProperty("LoadCustomBuylists", "false"));
			SUBCLASS_WITH_ITEM_AND_NO_QUEST = Boolean.parseBoolean(customSettings.getProperty("SubclassWithItemAndNoQuest", "false"));
			SUBCLASS_WITH_CUSTOM_ITEM = Boolean.parseBoolean(customSettings.getProperty("SubclassWithCustomItem", "false"));;
			SUBCLASS_WITH_CUSTOM_ITEM_ID = Integer.parseInt(customSettings.getProperty("SubclassWithCustomItemID", "4037"));
			SUBCLASS_WITH_CUSTOM_ITEM_COUNT  = Integer.parseInt(customSettings.getProperty("SubclassWithCustomItemCount", "1"));

			SUBCLASS_MAX_LEVEL_BYTE = Byte.parseByte(customSettings.getProperty("SubclassMaxLevel", "80"));
			SUBCLASS_MAX_LEVEL = Integer.parseInt(customSettings.getProperty("SubclassMaxLevel", "80"));
			SUBCLASS_INIT_LEVEL = Byte.parseByte(customSettings.getProperty("SublcassInitLevel", "40"));
				if (SUBCLASS_INIT_LEVEL > SUBCLASS_MAX_LEVEL_BYTE)
					SUBCLASS_INIT_LEVEL = SUBCLASS_MAX_LEVEL_BYTE;
			ACUMULATIVE_SUBCLASS_SKILLS = Boolean.parseBoolean(customSettings.getProperty("AltSubClassSkills", "false"));
			MAX_SUBCLASS = Integer.parseInt(customSettings.getProperty("MaxSubClass", "3"));
			STARTING_AA = Integer.parseInt(customSettings.getProperty("StartingAA", "100"));
			STARTING_ADENA = Integer.parseInt(customSettings.getProperty("StartingAdena", "0"));
			SHOW_HTML_WELCOME = Boolean.parseBoolean(customSettings.getProperty("ShowHTMLWelcome", "false"));
			ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(customSettings.getProperty("AltBlacksmithUseRecipes", "true"));
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(customSettings.getProperty("AltSubClassWithoutQuests", "false"));
			ALT_RECOMMEND = Boolean.parseBoolean(customSettings.getProperty("AltRecommend", "false"));
			ALT_WEIGHT_LIMIT = Double.parseDouble(customSettings.getProperty("AltWeightLimit", "1."));
			LEVEL_ADD_LOAD = Boolean.parseBoolean(customSettings.getProperty("IncreaseWeightLimitByLevel", "false"));
			ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(customSettings.getProperty("MagicFailures", "false"));
			SHOW_SKILL_SUCCESS_CHANCE = Boolean.parseBoolean(customSettings.getProperty("ShowSuccessChance", "false"));
			SHOW_DEBUFF_ONLY = Boolean.parseBoolean(customSettings.getProperty("ShowDebuffOnly", "true"));
			ALT_PLAYER_CAN_DROP_ADENA = Boolean.parseBoolean(customSettings.getProperty("PlayerCanDropAdena", "false"));
			PLAYER_RATE_DROP_ADENA = Integer.parseInt(customSettings.getProperty("PlayerRateDropAdena", "1"));
			ALT_DISABLE_RAIDBOSS_PETRIFICATION = Boolean.parseBoolean(customSettings.getProperty("DisableRaidBossFossilization", "false"));
			MAX_LEVEL_RAID_CURSE = Integer.parseInt(customSettings.getProperty("MaxLevelRaidBossCurse", "1"));
			ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(customSettings.getProperty("AltFreeTeleporting", "false"));
			WEAR_DELAY = Integer.parseInt(customSettings.getProperty("WearDelay", "5"));
			WEAR_PRICE = Integer.parseInt(customSettings.getProperty("WearPrice", "10"));
			ALLOW_TELE_IN_SIEGE_TOWN = Boolean.parseBoolean(customSettings.getProperty("AllowTeleportInSiegeTown", "false"));
			ALT_MANA_POTIONS = Boolean.parseBoolean(customSettings.getProperty("AllowManaPotions", "false"));
			MANAHEAL_POWER = Integer.parseInt(customSettings.getProperty("ManaPotionPower", "200"));
			FORCE_UPDATE_RAIDBOSS_ON_DB = Boolean.parseBoolean(customSettings.getProperty("ForceUpdateRaidBossOnDB", "false"));
			ALLOW_NEW_CHARACTER_TITLE = Boolean.parseBoolean(customSettings.getProperty("AllowNewCharacterTitle", "false"));
			NEW_CHARACTER_TITLE = customSettings.getProperty("NewCharacterTitle", "Newbie");
			NEW_CHAR_IS_NOBLE = Boolean.parseBoolean(customSettings.getProperty("NewCharIsNoble", "false"));
			ENABLE_STARTUP_LVL = Boolean.parseBoolean(customSettings.getProperty("EnableStartupLvl", "false"));
			GUARD_ATTACK_MOBS = Boolean.parseBoolean(customSettings.getProperty("GuardsCanAttackMob","true")); 
			ADD_LVL_NEWBIE = Integer.parseInt(customSettings.getProperty("StartupLvl", "1"));
				if (ADD_LVL_NEWBIE < 1)
					ADD_LVL_NEWBIE = 1;
			ALLOW_SERVER_WELCOME_MESSAGE = Boolean.parseBoolean(customSettings.getProperty("AllowServerWelcomeMessage", "false"));
			SERVER_WELCOME_MESSAGE = customSettings.getProperty("ServerWelcomeMessage", "Welcome to the best server!");
			ALLOW_PVP_REWARD = Boolean.parseBoolean(customSettings.getProperty("AllowPvpReward", "false"));
			PVP_REWARD_ITEM_ID = Integer.parseInt(customSettings.getProperty("PvpRewardItemId", "57"));
			PVP_REWARD_ITEM_AMMOUNT = Integer.parseInt(customSettings.getProperty("PvpRewardAmmount", "100"));
			PVP_REWARD_LEVEL = Integer.parseInt(customSettings.getProperty("PvpRewardLevel", "0"));
			if (PVP_REWARD_LEVEL == 0)
				ALLOW_PVP_REWARD = false;
			ALLOW_MENU = Boolean.parseBoolean(customSettings.getProperty("AllowUserMenu","true"));
			DISABLED_COMMANDS = customSettings.getProperty("DisabledCommands","");
			ALLOW_CUSTOM_STARTER_ITEMS = Boolean.parseBoolean(customSettings.getProperty("AllowCustomStarterItems", "false"));
			ALT_ENCHANT_HP_BONUS = Boolean.parseBoolean(customSettings.getProperty("EnchantGiveHPBonus", "false"));

			if (ALLOW_CUSTOM_STARTER_ITEMS) 
			{
				CUSTOM_STARTER_ITEMS.clear();
				String[] propertySplit = customSettings.getProperty("CustomStarterItems", "0,0").split(";");
				for (String starteritems : propertySplit)
				{
					String[] starteritemsSplit = starteritems.split(",");
					if (starteritemsSplit.length != 2)
					{
						ALLOW_CUSTOM_STARTER_ITEMS = false;
						_log.info("StarterItems[Config.load()]: invalid config property -> starter items \""+ starteritems + "\"");
					}
					else
					{
					try
						{
							CUSTOM_STARTER_ITEMS.add(new int[] { Integer.valueOf(starteritemsSplit[0]), Integer.valueOf(starteritemsSplit[1]) });
						}
							catch (NumberFormatException nfe)
							{
								if (!starteritems.equals(""))
								{
									ALLOW_CUSTOM_STARTER_ITEMS = false;
									_log.info("StarterItems[Config.load()]: invalid config property -> starter items \"" + starteritems + "\"");
								}
							}
					}
				}
			}
			ALLOW_NEW_CHAR_CUSTOM_POSITION = Boolean.parseBoolean(customSettings.getProperty("AltSpawnNewChar", "false"));
			NEW_CHAR_POSITION_X = Integer.parseInt(customSettings.getProperty("AltSpawnX", "0"));
			NEW_CHAR_POSITION_Y = Integer.parseInt(customSettings.getProperty("AltSpawnY", "0"));
			NEW_CHAR_POSITION_Z = Integer.parseInt(customSettings.getProperty("AltSpawnZ", "0"));
			DEAMON_MAX_VOTES = Integer.parseInt(customSettings.getProperty("DeamonMaxVotesPerSession","20"));
			RAID_MIN_MP_TO_CAST = Integer.parseInt(customSettings.getProperty("MinBossManaToCast","300"));
                        PET_FOOD = Boolean.parseBoolean(customSettings.getProperty("petFood","true"));                        
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			EPIC_REQUIRE_QUEST = Boolean.parseBoolean(p.getProperty("QuestRequired","true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + CUSTOM_FILE + " File.");
		}
	}

	//=====================================================================================
	public static final String	CHAMPION_FILE	= "./config/mods/champion_mobs.properties";
	//=====================================================================================
	public static int			CHAMPION_FREQUENCY;
	
	public static int			CHAMPION_HP;
	public static boolean		CHAMPION_PASSIVE;
	public static String		CHAMPION_TITLE;
	public static int			CHAMPION_ADENA;
	public static int			CHAMPION_REWARDS;
	public static int			CHAMPION_EXP_SP;
	public static int			CHAMPION_MIN_LEVEL;
	public static int			CHAMPION_MAX_LEVEL;
	public static int			CHAMPION_SPCL_CHANCE;
	public static int			CHAMPION_SPCL_ITEM;
	public static int			CHAMPION_SPCL_QTY;
	public static int			CHAMPION_SPCL_LVL_DIFF;
	public static float			CHAMPION_HP_REGEN;
	public static float			CHAMPION_ATK;
	public static float			CHAMPION_SPD_ATK;
	public static boolean		CHAMPION_BOSS;
	public static boolean		CHAMPION_MINIONS;
	public static boolean		CHAMPION_ENABLE;

	//****************************************************************************
	public static void loadChampionsConfig()
	{
		_log.info("Loading: " + CHAMPION_FILE);
		try
		{
			Properties championsSettings = new L2Properties("./" + CHAMPION_FILE);

			CHAMPION_PASSIVE = Boolean.parseBoolean(championsSettings.getProperty("ChampionPassive", "false"));
			CHAMPION_TITLE = championsSettings.getProperty("ChampionTitle", "Champion").trim();
			CHAMPION_ENABLE = Boolean.parseBoolean(championsSettings.getProperty("ChampionEnable", "false"));
			CHAMPION_FREQUENCY = Integer.parseInt(championsSettings.getProperty("ChampionFrequency", "0"));
			CHAMPION_HP = Integer.parseInt(championsSettings.getProperty("ChampionHp", "7"));
			CHAMPION_HP_REGEN = Float.parseFloat(championsSettings.getProperty("ChampionHpRegen", "1."));
			CHAMPION_REWARDS = Integer.parseInt(championsSettings.getProperty("ChampionRewards", "8"));
			CHAMPION_ADENA = Integer.parseInt(championsSettings.getProperty("ChampionAdenasRewards", "1"));
			CHAMPION_ATK = Float.parseFloat(championsSettings.getProperty("ChampionAtk", "1."));
			CHAMPION_SPD_ATK = Float.parseFloat(championsSettings.getProperty("ChampionSpdAtk", "1."));
			CHAMPION_EXP_SP = Integer.parseInt(championsSettings.getProperty("ChampionExpSp", "8"));
			CHAMPION_BOSS = Boolean.parseBoolean(championsSettings.getProperty("ChampionBoss", "false"));
			CHAMPION_MIN_LEVEL = Integer.parseInt(championsSettings.getProperty("ChampionMinLevel", "20"));
			CHAMPION_MAX_LEVEL = Integer.parseInt(championsSettings.getProperty("ChampionMaxLevel", "60"));
			CHAMPION_MINIONS = Boolean.parseBoolean(championsSettings.getProperty("ChampionMinions", "false"));
			CHAMPION_SPCL_CHANCE = Integer.parseInt(championsSettings.getProperty("ChampionSpecialItemChance", "0"));
			CHAMPION_SPCL_ITEM = Integer.parseInt(championsSettings.getProperty("ChampionSpecialItemID", "6393"));
			CHAMPION_SPCL_QTY = Integer.parseInt(championsSettings.getProperty("ChampionSpecialItemAmount", "1"));
			CHAMPION_SPCL_LVL_DIFF = Integer.parseInt(championsSettings.getProperty("ChampionSpecialItemLevelDiff", "0"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + CHAMPION_FILE + " File.");

		}
	}

	//==================================================================================
	public static final String	FUN_EVENTS_FILE	= "./config/mods/fun_events.properties";
	//==================================================================================

	//StarlightFestival
	public static boolean		STAR_DROP;
	public static boolean		STAR_SPAWN;
	public static int			STAR_CHANCE1;
	public static int			STAR_CHANCE2;
	public static int			STAR_CHANCE3;
	// Medals
	public static boolean		MEDAL_DROP;
	public static boolean		MEDAL_SPAWN;
	public static int			MEDAL_CHANCE1;
	public static int			MEDAL_CHANCE2;
	// Cristmas
	public static boolean		CRISTMAS_DROP;
	public static boolean		CRISTMAS_SPAWN;
	public static int			CRISTMAS_CHANCE;
	public static int			CRISTMAS_TREE_TIME;
	//L2Day
	public static boolean		L2DAY_DROP;
	public static boolean		L2DAY_SPAWN;
	public static int			L2DAY_CHANCE;
	public static int			L2DAY_SCROLLCHANCE;
	public static int			L2DAY_ENCHSCROLLCHANCE;
	public static int			L2DAY_ACCCHANCE;
	public static String 		L2DAY_REWARD;
	public static String 		L2DAY_ACCESSORIE;
	public static String 		L2DAY_SCROLL;
	
	
	//BigSquash
	public static int			BIGSQUASH_CHANCE;
	public static boolean		BIGSQUASH_SPAWN;
	public static boolean		BIGSQUASH_DROP;
	public static boolean		BIGSQUASH_USE_SEEDS;
	//ARENA
	public static boolean		ARENA_ENABLED;
	public static int			ARENA_INTERVAL;
	public static int			ARENA_REWARD_ID;
	public static int			ARENA_REWARD_COUNT;
	//FISHERMAN
	public static boolean		FISHERMAN_ENABLED;
	public static int			FISHERMAN_INTERVAL;
	public static int			FISHERMAN_REWARD_ID;
	public static int			FISHERMAN_REWARD_COUNT;

	//*********************************************************************************************************
	public static void loadFunEventsConfig()
	{
		_log.info("Loading: " + FUN_EVENTS_FILE);
		try
		{
			Properties funEventsSettings = new L2Properties("./" + FUN_EVENTS_FILE);
			// *** MEDALS EVENT
			MEDAL_DROP = Boolean.parseBoolean(funEventsSettings.getProperty("MedalAddDrop", "false"));
			MEDAL_SPAWN = Boolean.parseBoolean(funEventsSettings.getProperty("MedalSpawnMeneger", "false"));
			MEDAL_CHANCE1 = Integer.parseInt(funEventsSettings.getProperty("Medal1DropChance", "10"));
			MEDAL_CHANCE2 = Integer.parseInt(funEventsSettings.getProperty("Medal2DropChance", "2"));
			// *** STARLIGHT FESTIVAL
			STAR_DROP = Boolean.parseBoolean(funEventsSettings.getProperty("StarAddDrop", "false"));
			STAR_SPAWN = Boolean.parseBoolean(funEventsSettings.getProperty("StarSpawnMeneger", "false"));
			STAR_CHANCE1 = Integer.parseInt(funEventsSettings.getProperty("Star1DropChance", "10"));
			STAR_CHANCE2 = Integer.parseInt(funEventsSettings.getProperty("Star2DropChance", "5"));
			STAR_CHANCE3 = Integer.parseInt(funEventsSettings.getProperty("Star3DropChance", "2"));
			// *** CRISTMAS EVENT
			CRISTMAS_DROP = Boolean.parseBoolean(funEventsSettings.getProperty("CristmasAddDrop", "false"));
			CRISTMAS_SPAWN = Boolean.parseBoolean(funEventsSettings.getProperty("CristmasSpawnSanta", "false"));
			CRISTMAS_CHANCE = Integer.parseInt(funEventsSettings.getProperty("CristmasDropChance", "5"));
			CRISTMAS_TREE_TIME = Integer.parseInt(funEventsSettings.getProperty("CristmasTreeLifeTime", "5"));
			// *** L2DAY EVENT
			L2DAY_DROP = Boolean.parseBoolean(funEventsSettings.getProperty("L2DayAddDrop", "false"));
			L2DAY_SPAWN = Boolean.parseBoolean(funEventsSettings.getProperty("L2DaySpawnManager", "false"));
			L2DAY_CHANCE = Integer.parseInt(funEventsSettings.getProperty("L2DayDropChance", "5"));
			L2DAY_SCROLLCHANCE = Integer.parseInt(funEventsSettings.getProperty("L2DayScrollChs", "300"));
			L2DAY_ENCHSCROLLCHANCE = Integer.parseInt(funEventsSettings.getProperty("L2DayEnchScrollChs", "100"));
			L2DAY_ACCCHANCE = Integer.parseInt(funEventsSettings.getProperty("L2DayAccScrollChs", "10"));
			L2DAY_REWARD = funEventsSettings.getProperty("L2DayRewards","3931,3927,3928,3929,3926,3930,3933,3932,3935,3934");
			L2DAY_ACCESSORIE =funEventsSettings.getProperty("L2DayRewardsAccessorie","6662,6660");
			L2DAY_SCROLL = funEventsSettings.getProperty("L2DayRewardsScroll","3958,3959");
			// *** BIG SQUASH EVENT
			BIGSQUASH_DROP = Boolean.parseBoolean(funEventsSettings.getProperty("BigSquashAddDrop", "false"));
			BIGSQUASH_CHANCE = Integer.parseInt(funEventsSettings.getProperty("BigSquashDropChance", "5"));
			BIGSQUASH_SPAWN = Boolean.parseBoolean(funEventsSettings.getProperty("BigSquashSpawnManager", "false"));
			BIGSQUASH_USE_SEEDS = Boolean.parseBoolean(funEventsSettings.getProperty("BigSquashUseSeeds", "true"));
			// *** ARENA MOD
			ARENA_ENABLED = Boolean.parseBoolean(funEventsSettings.getProperty("ArenaEnabled", "false"));
			ARENA_INTERVAL = Integer.parseInt(funEventsSettings.getProperty("ArenaInterval", "60"));
			ARENA_REWARD_ID = Integer.parseInt(funEventsSettings.getProperty("ArenaRewardId", "57"));
			ARENA_REWARD_COUNT = Integer.parseInt(funEventsSettings.getProperty("ArenaRewardCount", "100"));
			// *** FISHERMAN MOD
			FISHERMAN_ENABLED = Boolean.parseBoolean(funEventsSettings.getProperty("FishermanEnabled", "false"));
			FISHERMAN_INTERVAL = Integer.parseInt(funEventsSettings.getProperty("FishermanInterval", "60"));
			FISHERMAN_REWARD_ID = Integer.parseInt(funEventsSettings.getProperty("FishermanRewardId", "57"));
			FISHERMAN_REWARD_COUNT = Integer.parseInt(funEventsSettings.getProperty("FishermanRewardCount", "100"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + FUN_EVENTS_FILE + " File.");
		}
	}

	// =======================================================================================================
	public static final String	FORTSIEGE_CONFIGURATION_FILE	= "./config/main/events/fortsiege.properties";
	//========================================================================================================
	public static int			FORTSIEGE_MAX_ATTACKER;
	public static int			FORTSIEGE_FLAG_MAX_COUNT;
	public static int			FORTSIEGE_CLAN_MIN_LEVEL;
	public static int			FORTSIEGE_LENGTH_MINUTES;
	public static int			FORTSIEGE_COUNTDOWN_LENGTH;
	public static int			FORTSIEGE_MERCHANT_DELAY;
	public static int			FORTSIEGE_COMBAT_FLAG_ID;
	public static int			FORTSIEGE_REWARD_ID;
	public static int			FORTSIEGE_REWARD_COUNT;

	public static void loadFortSiegeConfig()
	{
		_log.info("Loading: " + FORTSIEGE_CONFIGURATION_FILE);
		try
		{
			Properties fortSiegeSettings = new L2Properties("./" + FORTSIEGE_CONFIGURATION_FILE);

			FORTSIEGE_MAX_ATTACKER = Integer.parseInt(fortSiegeSettings.getProperty("AttackerMaxClans", "500"));
			FORTSIEGE_FLAG_MAX_COUNT = Integer.parseInt(fortSiegeSettings.getProperty("MaxFlags", "1"));
			FORTSIEGE_CLAN_MIN_LEVEL = Integer.parseInt(fortSiegeSettings.getProperty("SiegeClanMinLevel", "4"));
			FORTSIEGE_LENGTH_MINUTES = Integer.parseInt(fortSiegeSettings.getProperty("SiegeLength", "60"));
			FORTSIEGE_COUNTDOWN_LENGTH = Integer.decode(fortSiegeSettings.getProperty("CountDownLength", "10"));
			FORTSIEGE_MERCHANT_DELAY = Integer.decode(fortSiegeSettings.getProperty("SuspiciousMerchantRespawnDelay", "180"));
			FORTSIEGE_COMBAT_FLAG_ID = Integer.parseInt(fortSiegeSettings.getProperty("CombatFlagID", "6718"));
			FORTSIEGE_REWARD_ID = Integer.parseInt(fortSiegeSettings.getProperty("RewardID","0"));
			FORTSIEGE_REWARD_COUNT = Integer.parseInt(fortSiegeSettings.getProperty("RewardCount","0"));
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new Error("Failed to Load " + FORTSIEGE_CONFIGURATION_FILE + " File.");
		}
	}

	//===============================================================================
	public static final String	OPTIONS_FILE	= "./config/main/options.properties";
	//===============================================================================
	public static boolean		AUTODELETE_INVALID_QUEST_DATA;
	public static boolean		LOG_ITEMS;
	public static String		IGNORE_LOG;
	public static boolean		FALLDOWNONDEATH;
	public static int			COORD_SYNCHRONIZE;
	public static int			MAX_DRIFT_RANGE;
	public static int			DELETE_DAYS;
	public static int			ZONE_TOWN;
	public static int			CHAR_STORE_INTERVAL;
	public static boolean		UPDATE_ITEMS_ON_CHAR_STORE;
	public static boolean		LAZY_ITEMS_UPDATE;
	public static String		FISHINGMODE;
	public static boolean		RESTORE_PLAYER_INSTANCE;
	public static boolean		ALLOW_SUMMON_TO_INSTANCE;
	public static int			MAX_PERSONAL_FAME_POINTS;
	public static int			FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	public static int			FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	public static int			CASTLE_ZONE_FAME_TASK_FREQUENCY;
	public static int			CASTLE_ZONE_FAME_AQUIRE_POINTS;
	public static boolean		USE_BOW_CROSSBOW_DISTANCE_PENALTY;
	public static double		BOW_CROSSBOW_DISTANCE_PENALTY;

	//************************************************************************************************
	public static void loadOptionsConfig()
	{
		_log.info("Loading: " + OPTIONS_FILE);
		try
		{
			Properties optionsSettings = new L2Properties("./" + OPTIONS_FILE);

			RESTORE_PLAYER_INSTANCE = Boolean.parseBoolean(optionsSettings.getProperty("RestorePlayerInstance", "false"));
			ALLOW_SUMMON_TO_INSTANCE = Boolean.parseBoolean(optionsSettings.getProperty("AllowSummonToInstance", "true"));
			CHAR_STORE_INTERVAL = Integer.parseInt(optionsSettings.getProperty("CharacterDataStoreInterval", "15"));
			UPDATE_ITEMS_ON_CHAR_STORE = Boolean.parseBoolean(optionsSettings.getProperty("UpdateItemsOnCharStore", "false"));
			LAZY_ITEMS_UPDATE = Boolean.parseBoolean(optionsSettings.getProperty("LazyItemsUpdate", "false"));
			COORD_SYNCHRONIZE = Integer.parseInt(optionsSettings.getProperty("CoordSynchronize", "-1"));
			SERVER_GMONLY = Boolean.parseBoolean(optionsSettings.getProperty("ServerGMOnly", "false"));
			
			LOG_ITEMS = Boolean.parseBoolean(optionsSettings.getProperty("LogItems", "false"));
			IGNORE_LOG = optionsSettings.getProperty("IgnoreLogItems","CONSUME").toUpperCase();
			AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(optionsSettings.getProperty("AutoDeleteInvalidQuestData", "false"));
			SERVER_LIST_BRACKET = Boolean.parseBoolean(optionsSettings.getProperty("ServerListBrackets", "false"));
			SERVER_LIST_CLOCK = Boolean.parseBoolean(optionsSettings.getProperty("ServerListClock", "false"));
			FISHINGMODE = optionsSettings.getProperty("FishingMode", "water");
			ZONE_TOWN = Integer.parseInt(optionsSettings.getProperty("ZoneTown", "0"));
			DELETE_DAYS = Integer.parseInt(optionsSettings.getProperty("DeleteCharAfterDays", "7"));
			FALLDOWNONDEATH = Boolean.parseBoolean(optionsSettings.getProperty("FallDownOnDeath", "true"));
			MAX_PERSONAL_FAME_POINTS = Integer.parseInt(optionsSettings.getProperty("MaxPersonalFamePoints","65535"));
			FORTRESS_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(optionsSettings.getProperty("FortressZoneFameTaskFrequency","300"));
			FORTRESS_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(optionsSettings.getProperty("FortressZoneFameAquirePoints","31"));
			CASTLE_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(optionsSettings.getProperty("CastleZoneFameTaskFrequency","300"));
			CASTLE_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(optionsSettings.getProperty("CastleZoneFameAquirePoints","125"));
			USE_BOW_CROSSBOW_DISTANCE_PENALTY = Boolean.parseBoolean(optionsSettings.getProperty("UseBowDistancePenalty", "true"));
			BOW_CROSSBOW_DISTANCE_PENALTY = Double.parseDouble(optionsSettings.getProperty("MaxBowDistancePenalty", "0.6"));
			if (BOW_CROSSBOW_DISTANCE_PENALTY>1)
				BOW_CROSSBOW_DISTANCE_PENALTY=0.6;
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + OPTIONS_FILE + " File.");
		}
	}

	//==================================================================================
	public static final String	ID_FACTORY_FILE	= "./config/main/id_factory.properties";
	//==================================================================================
	public static enum IdFactoryType
	{
		Compaction, BitSet, Stack, Increment, Rebuild
	}

	public static enum ObjectMapType
	{
		L2ObjectHashMap, WorldObjectMap
	}

	public static enum ObjectSetType
	{
		L2ObjectHashSet, WorldObjectSet
	}

	public static IdFactoryType	IDFACTORY_TYPE;
	public static boolean		BAD_ID_CHECKING;
	public static boolean		ID_FACTORY_CLEANUP;

	//*************************************************************************************
	public static void loadIdFactoryConfig()
	{
		_log.info("Loading: " + ID_FACTORY_FILE);
		try
		{
			Properties idFactorySettings = new L2Properties("./" + ID_FACTORY_FILE);

			IDFACTORY_TYPE = IdFactoryType.valueOf(idFactorySettings.getProperty("IDFactory", "BitSet"));
			BAD_ID_CHECKING = Boolean.parseBoolean(idFactorySettings.getProperty("BadIdChecking", "true"));
			ID_FACTORY_CLEANUP = Boolean.parseBoolean(idFactorySettings.getProperty("CleanBadIDs","true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + ID_FACTORY_FILE + " File.");
		}
	}

	//=================================================================================
	public static final String	PVTSTORE_FILE	= "./config/main/pvtstores.properties";
	//=================================================================================
	public static int			MAX_PVTSTORESELL_SLOTS_DWARF;
	public static int			MAX_PVTSTORESELL_SLOTS_OTHER;
	public static int			MAX_PVTSTOREBUY_SLOTS_DWARF;
	public static int			MAX_PVTSTOREBUY_SLOTS_OTHER;
	public static boolean		CHECK_ZONE_ON_PVT;

	//	**************************************************************************************
	public static void loadPvtStoresConfig()
	{
		_log.info("Loading: " + PVTSTORE_FILE);
		try
		{
			Properties pvtStoresSettings = new L2Properties("./" + PVTSTORE_FILE);

			MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(pvtStoresSettings.getProperty("MaxPvtStoreSellSlotsDwarf", "4"));
			MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(pvtStoresSettings.getProperty("MaxPvtStoreSellSlotsOther", "3"));
			MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(pvtStoresSettings.getProperty("MaxPvtStoreBuySlotsDwarf", "5"));
			MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(pvtStoresSettings.getProperty("MaxPvtStoreBuySlotsOther", "4"));
			CHECK_ZONE_ON_PVT = Boolean.parseBoolean(pvtStoresSettings.getProperty("CheckZoneOnPvt", "false"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + PVTSTORE_FILE + " File.");
		}
	}

	//=================================================================================
	public static final String	WAREHOUSE_FILE	= "./config/main/warehouse.properties";
	//=================================================================================
	public static int			WAREHOUSE_SLOTS_NO_DWARF;
	public static int			WAREHOUSE_SLOTS_DWARF;
	public static int			WAREHOUSE_SLOTS_CLAN;
	public static int			FREIGHT_SLOTS;
	public static int			ALT_GAME_FREIGHT_PRICE;
	public static boolean		ALT_GAME_FREIGHTS;
	public static boolean		ENABLE_WAREHOUSESORTING_CLAN;
	public static boolean		ENABLE_WAREHOUSESORTING_PRIVATE;
	public static boolean		ENABLE_WAREHOUSESORTING_FREIGHT;
	public static boolean		ALLOW_WAREHOUSE;
	public static boolean		ALLOW_FREIGHT;
	public static boolean		WAREHOUSE_CACHE;
	public static int			WAREHOUSE_CACHE_TIME;

	//*******************************************************************************************
	public static void loadWhConfig()
	{
		_log.info("Loading: " + WAREHOUSE_FILE);
		try
		{
			Properties whSettings = new L2Properties("./" + WAREHOUSE_FILE);

			ALLOW_WAREHOUSE = Boolean.parseBoolean(whSettings.getProperty("AllowWarehouse", "true"));
			ALLOW_FREIGHT = Boolean.parseBoolean(whSettings.getProperty("AllowFreight", "true"));
			ENABLE_WAREHOUSESORTING_CLAN = Boolean.parseBoolean(whSettings.getProperty("EnableWarehouseSortingClan", "false"));
			ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.parseBoolean(whSettings.getProperty("EnableWarehouseSortingPrivate", "false"));
			ENABLE_WAREHOUSESORTING_FREIGHT = Boolean.parseBoolean(whSettings.getProperty("EnableWarehouseSortingFreight", "false"));
			ALT_GAME_FREIGHTS = Boolean.parseBoolean(whSettings.getProperty("AltGameFreights", "false"));
			ALT_GAME_FREIGHT_PRICE = Integer.parseInt(whSettings.getProperty("AltGameFreightPrice", "1000"));
			WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(whSettings.getProperty("MaxWarehouseSlotsForOther", "100"));
			WAREHOUSE_SLOTS_DWARF = Integer.parseInt(whSettings.getProperty("MaxWarehouseSlotsForDwarf", "120"));
			WAREHOUSE_SLOTS_CLAN = Integer.parseInt(whSettings.getProperty("MaxWarehouseSlotsForClan", "150"));
			FREIGHT_SLOTS = Integer.parseInt(whSettings.getProperty("MaxWarehouseFreightSlots", "100"));
			WAREHOUSE_CACHE = Boolean.parseBoolean(whSettings.getProperty("WarehouseCache", "false"));
			WAREHOUSE_CACHE_TIME = Integer.parseInt(whSettings.getProperty("WarehouseCacheTime", "15"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + WAREHOUSE_FILE + " File.");
		}
	}

	//==============================================================================================
	public static final String		OTHER_FILE			= "./config/main/other_settings.properties";
	//==============================================================================================
	public static int				PLAYER_SPAWN_PROTECTION;
	public static int				PLAYER_FAKEDEATH_UP_PROTECTION;
	public static int				UNSTUCK_INTERVAL;
	public static int				DEATH_PENALTY_CHANCE;
	public static String			PET_RENT_NPC;
	public static FastList<Integer>	LIST_PET_RENT_NPC	= new FastList<Integer>();
	public static boolean			CLASSIC_BUFF_MODE;
	public static boolean			ALLOW_KEYBOARD_MOVEMENT;
	public static boolean			CHECK_PLAYER_MACRO;
	public static FastList<String>	LIST_MACRO_RESTRICTED_WORDS	= new FastList<String>();
	public static boolean			CONSUME_SPIRIT_SOUL_SHOTS;


	//******************************************************************************************************
	public static void loadOtherConfig()
	{
		_log.info("Loading: " + OTHER_FILE);
		try
		{
			Properties otherSettings = new L2Properties("./" + OTHER_FILE);

			PET_RENT_NPC = otherSettings.getProperty("ListPetRentNpc", "30827");
			LIST_PET_RENT_NPC = new FastList<Integer>();

			for (String id : PET_RENT_NPC.split(","))
				LIST_PET_RENT_NPC.add(Integer.parseInt(id));

			UNSTUCK_INTERVAL = Integer.parseInt(otherSettings.getProperty("PlayerUnstuckInterval", "350"));
			PLAYER_SPAWN_PROTECTION = Integer.parseInt(otherSettings.getProperty("PlayerSpawnProtection", "5"));
			PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(otherSettings.getProperty("PlayerFakeDeathUpProtection", "0"));
			DEATH_PENALTY_CHANCE = Integer.parseInt(otherSettings.getProperty("DeathPenaltyChance", "20"));
			CLASSIC_BUFF_MODE = Boolean.parseBoolean(otherSettings.getProperty("ClassicGetBuffMode", "true"));
			ALLOW_KEYBOARD_MOVEMENT = Boolean.parseBoolean(otherSettings.getProperty("AllowKeyboardMovement", "true"));

			CHECK_PLAYER_MACRO = Boolean.parseBoolean(otherSettings.getProperty("CheckPlayerMacro", "true"));
			LIST_MACRO_RESTRICTED_WORDS = new FastList<String>();
			for (String command : otherSettings.getProperty("MacroRestrictedCommandList","exit").split(","))
				LIST_MACRO_RESTRICTED_WORDS.add(command);

			CONSUME_SPIRIT_SOUL_SHOTS = Boolean.parseBoolean(otherSettings.getProperty("ConsumeSSShot", "true"));

		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + OTHER_FILE + " File.");
		}
	}

	//================================================================================
	public static final String	RESPAWN_FILE	= "./config/main/respawns.properties";
	//================================================================================
	public static double		RAID_MINION_RESPAWN_TIMER;
	public static double		RESPAWN_RESTORE_CP;
	public static double		RESPAWN_RESTORE_HP;
	public static double		RESPAWN_RESTORE_MP;
	public static int			RESPAWN_RANDOM_MAX_OFFSET;
	public static float			RAID_MIN_RESPAWN_MULTIPLIER;
	public static float			RAID_MAX_RESPAWN_MULTIPLIER;
	public static boolean		RESPAWN_RANDOM_ENABLED;
	public static int			ALT_DEFAULT_RESTARTTOWN;
	public static boolean		MON_RESPAWN_RANDOM_ENABLED;
	public static int			MON_RESPAWN_RANDOM_ZONE;
	
	//****************************************************************************************************
	public static void loadRespawnsConfig()
	{
		_log.info("Loading: " + RESPAWN_FILE);
		try
		{
			Properties respawnSettings = new L2Properties("./" + RESPAWN_FILE);

			ALT_DEFAULT_RESTARTTOWN = Integer.parseInt(respawnSettings.getProperty("AltDefaultRestartTown", "0"));
			RESPAWN_RANDOM_MAX_OFFSET = Integer.parseInt(respawnSettings.getProperty("RespawnRandomMaxOffset", "50"));
			RAID_MINION_RESPAWN_TIMER = Integer.parseInt(respawnSettings.getProperty("RaidMinionRespawnTime", "300000"));
			RAID_MIN_RESPAWN_MULTIPLIER = Float.parseFloat(respawnSettings.getProperty("RaidMinRespawnMultiplier", "1.0"));
			RAID_MAX_RESPAWN_MULTIPLIER = Float.parseFloat(respawnSettings.getProperty("RaidMaxRespawnMultiplier", "1.0"));
			RESPAWN_RANDOM_ENABLED = Boolean.parseBoolean(respawnSettings.getProperty("RespawnRandomInTown", "false"));
			RESPAWN_RESTORE_CP = Double.parseDouble(respawnSettings.getProperty("RespawnRestoreCP", "0")) / 100;
			RESPAWN_RESTORE_HP = Double.parseDouble(respawnSettings.getProperty("RespawnRestoreHP", "70")) / 100;
			RESPAWN_RESTORE_MP = Double.parseDouble(respawnSettings.getProperty("RespawnRestoreMP", "70")) / 100;
			MON_RESPAWN_RANDOM_ENABLED = Boolean.parseBoolean(respawnSettings.getProperty("UseMonsterRndSpawn", "true"));
			MON_RESPAWN_RANDOM_ZONE = Integer.parseInt(respawnSettings.getProperty("RndSpawnZone", "300"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + RESPAWN_FILE + " File.");
		}
	}

	//=================================================================================
	public static final String	PETITION_FILE	= "./config/main/petitions.properties";
	//=================================================================================
	public static int			MAX_PETITIONS_PER_PLAYER;
	public static int			MAX_PETITIONS_PENDING;
	public static boolean		PETITIONING_ALLOWED;
	public static boolean		PETITION_NEED_GM_ONLINE;
	public static boolean		SEND_PAGE_ON_PETTITON;

	//******************************************************************************************************
	public static void loadPetitionSettings()
	{
		_log.info("Loading: " + PETITION_FILE);
		try
		{
			Properties petitionSettings = new L2Properties("./" + PETITION_FILE);

			SEND_PAGE_ON_PETTITON = Boolean.parseBoolean(petitionSettings.getProperty("SendPageOnPetition", "false"));
			PETITIONING_ALLOWED = Boolean.parseBoolean(petitionSettings.getProperty("PetitioningAllowed", "true"));
			MAX_PETITIONS_PER_PLAYER = Integer.parseInt(petitionSettings.getProperty("MaxPetitionsPerPlayer", "5"));
			MAX_PETITIONS_PENDING = Integer.parseInt(petitionSettings.getProperty("MaxPetitionsPending", "25"));
			PETITION_NEED_GM_ONLINE = Boolean.parseBoolean(petitionSettings.getProperty("PetitioningNeedGmOnline", "true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + PETITION_FILE + " File.");
		}
	}

	//================================================================================
	public static final String	REGEN_FILE	= "./config/main/regeneration.properties";
	//================================================================================
	public static double		NPC_HP_REGEN_MULTIPLIER;
	public static double		NPC_MP_REGEN_MULTIPLIER;
	public static double		PLAYER_CP_REGEN_MULTIPLIER;
	public static double		PLAYER_HP_REGEN_MULTIPLIER;
	public static double		PLAYER_MP_REGEN_MULTIPLIER;
	public static double		RAID_HP_REGEN_MULTIPLIER;
	public static double		RAID_MP_REGEN_MULTIPLIER;
	public static double		RAID_PDEFENCE_MULTIPLIER;
	public static double		RAID_MDEFENCE_MULTIPLIER;
	public static double		PET_HP_REGEN_MULTIPLIER;
	public static double		PET_MP_REGEN_MULTIPLIER;
	//**************************************************************************************************
	public static void loadRegenSettings()
	{
		_log.info("Loading: " + REGEN_FILE);
		try
		{
			Properties regenSettings = new L2Properties("./" + REGEN_FILE);

			NPC_HP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("NPCHpRegenMultiplier", "100")) / 100;
			NPC_MP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("NPCMpRegenMultiplier", "100")) / 100;
			PLAYER_CP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("PlayerCpRegenMultiplier", "100")) / 100;
			PLAYER_HP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("PlayerHpRegenMultiplier", "100")) / 100;
			PLAYER_MP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("PlayerMpRegenMultiplier", "100")) / 100;
			RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("RaidHpRegenMultiplier", "100")) / 100;
			RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("RaidMpRegenMultiplier", "100")) / 100;
			RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("RaidPDefenceMultiplier", "100")) / 100;
			RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("RaidMDefenceMultiplier", "100")) / 100;
			PET_HP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("PetHpRegenMultiplier", "100")) / 100;
			PET_MP_REGEN_MULTIPLIER = Double.parseDouble(regenSettings.getProperty("PetMpRegenMultiplier", "100")) / 100;
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + REGEN_FILE + " File.");
		}
	}

	//===============================================================================
	public static final String	ENCHANT_FILE	= "./config/main/enchant.properties";
	//===============================================================================
	public static FastMap<Integer, Integer> NORMAL_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> BLESS_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> CRYTAL_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();

	public static FastMap<Integer, Integer> NORMAL_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> BLESS_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> CRYSTAL_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();

	public static FastMap<Integer, Integer> NORMAL_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> BLESS_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> CRYSTAL_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	
	public static boolean		ALLOW_CRYSTAL_SCROLL;
	public static int			ENCHANT_MAX_WEAPON;
	public static int			ENCHANT_MAX_ARMOR;
	public static int			ENCHANT_MAX_JEWELRY;
	public static int			ENCHANT_SAFE_MAX;
	public static int			ENCHANT_SAFE_MAX_FULL;
	public static int			ENCHANT_DWARF_1_ENCHANTLEVEL;
	public static int			ENCHANT_DWARF_2_ENCHANTLEVEL;
	public static int			ENCHANT_DWARF_3_ENCHANTLEVEL;
	public static int			ENCHANT_DWARF_1_CHANCE;
	public static int			ENCHANT_DWARF_2_CHANCE;
	public static int			ENCHANT_DWARF_3_CHANCE;
	public static boolean		ENCHANT_DWARF_SYSTEM;
	public static boolean		CHECK_ENCHANT_LEVEL_EQUIP;
	public static boolean		AUGMENT_EXCLUDE_NOTDONE;
	public static int			AUGMENTATION_NG_SKILL_CHANCE;
	public static int			AUGMENTATION_NG_GLOW_CHANCE;
	public static int			AUGMENTATION_MID_SKILL_CHANCE;
	public static int			AUGMENTATION_MID_GLOW_CHANCE;
	public static int			AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int			AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int			AUGMENTATION_TOP_SKILL_CHANCE;
	public static int			AUGMENTATION_TOP_GLOW_CHANCE;
	public static int			AUGMENTATION_BASESTAT_CHANCE;
	public static boolean		ALT_FAILED_ENC_LEVEL;
	public static boolean		ENCHANT_LIMIT_AURA_SELF;
	public static boolean		ENCHANT_LIMIT_AURA_OTHER;
	public static int			ENCHANT_LIMIT_AURA_LEVEL;

	public static boolean 			ENCHANT_FOR_MAGIC_WEAPON;
	public static float 		ENCHANT_MAGIC_WEAPON_CHANCE;
	public static int			ENCHANT_SAFE_MAX_MAGIC_WEAPON;

	//********************************************************************************************
	public static void loadEnchantConfig()
	{
		_log.info("Loading: " + ENCHANT_FILE);
		try
		{
			Properties enchantSettings = new L2Properties("./" + ENCHANT_FILE);

			ENCHANT_MAGIC_WEAPON_CHANCE = Float.parseFloat(enchantSettings.getProperty("EnchantMagicWeaponChance", "1.0"));
			ENCHANT_SAFE_MAX_MAGIC_WEAPON = Integer.parseInt(enchantSettings.getProperty("SaveMaxMagicWeapon", "7"));
			ENCHANT_FOR_MAGIC_WEAPON = Boolean.parseBoolean(enchantSettings.getProperty("EnchantMagicWeaponEnabled", "false"));

			String[] propertySplit = enchantSettings.getProperty("NormalWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						NORMAL_WEAPON_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("BlessWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						BLESS_WEAPON_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("CrystalWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						CRYTAL_WEAPON_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			////

			propertySplit = enchantSettings.getProperty("NormalArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						NORMAL_ARMOR_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("BlessArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						BLESS_ARMOR_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("CrystalArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						CRYSTAL_ARMOR_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			////

			propertySplit = enchantSettings.getProperty("NormalJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						NORMAL_JEWELRY_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("BlessJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						BLESS_JEWELRY_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			propertySplit = enchantSettings.getProperty("CrystalJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					_log.info("invalid config property");
				}
				else
				{
					try
					{
						CRYSTAL_JEWELRY_ENCHANT_LEVEL.put(Integer.parseInt(writeData[0]), Integer.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							_log.info("invalid config property");
						}
					}
				}
			}

			ALLOW_CRYSTAL_SCROLL = Boolean.parseBoolean(enchantSettings.getProperty("AllowCrystalScroll", "false"));
			ENCHANT_MAX_WEAPON = Integer.parseInt(enchantSettings.getProperty("EnchantMaxWeapon", "25"));
			ENCHANT_MAX_ARMOR = Integer.parseInt(enchantSettings.getProperty("EnchantMaxArmor", "25"));
			ENCHANT_MAX_JEWELRY = Integer.parseInt(enchantSettings.getProperty("EnchantMaxJewelry", "25"));
			ENCHANT_SAFE_MAX = Integer.parseInt(enchantSettings.getProperty("EnchantSafeMax", "3"));
			ENCHANT_SAFE_MAX_FULL = Integer.parseInt(enchantSettings.getProperty("EnchantSafeMaxFull", "4"));
			ENCHANT_DWARF_SYSTEM = Boolean.parseBoolean(enchantSettings.getProperty("EnchantDwarfSystem", "false"));
			ENCHANT_DWARF_1_ENCHANTLEVEL = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf1Enchantlevel", "8"));
			ENCHANT_DWARF_2_ENCHANTLEVEL = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf2Enchantlevel", "10"));
			ENCHANT_DWARF_3_ENCHANTLEVEL = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf3Enchantlevel", "12"));
			ENCHANT_DWARF_1_CHANCE = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf1Chance", "15"));
			ENCHANT_DWARF_2_CHANCE = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf2Chance", "15"));
			ENCHANT_DWARF_3_CHANCE = Integer.parseInt(enchantSettings.getProperty("EnchantDwarf3Chance", "15"));
			AUGMENT_EXCLUDE_NOTDONE = Boolean.parseBoolean(enchantSettings.getProperty("AugmentExcludeNotdone", "false"));
			AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationNGSkillChance", "15"));
			AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationNGGlowChance", "0"));
			AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationMidSkillChance", "30"));
			AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationMidGlowChance", "40"));
			AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationHighSkillChance", "45"));
			AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationHighGlowChance", "70"));
			AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationTopSkillChance", "60"));
			AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationTopGlowChance", "100"));
			AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(enchantSettings.getProperty("AugmentationBaseStatChance", "1"));
			CHECK_ENCHANT_LEVEL_EQUIP = Boolean.parseBoolean(enchantSettings.getProperty("CheckEnchantLevelEquip", "true"));
			ALT_FAILED_ENC_LEVEL = Boolean.parseBoolean(enchantSettings.getProperty("AltEncLvlAfterFail", "false"));
			ENCHANT_LIMIT_AURA_SELF = Boolean.parseBoolean(enchantSettings.getProperty("EnchantLimitAuraSelf", "false"));
			ENCHANT_LIMIT_AURA_OTHER = Boolean.parseBoolean(enchantSettings.getProperty("EnchantLimitAuraOther", "false"));
			ENCHANT_LIMIT_AURA_LEVEL = Integer.parseInt(enchantSettings.getProperty("EnchantLimitAuraLevel", "15"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + ENCHANT_FILE + " File.");
		}
	}
	//=========================================================================
	public static final String	SERVICES_FILE	= "./config/main/services.properties";
	//=========================================================================
	public static boolean		USE_PREMIUMSERVICE;
	public static float			PREMIUM_RATE_XP;
	public static float			PREMIUM_RATE_SP;
	public static float			PREMIUM_RATE_DROP_ADENA;
	public static float			PREMIUM_RATE_DROP_SPOIL;
	public static float			PREMIUM_RATE_DROP_ITEMS;
	public static float			PREMIUM_PET_XP_RATE;
	public static float			PREMIUM_SINEATER_XP_RATE;
	public static float			PREMIUM_RATE_DROP_QUEST;
	public static boolean		SHOW_PREMIUM_STATE_ON_ENTER;
	public static Map<Integer, Integer> PREMIUM_SKILLS = new FastMap<Integer, Integer>();

	public static boolean		ALLOW_OFFLINE_TRADE;
	public static boolean		ALLOW_OFFLINE_TRADE_CRAFT;
	public static boolean		ALLOW_OFFLINE_TRADE_COLOR_NAME;
	public static int			OFFLINE_TRADE_COLOR_NAME;
	public static boolean		ALLOW_OFFLINE_TRADE_PROTECTION;
	public static int			OFFLINE_TRADE_PRICE_ITEM_ID;
	public static int			OFFLINE_TRADE_PRICE_ITEM_ID_TIME;	
	public static int			OFFLINE_TRADE_PRICE_COUNT;
	public static int			OFFLINE_CRAFT_PRICE_ITEM_ID;
	public static int			OFFLINE_CRAFT_PRICE_ITEM_ID_TIME;	
	public static int			OFFLINE_CRAFT_PRICE_COUNT;
	public static boolean		RESTORE_OFFLINE_TRADERS;
	public static int			ALLOW_OFFLINE_HOUR;


	public static boolean		ALLOW_MAMMON_SEARCH;
	public static boolean		ALLOW_READ_RULES;
	public static boolean		ALLOW_USE_EXP_SET;

	public static boolean		ALT_ALLOW_AWAY_STATUS;
	public static int			ALT_AWAY_TIMER;
	public static int			ALT_BACK_TIMER;
	public static int			ALT_AWAY_TITLE_COLOR;
	public static boolean		ALT_AWAY_ALLOW_INTERFERENCE;
	public static boolean		ALT_AWAY_PLAYER_TAKE_AGGRO;
	public static boolean		ALT_AWAY_PEACE_ZONE;

	public static boolean		ENABLE_EVENT_MANAGER;
	public static int			EVENT_MANAGER_ID;
	public static boolean		SPAWN_EVENT_MANAGER;

	public static boolean		ENABLE_RESTART;
	public static String		RESTART_TIME;
	public static String		RESTART_WARN_TIME;
	
	

	public static void loadServicesConfig()
	{
		_log.info("Loading: " + SERVICES_FILE);
		try
		{
			Properties servicesSettings = new L2Properties("./" + SERVICES_FILE);
			
			USE_PREMIUMSERVICE = Boolean.parseBoolean(servicesSettings.getProperty("UsePremiumServices", "false"));
			PREMIUM_RATE_XP = Float.parseFloat(servicesSettings.getProperty("RateXp", "1."));
			PREMIUM_RATE_SP = Float.parseFloat(servicesSettings.getProperty("RateSp", "1."));
			PREMIUM_RATE_DROP_ADENA = Float.parseFloat(servicesSettings.getProperty("RateDropAdena", "1."));
			PREMIUM_RATE_DROP_SPOIL = Float.parseFloat(servicesSettings.getProperty("RateDropSpoil", "1."));
			PREMIUM_RATE_DROP_ITEMS = Float.parseFloat(servicesSettings.getProperty("RateDropItems", "1."));
			PREMIUM_RATE_DROP_QUEST = Float.parseFloat(servicesSettings.getProperty("RateDropQuest", "1."));
			SHOW_PREMIUM_STATE_ON_ENTER = Boolean.parseBoolean(servicesSettings.getProperty("ShowPAStatusOnEnter", "true"));
			PREMIUM_SKILLS.clear();
			for(String s : servicesSettings.getProperty("PremiumSkills","").split(";")) try { 
				String sk[] = s.split(":");
				PREMIUM_SKILLS.put(Integer.parseInt(sk[0]), Integer.parseInt(sk[1]));
			} catch(Exception e) {
			}
			
			ALLOW_OFFLINE_TRADE = Boolean.parseBoolean(servicesSettings.getProperty("AllowOfflineTrade", "false"));
			ALLOW_OFFLINE_TRADE_CRAFT = Boolean.parseBoolean(servicesSettings.getProperty("AllowOfflineTradeCraft", "false"));
			ALLOW_OFFLINE_TRADE_COLOR_NAME = Boolean.parseBoolean(servicesSettings.getProperty("AllowOfflineTradeColorName", "false"));
			OFFLINE_TRADE_COLOR_NAME = Integer.parseInt(servicesSettings.getProperty("OfflineTradeColorName", "999999"));
			ALLOW_OFFLINE_TRADE_PROTECTION = Boolean.parseBoolean(servicesSettings.getProperty("AllowOfflineTradeProtection", "false"));
			
			OFFLINE_TRADE_PRICE_ITEM_ID = Integer.parseInt(servicesSettings.getProperty("OfflineTradePriceID", "57"));
			
			OFFLINE_TRADE_PRICE_ITEM_ID_TIME = Integer.parseInt(servicesSettings.getProperty("OfflineTradePriceIDTime", "1"));
			OFFLINE_TRADE_PRICE_COUNT = Integer.parseInt(servicesSettings.getProperty("OfflineTradePriceCount", "500000"));
			OFFLINE_CRAFT_PRICE_ITEM_ID = Integer.parseInt(servicesSettings.getProperty("OfflineCraftPriceID", "57"));
			
			OFFLINE_CRAFT_PRICE_ITEM_ID_TIME = Integer.parseInt(servicesSettings.getProperty("OfflineCraftPriceIDTime", "1"));
			OFFLINE_CRAFT_PRICE_COUNT = Integer.parseInt(servicesSettings.getProperty("OfflineCraftPriceCount", "500000"));
			RESTORE_OFFLINE_TRADERS = Boolean.parseBoolean(servicesSettings.getProperty("RestoreOfflineTraders", "true"));
			ALLOW_OFFLINE_HOUR = Integer.parseInt(servicesSettings.getProperty("AllowOfflineHour", "72"));

			if (!ALLOW_OFFLINE_TRADE)
				RESTORE_OFFLINE_TRADERS = false;

			
			ALLOW_MAMMON_SEARCH = Boolean.parseBoolean(servicesSettings.getProperty("AllowSearchMammon", "false"));
			ALLOW_READ_RULES = Boolean.parseBoolean(servicesSettings.getProperty("AllowReadRules", "false"));
			ALLOW_USE_EXP_SET = Boolean.parseBoolean(servicesSettings.getProperty("AllowUseExpSet", "false"));

			ALT_ALLOW_AWAY_STATUS = Boolean.parseBoolean(servicesSettings.getProperty("AllowAwayStatus", "false"));
			ALT_AWAY_ALLOW_INTERFERENCE = Boolean.parseBoolean(servicesSettings.getProperty("AwayAllowInterference", "false"));
			ALT_AWAY_PLAYER_TAKE_AGGRO = Boolean.parseBoolean(servicesSettings.getProperty("AwayPlayerTakeAggro", "false"));
			ALT_AWAY_TITLE_COLOR = Integer.decode("0x" + servicesSettings.getProperty("AwayTitleColor", "0000FF"));
			ALT_AWAY_TIMER = Integer.parseInt(servicesSettings.getProperty("AwayTimer", "30"));
			ALT_BACK_TIMER = Integer.parseInt(servicesSettings.getProperty("BackTimer", "30"));
			ALT_AWAY_PEACE_ZONE = Boolean.parseBoolean(servicesSettings.getProperty("AwayOnlyInPeaceZone", "false"));

			ENABLE_EVENT_MANAGER = Boolean.parseBoolean(servicesSettings.getProperty("EnableEventManager", "false"));
			EVENT_MANAGER_ID = Integer.parseInt(servicesSettings.getProperty("EventManagerNpcId", "50004"));
			SPAWN_EVENT_MANAGER = Boolean.parseBoolean(servicesSettings.getProperty("EnableAutoSpawn", "false"));

			ENABLE_RESTART = Boolean.parseBoolean(servicesSettings.getProperty("EnableRestart", "false"));
			RESTART_TIME = servicesSettings.getProperty("RestartTime", "06:20:00");
			RESTART_WARN_TIME = servicesSettings.getProperty("RestartWarnTime", "600");
			
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + SERVICES_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	RATES_FILE	= "./config/main/rates.properties";
	//=========================================================================
	public static int			KARMA_DROP_LIMIT;
	public static int			KARMA_RATE_DROP;
	public static int			KARMA_RATE_DROP_ITEM;
	public static int			KARMA_RATE_DROP_EQUIP;
	public static int			KARMA_RATE_DROP_EQUIP_WEAPON;
	public static float			RATE_DROP_MANOR;
	public static float			RATE_XP;
	public static float			RATE_SP;
	public static float			RATE_PARTY_XP;
	public static float			RATE_PARTY_SP;
	public static float			RATE_QUESTS_REWARD_EXPSP;
	public static float			RATE_QUESTS_REWARD_ADENA;
	public static float			RATE_QUESTS_REWARD_ITEMS;
	public static float			RATE_RUN_SPEED;
	public static float			RATE_DROP_ADENA;
	public static float			RATE_CONSUMABLE_COST;
	public static float			RATE_CRAFT_COST;
	public static float			RATE_DROP_ITEMS;
	public static float			RATE_DROP_SPOIL;
	public static float			RATE_DROP_QUEST;
	public static int			RATE_EXTR_FISH;
	public static float			RATE_KARMA_EXP_LOST;
	public static float			RATE_SIEGE_GUARDS_PRICE;
	public static float			RATE_DROP_COMMON_HERBS;
	public static float			RATE_DROP_MP_HP_HERBS;
	public static float			RATE_DROP_GREATER_HERBS;
	public static float			RATE_DROP_SUPERIOR_HERBS;
	public static float			RATE_DROP_SPECIAL_HERBS;
	public static float			PET_XP_RATE;
	public static float			PET_FOOD_RATE;
	public static float			SINEATER_XP_RATE;
	public static float			RATE_DROP_ITEMS_BY_RAID;
	public static float			RATE_DROP_ITEMS_BY_GRAND;

	//**************************************************************************************************
	public static void loadRatesConfig()
	{
		_log.info("Loading: " + RATES_FILE);
		try
		{
			Properties ratesSettings = new L2Properties("./" + RATES_FILE);

			SINEATER_XP_RATE = Float.parseFloat(ratesSettings.getProperty("SinEaterXpRate", "1."));
			RATE_XP = Float.parseFloat(ratesSettings.getProperty("RateXp", "1."));
			RATE_SP = Float.parseFloat(ratesSettings.getProperty("RateSp", "1."));
			RATE_PARTY_XP = Float.parseFloat(ratesSettings.getProperty("RatePartyXp", "1."));
			RATE_PARTY_SP = Float.parseFloat(ratesSettings.getProperty("RatePartySp", "1."));
			RATE_QUESTS_REWARD_EXPSP = Float.parseFloat(ratesSettings.getProperty("RateQuestsRewardExpSp", "1."));
			RATE_QUESTS_REWARD_ADENA = Float.parseFloat(ratesSettings.getProperty("RateQuestsRewardAdena", "1."));
			RATE_QUESTS_REWARD_ITEMS = Float.parseFloat(ratesSettings.getProperty("RateQuestsRewardItems", "1."));
			RATE_DROP_ADENA = Float.parseFloat(ratesSettings.getProperty("RateDropAdena", "1."));
			RATE_CONSUMABLE_COST = Float.parseFloat(ratesSettings.getProperty("RateConsumableCost", "1."));
			RATE_CRAFT_COST = Float.parseFloat(ratesSettings.getProperty("RateCraftCost", "1."));
			RATE_DROP_ITEMS = Float.parseFloat(ratesSettings.getProperty("RateDropItems", "1."));
			RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(ratesSettings.getProperty("RateRaidDropItems", "1."));
			RATE_DROP_ITEMS_BY_GRAND = Float.parseFloat(ratesSettings.getProperty("RateGrandDropItems", "1."));
			RATE_DROP_SPOIL = Float.parseFloat(ratesSettings.getProperty("RateDropSpoil", "1."));
			RATE_DROP_QUEST = Float.parseFloat(ratesSettings.getProperty("RateDropQuest", "1."));
			RATE_EXTR_FISH = Integer.parseInt(ratesSettings.getProperty("RateExtractFish", "1"));
			RATE_KARMA_EXP_LOST = Float.parseFloat(ratesSettings.getProperty("RateKarmaExpLost", "1."));
			RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(ratesSettings.getProperty("RateSiegeGuardsPrice", "1."));
			RATE_DROP_COMMON_HERBS = Float.parseFloat(ratesSettings.getProperty("RateCommonHerbs", "15."));
			RATE_DROP_MP_HP_HERBS = Float.parseFloat(ratesSettings.getProperty("RateHpMpHerbs", "10."));
			RATE_DROP_GREATER_HERBS = Float.parseFloat(ratesSettings.getProperty("RateGreaterHerbs", "4."));
			RATE_DROP_SUPERIOR_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSuperiorHerbs", "0.8")) * 10;
			RATE_DROP_SPECIAL_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSpecialHerbs", "0.2")) * 10;
			RATE_RUN_SPEED = Float.parseFloat(ratesSettings.getProperty("RateRunSpeed", "1."));
			RATE_DROP_MANOR = Float.parseFloat(ratesSettings.getProperty("RateDropManor", "1."));
			KARMA_DROP_LIMIT = Integer.parseInt(ratesSettings.getProperty("KarmaDropLimit", "10"));
			KARMA_RATE_DROP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDrop", "70"));
			KARMA_RATE_DROP_ITEM = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropItem", "50"));
			KARMA_RATE_DROP_EQUIP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquip", "40"));
			KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquipWeapon", "10"));

			PET_XP_RATE = Float.parseFloat(ratesSettings.getProperty("PetXpRate", "1."));
			PET_FOOD_RATE = Float.parseFloat(ratesSettings.getProperty("PetFoodRate", "1"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + RATES_FILE + " File.");
		}
	}

	//===========================================================================
	public static final String	ALT_FILE	= "./config/main/altgame.properties";
	//===========================================================================
	public static int			MIN_NPC_ANIMATION;
	public static int			MAX_NPC_ANIMATION;
	public static int			MIN_MONSTER_ANIMATION;
	public static int			MAX_MONSTER_ANIMATION;
	public static int			NPC_MIN_WALK_ANIMATION;
	public static int			NPC_MAX_WALK_ANIMATION;
	public static boolean		ALT_GAME_VIEWNPC;
	public static boolean		ALT_GAME_SHOWPC_DROP;
	public static boolean		SHOW_NPC_LVL;
	public static boolean		ALT_MOB_AGGRO_IN_PEACEZONE;
	public static boolean		ALT_ATTACKABLE_NPCS;
	public static boolean		ALLOW_EXCHANGE;
	public static boolean		ALLOW_RENTPET;
	public static boolean		ALLOW_PET_WALKERS;
	public static boolean		ALLOW_WYVERN_UPGRADER;
	public static int			ALT_URN_TEMP_FAIL;
	public static int			MANAGER_CRYSTAL_COUNT;
	public static double		ALT_GAME_CREATION_SPEED;
	public static double		ALT_GAME_CREATION_XP_RATE;
	public static double		ALT_GAME_CREATION_SP_RATE;
	public static int			COMMON_RECIPE_LIMIT;
	public static int			DWARF_RECIPE_LIMIT;
	public static boolean		ALT_GAME_CREATION;
	public static boolean		IS_CRAFTING_ENABLED;
	public static float			ALT_GAME_EXPONENT_XP;
	public static float			ALT_GAME_EXPONENT_SP;
	public static boolean		ALT_GAME_DELEVEL;
	public static boolean		ALT_STRICT_HERO_SYSTEM;
	public static boolean		ENCHANT_HERO_WEAPONS;
	public static boolean		HERO_LOG_NOCLAN;
	public static int			MAX_RUN_SPEED;
	public static int			MAX_PATK_SPEED;
	public static int			MAX_MATK_SPEED;
	public static int			MAX_EVASION;
	public static int			CHANCE_LEVEL;
	public static float			ALT_GAME_SUMMON_PENALTY_RATE;
	public static int			ALT_MOB_NOAGRO;

	//********************************************************************************************
	public static void loadAltConfig()
	{
		_log.info("Loading: " + ALT_FILE);
		try
		{
			Properties altSettings = new L2Properties("./" + ALT_FILE);

			MANAGER_CRYSTAL_COUNT = Integer.parseInt(altSettings.getProperty("ManagerCrystalCount", "25"));
			ALT_URN_TEMP_FAIL = Integer.parseInt(altSettings.getProperty("UrnTempFail", "10"));
			ALLOW_EXCHANGE = Boolean.parseBoolean(altSettings.getProperty("AllowExchange", "true"));
			ALT_GAME_VIEWNPC = Boolean.parseBoolean(altSettings.getProperty("AltGameViewNpc", "false"));
			ALT_GAME_SHOWPC_DROP = Boolean.parseBoolean(altSettings.getProperty("AltGameViewNpcDrop", "false"));
			ALT_MOB_AGGRO_IN_PEACEZONE = Boolean.parseBoolean(altSettings.getProperty("AltMobAggroInPeaceZone", "true"));
			ALT_ATTACKABLE_NPCS = Boolean.parseBoolean(altSettings.getProperty("AltAttackableNpcs", "true"));
			MAX_DRIFT_RANGE = Integer.parseInt(altSettings.getProperty("MaxDriftRange", "200"));
			MIN_NPC_ANIMATION = Integer.parseInt(altSettings.getProperty("MinNPCAnimation", "10"));
			MAX_NPC_ANIMATION = Integer.parseInt(altSettings.getProperty("MaxNPCAnimation", "20"));
			NPC_MIN_WALK_ANIMATION = Integer.parseInt(altSettings.getProperty("MinNPCWalkAnimation", "10"));
			NPC_MAX_WALK_ANIMATION = Integer.parseInt(altSettings.getProperty("MaxNPCWalkAnimation", "20"));
			MIN_MONSTER_ANIMATION = Integer.parseInt(altSettings.getProperty("MinMonsterAnimation", "5"));
			MAX_MONSTER_ANIMATION = Integer.parseInt(altSettings.getProperty("MaxMonsterAnimation", "20"));
			SHOW_NPC_LVL = Boolean.parseBoolean(altSettings.getProperty("ShowNpcLevel", "false"));
			ALLOW_WYVERN_UPGRADER = Boolean.parseBoolean(altSettings.getProperty("AllowWyvernUpgrader", "false"));
			ALLOW_RENTPET = Boolean.parseBoolean(altSettings.getProperty("AllowRentPet", "false"));
			ALLOW_PET_WALKERS = Boolean.parseBoolean(altSettings.getProperty("AllowPetWalker", "false"));
			ALT_GAME_DELEVEL = Boolean.parseBoolean(altSettings.getProperty("Delevel", "true"));
			ALT_GAME_EXPONENT_XP = Float.parseFloat(altSettings.getProperty("AltGameExponentXp", "0."));
			ALT_GAME_EXPONENT_SP = Float.parseFloat(altSettings.getProperty("AltGameExponentSp", "0."));
			ALT_STRICT_HERO_SYSTEM = Boolean.parseBoolean(altSettings.getProperty("StrictHeroSystem", "true"));
			ENCHANT_HERO_WEAPONS = Boolean.parseBoolean(altSettings.getProperty("HeroWeaponsCanBeEnchanted", "false"));
			HERO_LOG_NOCLAN = Boolean.parseBoolean(altSettings.getProperty("LogIsHeroNoClan", "false"));
			ALT_GAME_TIREDNESS = Boolean.parseBoolean(altSettings.getProperty("AltGameTiredness", "false"));
			MAX_RUN_SPEED = Integer.parseInt(altSettings.getProperty("MaxRunSpeed", "250"));
			MAX_PATK_SPEED = Integer.parseInt(altSettings.getProperty("MaxPAtkSpeed", "1500"));
			MAX_MATK_SPEED = Integer.parseInt(altSettings.getProperty("MaxMAtkSpeed", "1999"));
			MAX_EVASION = Integer.parseInt(altSettings.getProperty("MaxEvasion", "200"));
			IS_CRAFTING_ENABLED = Boolean.parseBoolean(altSettings.getProperty("CraftingEnabled", "true"));
			DWARF_RECIPE_LIMIT = Integer.parseInt(altSettings.getProperty("DwarfRecipeLimit", "50"));
			COMMON_RECIPE_LIMIT = Integer.parseInt(altSettings.getProperty("CommonRecipeLimit", "50"));
			ALT_GAME_CREATION = Boolean.parseBoolean(altSettings.getProperty("AltGameCreation", "false"));
			ALT_GAME_CREATION_SPEED = Double.parseDouble(altSettings.getProperty("AltGameCreationSpeed", "1"));
			ALT_GAME_CREATION_XP_RATE = Double.parseDouble(altSettings.getProperty("AltGameCreationRateXp", "1"));
			ALT_GAME_CREATION_SP_RATE = Double.parseDouble(altSettings.getProperty("AltGameCreationRateSp", "1"));
			ALT_MOB_NOAGRO = Integer.parseInt(altSettings.getProperty("AltMobNoAttackWithLevelDifference","0"));
			CHANCE_LEVEL = Integer.parseInt(altSettings.getProperty("ChanceToLevel", "32"));
			ALT_GAME_SUMMON_PENALTY_RATE = Float.parseFloat(altSettings.getProperty("AltSummonPenaltyRate", "1."));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + ALT_FILE + " File.");
		}
	}

	
	
	//=======================================================================================
	public static final String	DEV_FILE	= "./config/administration/developer.properties";
	//=======================================================================================
	public static boolean		ASSERT = false;
	public static boolean		DEVELOPER = false;
	public static boolean		DEBUG = false;
	public static boolean		ALT_DEV_NO_QUESTS = false;
	public static boolean		ALT_DEV_NO_SPAWNS = false;
	public static boolean		SHOW_NOT_REG_QUEST = false;
	public static boolean		SERVER_LIST_TESTSERVER = false;
	public static boolean		SHOW_VALID_CHECK = false;	
	public static int			DEADLOCKCHECK_INTERVAL = 10000;
	public static boolean		EXPEREMENTAL_MODE = false;
	public static boolean		SEND_PACKET_LOG = false;
	public static boolean		RECIVE_PACKET_LOG = false;
	public static boolean       DO_NOT_DETECT_LAME = false;
	
	//**************************************************************************************************
	public static void loadDevConfig()
	{
		File f = new File(DEV_FILE);
		if(!f.exists())
			return;
		_log.info("Loading: " + DEV_FILE);
		try
		{
			Properties devSettings = new L2Properties("./" + DEV_FILE);

			DEADLOCKCHECK_INTERVAL = Integer.parseInt(devSettings.getProperty("DeadLockCheck", "10000"));
			DEBUG = Boolean.parseBoolean(devSettings.getProperty("Debug", "false"));
			SERVER_LIST_TESTSERVER = Boolean.parseBoolean(devSettings.getProperty("TestServer", "false"));
			ASSERT = Boolean.parseBoolean(devSettings.getProperty("Assert", "false"));
			DEVELOPER = Boolean.parseBoolean(devSettings.getProperty("Developer", "false"));
			ALT_DEV_NO_QUESTS = Boolean.parseBoolean(devSettings.getProperty("AltDevNoQuests", "false"));
			ALT_DEV_NO_SPAWNS = Boolean.parseBoolean(devSettings.getProperty("AltDevNoSpawns", "false"));
			SHOW_NOT_REG_QUEST = Boolean.parseBoolean(devSettings.getProperty("ShowNotRegQuest", "false"));
			SHOW_VALID_CHECK = Boolean.parseBoolean(devSettings.getProperty("ShowValidCheck", "false"));
			EXPEREMENTAL_MODE = Boolean.parseBoolean(devSettings.getProperty("ExperementalMode", "false"));
			SEND_PACKET_LOG= Boolean.parseBoolean(devSettings.getProperty("LogSendPackets","false"));
			RECIVE_PACKET_LOG = Boolean.parseBoolean(devSettings.getProperty("LogRecivePackets","false"));
			DO_NOT_DETECT_LAME = Boolean.parseBoolean(devSettings.getProperty("DoNotDetectLame","false"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + DEV_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	AREAS_FILE	= "./config/main/areas.properties";
	//=========================================================================
	public static int			LIT_REGISTRATION_MODE;
	public static int			LIT_REGISTRATION_TIME;
	public static int			LIT_MIN_PARTY_CNT;
	public static int			LIT_MAX_PARTY_CNT;
	public static int			LIT_MIN_PLAYER_CNT;
	public static int			LIT_MAX_PLAYER_CNT;
	public static int			LIT_TIME_LIMIT;
	public static int			FS_PARTY_MEMBER_COUNT;
	public static int			RIFT_MIN_PARTY_SIZE;
	public static int			RIFT_MAX_JUMPS;
	public static int			RIFT_AUTO_JUMPS_TIME_MIN;
	public static int			RIFT_AUTO_JUMPS_TIME_MAX;
	public static int			RIFT_ENTER_COST_RECRUIT;
	public static int			RIFT_ENTER_COST_SOLDIER;
	public static int			RIFT_ENTER_COST_OFFICER;
	public static int			RIFT_ENTER_COST_CAPTAIN;
	public static int			RIFT_ENTER_COST_COMMANDER;
	public static int			RIFT_ENTER_COST_HERO;
	public static int			RIFT_SPAWN_DELAY;
	public static int			HS_DEBUFF_CHANCE;
	public static float			RIFT_BOSS_ROOM_TIME_MUTIPLY;
	public static int			FOG_MOBS_CLONE_CHANCE;

	//********************************************************************************
	public static void loadAreasConfig()
	{
		_log.info("Loading: " + AREAS_FILE);
		try
		{
			Properties areasSettings = new L2Properties("./" + AREAS_FILE);

			LIT_REGISTRATION_MODE = Integer.parseInt(areasSettings.getProperty("RegistrationMode", "0"));
			LIT_REGISTRATION_TIME = Integer.parseInt(areasSettings.getProperty("RegistrationTime", "10"));
			LIT_MIN_PARTY_CNT = Integer.parseInt(areasSettings.getProperty("MinPartyCount", "4"));
			LIT_MAX_PARTY_CNT = Integer.parseInt(areasSettings.getProperty("MaxPartyCount", "5"));
			LIT_MIN_PLAYER_CNT = Integer.parseInt(areasSettings.getProperty("MinPlayerCount", "7"));
			LIT_MAX_PLAYER_CNT = Integer.parseInt(areasSettings.getProperty("MaxPlayerCount", "45"));
			LIT_TIME_LIMIT = Integer.parseInt(areasSettings.getProperty("TimeLimit", "35"));
			RIFT_SPAWN_DELAY = Integer.parseInt(areasSettings.getProperty("RiftSpawnDelay", "10000"));
			RIFT_MIN_PARTY_SIZE = Integer.parseInt(areasSettings.getProperty("RiftMinPartySize", "5"));
			RIFT_MAX_JUMPS = Integer.parseInt(areasSettings.getProperty("MaxRiftJumps", "4"));
			RIFT_AUTO_JUMPS_TIME_MIN = Integer.parseInt(areasSettings.getProperty("AutoJumpsDelayMin", "480"));
			RIFT_AUTO_JUMPS_TIME_MAX = Integer.parseInt(areasSettings.getProperty("AutoJumpsDelayMax", "600"));
			RIFT_ENTER_COST_RECRUIT = Integer.parseInt(areasSettings.getProperty("RecruitCost", "18"));
			RIFT_ENTER_COST_SOLDIER = Integer.parseInt(areasSettings.getProperty("SoldierCost", "21"));
			RIFT_ENTER_COST_OFFICER = Integer.parseInt(areasSettings.getProperty("OfficerCost", "24"));
			RIFT_ENTER_COST_CAPTAIN = Integer.parseInt(areasSettings.getProperty("CaptainCost", "27"));
			RIFT_ENTER_COST_COMMANDER = Integer.parseInt(areasSettings.getProperty("CommanderCost", "30"));
			RIFT_ENTER_COST_HERO = Integer.parseInt(areasSettings.getProperty("HeroCost", "33"));
			RIFT_BOSS_ROOM_TIME_MUTIPLY = Float.parseFloat(areasSettings.getProperty("BossRoomTimeMultiply", "1.5"));
			HS_DEBUFF_CHANCE = Integer.parseInt(areasSettings.getProperty("HotSpringDebuffChance", "15"));
			FOG_MOBS_CLONE_CHANCE = Integer.parseInt(areasSettings.getProperty("FOGMobsCloneChance","10"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + AREAS_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	PARTY_FILE	= "./config/main/party.properties";
	//=========================================================================
	public static double		PARTY_XP_CUTOFF_PERCENT;
	public static int			ALT_PARTY_RANGE;
	public static int			ALT_PARTY_RANGE2;
	public static int			MAX_PARTY_LEVEL_DIFFERENCE;
	public static int			PARTY_XP_CUTOFF_LEVEL;
	public static boolean		NO_PARTY_LEVEL_LIMIT;
	public static String		PARTY_XP_CUTOFF_METHOD;

	//********************************************************************************************
	public static void loadPartyConfig()
	{
		_log.info("Loading: " + PARTY_FILE);
		try
		{
			Properties partySettings = new L2Properties("./" + PARTY_FILE);

			PARTY_XP_CUTOFF_METHOD = partySettings.getProperty("PartyXpCutoffMethod", "percentage");
			PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(partySettings.getProperty("PartyXpCutoffPercent", "3."));
			PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(partySettings.getProperty("PartyXpCutoffLevel", "30"));
			ALT_PARTY_RANGE = Integer.parseInt(partySettings.getProperty("AltPartyRange", "1600"));
			ALT_PARTY_RANGE2 = Integer.parseInt(partySettings.getProperty("AltPartyRange2", "1400"));
			MAX_PARTY_LEVEL_DIFFERENCE = Integer.parseInt(partySettings.getProperty("PartyMaxLevelDifference", "0"));
			NO_PARTY_LEVEL_LIMIT = Boolean.parseBoolean(partySettings.getProperty("PartLevelLimit", "true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + PARTY_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	DROP_FILE	= "./config/main/drops.properties";

	public static boolean		MULTIPLE_ITEM_DROP;
	public static boolean		PRECISE_DROP_CALCULATION;
	public static boolean		DEEPBLUE_DROP_RULES;
	public static String 		RATE_INCREASE_CHANCE;
	public static List<Integer> LIST_RATE_INCREASE_CHANCE = new FastList<Integer>();
	public static String 		RATE_INCREASE_QTTY;
	public static List<Integer> LIST_RATE_INCREASE_QTTY = new FastList<Integer>();

	//*******************************************************************************************************
	public static void loadDropsConfig()
	{
		_log.info("Loading: " + DROP_FILE);
		try
		{
			Properties dropSettings = new L2Properties("./" + DROP_FILE);

			MULTIPLE_ITEM_DROP = Boolean.parseBoolean(dropSettings.getProperty("MultipleItemDrop", "true"));
			DEEPBLUE_DROP_RULES = Boolean.parseBoolean(dropSettings.getProperty("UseDeepBlueDropRules", "true"));
			PRECISE_DROP_CALCULATION = Boolean.parseBoolean(dropSettings.getProperty("PreciseDropCalculation", "true"));

			RATE_INCREASE_CHANCE = dropSettings.getProperty("RateIncreaseChance", "0");
			LIST_RATE_INCREASE_CHANCE = new FastList<Integer>();
			for(String id : RATE_INCREASE_CHANCE.split(",")) try {
				LIST_RATE_INCREASE_CHANCE.add(Integer.parseInt(id));
			} catch(NumberFormatException e)  { }

			RATE_INCREASE_QTTY = dropSettings.getProperty("RateIncreaseQtty", "0");
			LIST_RATE_INCREASE_QTTY = new FastList<Integer>();
			for(String id : RATE_INCREASE_QTTY.split(",")) try {
				LIST_RATE_INCREASE_QTTY.add(Integer.parseInt(id));
			} catch(NumberFormatException e)  { }
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + DROP_FILE + " File.");
		}
	}

	//==============================================================================================
	public static final String	COMMUNITY_BOARD_FILE	= "./config/main/communityboard.properties";
	//==============================================================================================
	public static int			NAME_PAGE_SIZE_COMMUNITYBOARD;
	public static int			NAME_PER_ROW_COMMUNITYBOARD;
	public static boolean		SHOW_CURSED_WEAPON_OWNER;
	public static boolean		SHOW_LEVEL_COMMUNITYBOARD;
	public static boolean		SHOW_STATUS_COMMUNITYBOARD;
	public static int			COMMUNITY_TYPE;
	public static boolean		BBS_SHOW_PLAYERLIST;
	public static boolean		SHOW_LEGEND;
	public static boolean		SHOW_KARMA_PLAYERS;
	public static boolean		SHOW_JAILED_PLAYERS;
	public static boolean		SHOW_CLAN_LEADER;
	public static int			SHOW_CLAN_LEADER_CLAN_LEVEL;
	public static boolean		MAIL_STORE_DELETED_LETTERS;
	public static String 		BBS_RESTRICTIONS;
	public static boolean       BBS_SHOW_OFFLINE_TRADERS;
	public static List<String>  BBS_DISABLED_PAGES = new FastList<String>();

	//*************************************************************
	public static void loadCbConfig()
	{
		_log.info("Loading: " + COMMUNITY_BOARD_FILE);
		try
		{
			Properties cbSettings = new L2Properties("./" + COMMUNITY_BOARD_FILE);

			SHOW_CLAN_LEADER = Boolean.parseBoolean(cbSettings.getProperty("ShowClanLeader", "false"));
			SHOW_CLAN_LEADER_CLAN_LEVEL = Integer.parseInt(cbSettings.getProperty("ShowClanLeaderAtClanLevel", "3"));
			SHOW_LEGEND = Boolean.parseBoolean(cbSettings.getProperty("ShowLegend", "false"));
			SHOW_KARMA_PLAYERS = Boolean.parseBoolean(cbSettings.getProperty("ShowKarmaPlayers", "false"));
			SHOW_JAILED_PLAYERS = Boolean.parseBoolean(cbSettings.getProperty("ShowJailedPlayers", "false"));
			COMMUNITY_TYPE = Integer.parseInt(cbSettings.getProperty("CommunityType", "1"));
			BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(cbSettings.getProperty("BBSShowPlayerList", "false"));
			SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(cbSettings.getProperty("ShowLevelOnCommunityBoard", "false"));
			SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(cbSettings.getProperty("ShowStatusOnCommunityBoard", "true"));
			NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(cbSettings.getProperty("NamePageSizeOnCommunityBoard", "50"));
			MAIL_STORE_DELETED_LETTERS = Boolean.parseBoolean(cbSettings.getProperty("MailStoreDeletedLetters", "false"));
			BBS_RESTRICTIONS = cbSettings.getProperty("RestrictCBWhen","COMBAT OLY");
			if (NAME_PAGE_SIZE_COMMUNITYBOARD > 70)
				NAME_PAGE_SIZE_COMMUNITYBOARD = 70;
			NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(cbSettings.getProperty("NamePerRowOnCommunityBoard", "5"));
			BBS_SHOW_OFFLINE_TRADERS = Boolean.parseBoolean(cbSettings.getProperty("ShowOfflineTraders","true"));
			//avoid a client crash.
			if (NAME_PER_ROW_COMMUNITYBOARD > 5)
				NAME_PER_ROW_COMMUNITYBOARD = 5;
			SHOW_CURSED_WEAPON_OWNER = Boolean.parseBoolean(cbSettings.getProperty("ShowCursedWeaponOwner", "false"));
			BBS_DISABLED_PAGES.clear();
			for(String s : cbSettings.getProperty("DisabledPages","").split(" "))
				if(s!=null && s.length()>0)
					BBS_DISABLED_PAGES.add(s);
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + COMMUNITY_BOARD_FILE + " File.");
		}
	}

	//==============================================================================================
	public static final String			SKILLS_FILE				= "./config/main/skills.properties";
	//==============================================================================================
	public static int					ALT_PERFECT_SHLD_BLOCK;
	public static int					ALT_BUFFER_HATE;
	public static int					ALT_BUFFER_TIME;
	public static int					ALT_DANCE_TIME;
	public static int					ALT_SONG_TIME;
	public static int					ALT_HERO_TIME;
	public static int					ALT_5MIN_TIME;
	public static int					ALT_CH_TIME;
	public static int					ALT_PCRITICAL_CAP;
	public static int					ALT_MCRITICAL_CAP;
	public static int					BUFFS_MAX_AMOUNT;
	public static int					SEND_NOTDONE_SKILLS;
	public static int					ALT_MINIMUM_FALL_HEIGHT;
	public static float					ALT_ATTACK_DELAY;
	public static float					ALT_MAGES_PHYSICAL_DAMAGE_MULTI;
	public static float					ALT_MAGES_MAGICAL_DAMAGE_MULTI;
	public static float					ALT_FIGHTERS_PHYSICAL_DAMAGE_MULTI;
	public static float					ALT_FIGHTERS_SKILL_DAMAGE_HEAVY_MULTI;	
	public static float					ALT_FIGHTERS_SKILL_DAMAGE_ROBE_MULTI;
	public static float					ALT_FIGHTERS_SKILL_DAMAGE_LIGHT_MULTI;	
	public static float					ALT_FIGHTERS_SKILL_DAMAGE_OTHER_MULTI;	
	public static float					ALT_FIGHTERS_MAGICAL_DAMAGE_MULTI;
	public static float					ALT_PETS_PHYSICAL_DAMAGE_MULTI;
	public static float					ALT_PETS_MAGICAL_DAMAGE_MULTI;
	public static float					ALT_NPC_PHYSICAL_DAMAGE_MULTI;
	public static float					ALT_NPC_MAGICAL_DAMAGE_MULTI;
	public static float					ALT_DAGGER_DMG_VS_HEAVY;
	public static float					ALT_DAGGER_DMG_VS_ROBE;
	public static float					ALT_DAGGER_DMG_VS_LIGHT;
	public static float					ALT_DAGGER_DMG_VS_OTHER;
	public static byte					BLOW_FRONT;
	public static byte					BLOW_SIDE;
	public static byte					BLOW_BEHIND;
	public static boolean				ALT_GAME_MAGICFAILURES;
	public static boolean				ALT_GAME_TIREDNESS;
	public static boolean				ALT_GAME_SHIELD_BLOCKS;
	public static boolean				ALT_GAME_SKILL_LEARN;
	public static boolean				ALT_GAME_CANCEL_BOW;
	public static boolean				ALT_GAME_CANCEL_CAST;
	public static boolean				ES_SP_BOOK_NEEDED;
	public static boolean				ES_XP_NEEDED;
	public static boolean				ES_SP_NEEDED;
	public static boolean				AUTO_LEARN_SKILLS;
	public static int					AUTO_LEARN_MAX_LEVEL;
	public static boolean				AUTO_LEARN_FORGOTTEN_SKILLS;
	public static boolean				SP_BOOK_NEEDED;
	public static boolean				LIFE_CRYSTAL_NEEDED;
	public static boolean				FAIL_FAKEDEATH;
	public static boolean				CHECK_SKILLS_ON_ENTER;
	public static boolean				CHECK_ADDITIONAL_SKILLS;
	public static boolean				STORE_SKILL_COOLTIME;
	public static boolean				EFFECT_CANCELING;
	public static boolean				ALT_DANCE_MP_CONSUME;
	public static boolean				GRADE_PENALTY;
	public static boolean				AUTO_LEARN_DIVINE_INSPIRATION;
	public static boolean				ALT_ITEM_SKILLS_NOT_INFLUENCED;
	public static String				UNAFFECTED_SKILLS;
	public static FastList<Integer>		UNAFFECTED_SKILL_LIST	= new FastList<Integer>();
	public static boolean				ENABLE_MODIFY_SKILL_DURATION;
	public static Map<Integer, Integer>	SKILL_DURATION_LIST;
	public static String				ALLOWED_SKILLS;
	public static FastList<Integer>		ALLOWED_SKILLS_LIST		= new FastList<Integer>();
	public static float					ALT_LETHAL_RATE_DAGGER;
	public static float					ALT_LETHAL_RATE_ARCHERY;
	public static float					ALT_LETHAL_RATE_OTHER;
	public static boolean				CANCEL_AUGUMENTATION_EFFECT;
	public static boolean				CONSUME_ON_SUCCESS;
	public static boolean 				USE_STATIC_REUSE;
	public static boolean 				USE_OLY_STATIC_REUSE;
	public static int					SKILL_DELAY;
	public static float					MCRIT_RATE;
	public static boolean				USE_LEVEL_PENALTY;
	public static boolean				USE_CHAR_LEVEL_MOD;
	public static boolean				DISABLE_SKILLS_ON_LEVEL_LOST;
	public static boolean				OLD_CANCEL_MODE;
	public static int                   BLOCK_BUFF;
	public static boolean				HEALTH_SKILLS_TO_BOSSES;
	public static boolean				HEALTH_SKILLS_TO_EPIC_BOSSES;

	//********************************************************
	public static void loadSkillsConfig()
	{
		_log.info("Loading: " + SKILLS_FILE);
		try
		{
			Properties skillsSettings = new L2Properties("./" + SKILLS_FILE);

			ALLOWED_SKILLS = skillsSettings.getProperty("AllowedSkills", "0");
			ALLOWED_SKILLS_LIST = new FastList<Integer>();
			for (String id : ALLOWED_SKILLS.trim().split(","))
				ALLOWED_SKILLS_LIST.add(Integer.parseInt(id.trim()));

			UNAFFECTED_SKILLS = skillsSettings.getProperty("UnaffectedSkills");
			UNAFFECTED_SKILL_LIST = new FastList<Integer>();
			CONSUME_ON_SUCCESS = Boolean.parseBoolean(skillsSettings.getProperty("ConsumeOnSuccess","true"));
			USE_STATIC_REUSE = Boolean.parseBoolean(skillsSettings.getProperty("EnableSaticReuse","true"));
				
			for (String id : UNAFFECTED_SKILLS.trim().split(","))
				UNAFFECTED_SKILL_LIST.add(Integer.parseInt(id.trim()));

			ENABLE_MODIFY_SKILL_DURATION = Boolean.parseBoolean(skillsSettings.getProperty("EnableModifySkillDuration", "false"));
			MCRIT_RATE = Float.parseFloat(skillsSettings.getProperty("MCritRate","2"));

			// Create Map only if enabled
			if (ENABLE_MODIFY_SKILL_DURATION)
			{
				SKILL_DURATION_LIST = new FastMap<Integer, Integer>();
				String[] propertySplit;
				propertySplit = skillsSettings.getProperty("SkillDurationList", "").split(";");
				for (String skill : propertySplit)
				{
					String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
						_log.info("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
					else
					{
						try
						{
							SKILL_DURATION_LIST.put(Integer.valueOf(skillSplit[0]), Integer.valueOf(skillSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!skill.equals(""))
								_log.info("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
						}
					}
				}
			}

			ALT_ITEM_SKILLS_NOT_INFLUENCED = Boolean.parseBoolean(skillsSettings.getProperty("AltItemSkillsNotInfluenced", "false"));
			AUTO_LEARN_DIVINE_INSPIRATION = Boolean.parseBoolean(skillsSettings.getProperty("AutoLearnDivineInspiration", "false"));
			ALT_MINIMUM_FALL_HEIGHT = Integer.parseInt(skillsSettings.getProperty("AltMinimumFallHeight", "400"));
			SEND_NOTDONE_SKILLS = Integer.parseInt(skillsSettings.getProperty("SendNOTDONESkills", "2"));
			ALT_DANCE_MP_CONSUME = Boolean.parseBoolean(skillsSettings.getProperty("AltDanceMpConsume", "false"));
			ALT_DAGGER_DMG_VS_HEAVY = Float.parseFloat(skillsSettings.getProperty("DaggerVSHeavy", "1.00"));
			ALT_DAGGER_DMG_VS_ROBE = Float.parseFloat(skillsSettings.getProperty("DaggerVSRobe", "1.00"));
			ALT_DAGGER_DMG_VS_LIGHT = Float.parseFloat(skillsSettings.getProperty("DaggerVSLight", "1.00"));
			ALT_DAGGER_DMG_VS_OTHER = Float.parseFloat(skillsSettings.getProperty("DaggerVSLight", "1.00"));			
			BLOW_FRONT = Byte.parseByte(skillsSettings.getProperty("BlowFront", "50"));
			BLOW_SIDE = Byte.parseByte(skillsSettings.getProperty("BlowSide", "60"));
			BLOW_BEHIND = Byte.parseByte(skillsSettings.getProperty("BlowBehind", "70"));
			LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(skillsSettings.getProperty("LifeCrystalNeeded", "true"));
			ALT_BUFFER_HATE = Integer.parseInt(skillsSettings.getProperty("BufferHate", "4"));
			GRADE_PENALTY = Boolean.parseBoolean(skillsSettings.getProperty("GradePenalty", "true"));
			ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(skillsSettings.getProperty("AltGameSkillLearn", "false"));
			CHAR_VIP_SKIP_SKILLS_CHECK = Boolean.parseBoolean(skillsSettings.getProperty("CharViPSkipSkillsCheck", "false"));
			SP_BOOK_NEEDED = Boolean.parseBoolean(skillsSettings.getProperty("SpBookNeeded", "true"));
			AUTO_LEARN_SKILLS = Boolean.parseBoolean(skillsSettings.getProperty("AutoLearnSkills", "false"));
			AUTO_LEARN_FORGOTTEN_SKILLS = Boolean.parseBoolean(skillsSettings.getProperty("AutoLearnForgottenSkills", "false"));
			ALT_PCRITICAL_CAP = Integer.parseInt(skillsSettings.getProperty("AltPCriticalCap", "500"));
			ALT_MCRITICAL_CAP = Integer.parseInt(skillsSettings.getProperty("AltMCriticalCap", "200"));
			FAIL_FAKEDEATH = Boolean.parseBoolean(skillsSettings.getProperty("FailFakeDeath", "true"));
			CHECK_SKILLS_ON_ENTER = Boolean.parseBoolean(skillsSettings.getProperty("CheckSkillsOnEnter", "false"));
			CHECK_ADDITIONAL_SKILLS = Boolean.parseBoolean(skillsSettings.getProperty("CheckAdditionalSkills", "false"));
			BUFFS_MAX_AMOUNT = Integer.parseInt(skillsSettings.getProperty("MaxBuffAmount", "20"));
			ALT_BUFFER_TIME = Integer.parseInt(skillsSettings.getProperty("AltBufferTime", "1"));
			ALT_DANCE_TIME = Integer.parseInt(skillsSettings.getProperty("AltDanceTime", "1"));
			ALT_SONG_TIME = Integer.parseInt(skillsSettings.getProperty("AltSongTime", "1"));
			ALT_CH_TIME = Integer.parseInt(skillsSettings.getProperty("AltChTime", "1"));
			ALT_HERO_TIME = Integer.parseInt(skillsSettings.getProperty("AltHeroTime", "1"));
			ALT_5MIN_TIME = Integer.parseInt(skillsSettings.getProperty("Alt5MinTime", "1"));
			ALT_MAGES_PHYSICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPDamageMages", "1.00"));
			ALT_MAGES_MAGICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltMDamageMages", "1.00"));
			ALT_FIGHTERS_PHYSICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPDamageFighters", "1.00"));
			ALT_FIGHTERS_SKILL_DAMAGE_HEAVY_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPSkillHeavy", "1.00"));
			ALT_FIGHTERS_SKILL_DAMAGE_ROBE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPSkillRobe", "1.00"));
			ALT_FIGHTERS_SKILL_DAMAGE_LIGHT_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPSkillLight", "1.00"));
			ALT_FIGHTERS_SKILL_DAMAGE_OTHER_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPSkillOther", "1.00"));
			ALT_FIGHTERS_MAGICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltMDamageFighters", "1.00"));
			ALT_PETS_PHYSICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPDamagePets", "1.00"));
			ALT_PETS_MAGICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltMDamagePets", "1.00"));
			ALT_NPC_PHYSICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltPDamageNpc", "1.00"));
			ALT_NPC_MAGICAL_DAMAGE_MULTI = Float.parseFloat(skillsSettings.getProperty("AltMDamageNpc", "1.00"));
			ALT_ATTACK_DELAY = Float.parseFloat(skillsSettings.getProperty("AltAttackDelay", "1.00"));
			ALT_GAME_CANCEL_BOW = skillsSettings.getProperty("AltGameCancelByHit", "Cast").trim().equalsIgnoreCase("bow") || skillsSettings.getProperty("AltGameCancelByHit", "Cast").trim().equalsIgnoreCase("all");
			ALT_GAME_CANCEL_CAST = skillsSettings.getProperty("AltGameCancelByHit", "Cast".trim()).equalsIgnoreCase("cast") || skillsSettings.getProperty("AltGameCancelByHit", "Cast").trim().equalsIgnoreCase("all");
			ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(skillsSettings.getProperty("AltShieldBlocks", "false"));
			ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(skillsSettings.getProperty("AltPerfectShieldBlockRate", "10"));
			STORE_SKILL_COOLTIME = Boolean.parseBoolean(skillsSettings.getProperty("StoreSkillCooltime", "true"));
			EFFECT_CANCELING = Boolean.parseBoolean(skillsSettings.getProperty("CancelLesserEffect", "true"));
			ES_SP_BOOK_NEEDED = Boolean.parseBoolean(skillsSettings.getProperty("EnchantSkillSpBookNeeded", "true"));
			ES_XP_NEEDED = Boolean.parseBoolean(skillsSettings.getProperty("EnchSkillXpNeeded", "true"));
			ES_SP_NEEDED = Boolean.parseBoolean(skillsSettings.getProperty("EnchSkillSpNeeded", "true"));
			ALT_LETHAL_RATE_DAGGER = Float.parseFloat(skillsSettings.getProperty("AltLethalRateDagger", "0.5"));
			ALT_LETHAL_RATE_OTHER = Float.parseFloat(skillsSettings.getProperty("AltLethalRateOther", "1.0"));
			ALT_LETHAL_RATE_ARCHERY = Float.parseFloat(skillsSettings.getProperty("AltLethalRateArchery", "0.8"));
			CANCEL_AUGUMENTATION_EFFECT = Boolean.parseBoolean(skillsSettings.getProperty("CancelAugumentionEffect", "true"));
			AUTO_LEARN_MAX_LEVEL = Integer.parseInt(skillsSettings.getProperty("AutoLearnMaxLevel","-1"));
			SKILL_DELAY = Integer.parseInt(skillsSettings.getProperty("SkillReuseDelay","70"));
			USE_LEVEL_PENALTY = Boolean.parseBoolean(skillsSettings.getProperty("UseLevelPenalty","true"));
			USE_OLY_STATIC_REUSE = Boolean.parseBoolean(skillsSettings.getProperty("OlyUseStaticReuse","true"));
			DISABLE_SKILLS_ON_LEVEL_LOST = Boolean.parseBoolean(skillsSettings.getProperty("DisableSkillsOnLevelLost","false"));
			USE_CHAR_LEVEL_MOD = Boolean.parseBoolean(skillsSettings.getProperty("UseCharLevelModifier","true"));
			OLD_CANCEL_MODE = skillsSettings.getProperty("CancelMode","new").toLowerCase().equals("old");
			BLOCK_BUFF = Integer.parseInt(skillsSettings.getProperty("BlockBuff", "7077"));
			HEALTH_SKILLS_TO_BOSSES = Boolean.parseBoolean(skillsSettings.getProperty("HealthSkillsToBosses","true"));
			HEALTH_SKILLS_TO_EPIC_BOSSES = Boolean.parseBoolean(skillsSettings.getProperty("HealthSkillsToEpicBosses","true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + SKILLS_FILE + " File.");
		}
	}


	//===============================================================================================
	public static final String			CLASSMASTER_FILE	= "./config/mods/classmaster.properties";
	//===============================================================================================
	public static boolean				SPAWN_CLASS_MASTER;
	public static String				CLASS_MASTER_SETTINGS_LINE;
	public static ClassMasterSettings	CLASS_MASTER_SETTINGS;
	public static boolean				ALLOW_SAY_MSG_CLASS_MASSTER;
	public static boolean				CLASS_MASTER_POPUP;

	//*************************************************************************************************
	public static void loadClassMasterConfig()
	{
		_log.info("Loading: " + CLASSMASTER_FILE);
		try
		{
			Properties cmSettings = new L2Properties("./" + CLASSMASTER_FILE);

			ALLOW_SAY_MSG_CLASS_MASSTER = Boolean.parseBoolean(cmSettings.getProperty("AllowDialogClassMater", "false"));
			SPAWN_CLASS_MASTER = Boolean.parseBoolean(cmSettings.getProperty("SpawnClassMaster", "false"));

			if (!cmSettings.getProperty("ConfigClassMaster").trim().equalsIgnoreCase("false"))
				CLASS_MASTER_SETTINGS_LINE = cmSettings.getProperty("ConfigClassMaster");

			CLASS_MASTER_POPUP = Boolean.parseBoolean(cmSettings.getProperty("ClassMasterPopopWindow","true"));
			CLASS_MASTER_SETTINGS = new ClassMasterSettings(CLASS_MASTER_SETTINGS_LINE);
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("GameSever: Failed to Load " + CLASSMASTER_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	MANOR_FILE	= "./config/main/manor.properties";
	//=========================================================================
	public static int			ALT_MANOR_REFRESH_TIME;				// Manor Refresh Starting time
	public static int			ALT_MANOR_REFRESH_MIN;					// Manor Refresh Min
	public static int			ALT_MANOR_APPROVE_TIME;				// Manor Next Period Approve Starting time
	public static int			ALT_MANOR_APPROVE_MIN;					//Manor Next Period Approve Min
	public static int			ALT_MANOR_MAINTENANCE_PERIOD;			// Manor Maintenance Time
	public static int			ALT_MANOR_SAVE_PERIOD_RATE;			// Manor Save Period Rate
	public static boolean		ALLOW_MANOR;							// Allow Manor system
	public static boolean		ALT_MANOR_SAVE_ALL_ACTIONS;			// Manor Save All Actions

	//***********************************************************************************************
	public static void loadManorConfig()
	{
		_log.info("Loading: " + MANOR_FILE);
		try
		{
			Properties manorSettings = new L2Properties("./" + MANOR_FILE);

			ALLOW_MANOR = Boolean.parseBoolean(manorSettings.getProperty("AllowManor", "false"));
			ALT_MANOR_REFRESH_TIME = Integer.parseInt(manorSettings.getProperty("AltManorRefreshTime", "20"));
			ALT_MANOR_REFRESH_MIN = Integer.parseInt(manorSettings.getProperty("AltManorRefreshMin", "00"));
			ALT_MANOR_APPROVE_TIME = Integer.parseInt(manorSettings.getProperty("AltManorApproveTime", "6"));
			ALT_MANOR_APPROVE_MIN = Integer.parseInt(manorSettings.getProperty("AltManorApproveMin", "00"));
			ALT_MANOR_MAINTENANCE_PERIOD = Integer.parseInt(manorSettings.getProperty("AltManorMaintenancePeriod", "360000"));
			ALT_MANOR_SAVE_ALL_ACTIONS = Boolean.parseBoolean(manorSettings.getProperty("AltManorSaveAllActions", "false"));
			ALT_MANOR_SAVE_PERIOD_RATE = Integer.parseInt(manorSettings.getProperty("AltManorSavePeriodRate", "2"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("GameSever: Failed to Load " + MANOR_FILE + " File.");
		}
	}

	//=======================================================================================
	public static final String	PERMISSIONS_FILE	= "./config/main/permissions.properties";
	//=======================================================================================
	public static boolean		ALLOW_CURSED_WEAPONS;
	public static boolean		ALLOW_WEAR;
	public static boolean		ALLOW_LOTTERY;
	public static boolean		ALLOW_WATER;
	public static boolean		ALLOW_BOAT;
	public static boolean		ALLOW_GUARDS;
	public static boolean		ALLOW_FISHING;
	public static boolean		ALLOW_NPC_WALKERS;

	//***********************************************************************************************
	public static void loadPermissionsConfig()
	{
		_log.info("Loading: " + PERMISSIONS_FILE);
		try
		{
			Properties permissionsSettings = new L2Properties("./" + PERMISSIONS_FILE);

			ALLOW_NPC_WALKERS = Boolean.parseBoolean(permissionsSettings.getProperty("AllowNpcWalkers", "false"));
			ALLOW_GUARDS = Boolean.parseBoolean(permissionsSettings.getProperty("AllowGuards", "false"));
			ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(permissionsSettings.getProperty("AllowCursedWeapons", "false"));
			ALLOW_WEAR = Boolean.parseBoolean(permissionsSettings.getProperty("AllowWear", "false"));
			ALLOW_LOTTERY = Boolean.parseBoolean(permissionsSettings.getProperty("AllowLottery", "false"));
			ALLOW_WATER = Boolean.parseBoolean(permissionsSettings.getProperty("AllowWater", "true"));
			ALLOW_FISHING = Boolean.parseBoolean(permissionsSettings.getProperty("AllowFishing", "true"));
			ALLOW_BOAT = Boolean.parseBoolean(permissionsSettings.getProperty("AllowBoat", "false"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("GameSever: Failed to Load " + PERMISSIONS_FILE + " File.");
		}
	}

	//=========================================================================
	public static final String	CLAN_FILE	= "./config/main/clans.properties";
	//=========================================================================
	public static int			MIN_CLAN_LEVEL_FOR_USE_AUCTION;
	public static int			ALT_CLAN_MEMBERS_FOR_WAR;
	public static int			ALT_CLAN_JOIN_DAYS;
	public static int			ALT_CLAN_CREATE_DAYS;
	public static int			ALT_CLAN_DISSOLVE_DAYS;
	public static int			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int			ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int			MEMBER_FOR_LEVEL_SIX;					// Number of members to level up a clan to lvl 6
	public static int			MEMBER_FOR_LEVEL_SEVEN;				// Number of members to level up a clan to lvl 7
	public static int			MEMBER_FOR_LEVEL_EIGHT;				// Number of members to level up a clan to lvl 8
	public static int			MINIMUN_LEVEL_FOR_PLEDGE_CREATION;		// minimun level to create a clan.
	public static boolean		ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;	// Alternative gaming - clan members with see privilege can also withdraw from clan warehouse.
	public static boolean		REMOVE_CASTLE_CIRCLETS;				// Remove Castle circlets after clan lose his castle? - default true
	public static int			ALT_REPUTATION_SCORE_PER_KILL;			// Number of reputation points gained per Kill in Clanwar.
	public static String		ALT_REPUTATION_SCORE_PER_KILL_SM;

	//***********************************************************************
	public static void loadClansConfig()
	{
		_log.info("Loading: " + CLAN_FILE);
		try
		{
			Properties clansSettings = new L2Properties("./" + CLAN_FILE);

			MIN_CLAN_LEVEL_FOR_USE_AUCTION = Integer.parseInt(clansSettings.getProperty("LvlForUseAuction", "2"));
			REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(clansSettings.getProperty("RemoveCastleCirclets", "true"));
			MINIMUN_LEVEL_FOR_PLEDGE_CREATION = Integer.parseInt(clansSettings.getProperty("MinLevelToCreatePledge", "10"));
			ALT_CLAN_MEMBERS_FOR_WAR = Integer.parseInt(clansSettings.getProperty("AltClanMembersForWar", "15"));
			ALT_CLAN_JOIN_DAYS = Integer.parseInt(clansSettings.getProperty("DaysBeforeJoinAClan", "1"));
			ALT_CLAN_CREATE_DAYS = Integer.parseInt(clansSettings.getProperty("DaysBeforeCreateAClan", "10"));
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(clansSettings.getProperty("AltMembersCanWithdrawFromClanWH", "false"));
			ALT_MAX_NUM_OF_CLANS_IN_ALLY = Integer.parseInt(clansSettings.getProperty("AltMaxNumOfClansInAlly", "3"));
			ALT_CLAN_DISSOLVE_DAYS = Integer.parseInt(clansSettings.getProperty("DaysToPassToDissolveAClan", "7"));
			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = Integer.parseInt(clansSettings.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = Integer.parseInt(clansSettings.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = Integer.parseInt(clansSettings.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = Integer.parseInt(clansSettings.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
			MEMBER_FOR_LEVEL_SIX = Integer.parseInt(clansSettings.getProperty("MemberForLevel6", "30"));
			MEMBER_FOR_LEVEL_SEVEN = Integer.parseInt(clansSettings.getProperty("MemberForLevel7", "80"));
			MEMBER_FOR_LEVEL_EIGHT = Integer.parseInt(clansSettings.getProperty("MemberForLevel8", "120"));
			ALT_REPUTATION_SCORE_PER_KILL = Integer.parseInt(clansSettings.getProperty("ReputationScorePerKill", "1"));
			ALT_REPUTATION_SCORE_PER_KILL_SM = clansSettings.getProperty("ReputationScorePerKill", "1");
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + CLAN_FILE + " File.");
		}
	}

	//=======================================================================================================
	public static final String		OLYMPIAD_FILE				= "./config/main/events/olympiad.properties";
	//=======================================================================================================
	public static int				ALT_OLY_START_TIME;										// Olympiad Competition Starting time
	public static int				ALT_OLY_MIN;												// Olympiad Minutes
	public static int				ALT_OLY_CPERIOD;											// Olympaid Competition Period
	public static int				ALT_OLY_BATTLE;											// Olympiad Battle Period
	public static int				ALT_OLY_WPERIOD;
	public static int				ALT_OLY_VPERIOD;
	public static boolean			ALT_OLY_ALLOW_BSS;
	public static boolean			ALT_OLY_ENABLED;
	public static boolean			ALT_OLY_SAME_IP;
	public static int				ALT_OLY_CLASSED;
	public static int				ALT_OLY_BATTLE_REWARD_ITEM;
	public static int				ALT_OLY_CLASSED_RITEM_C;
	public static int				ALT_OLY_NONCLASSED_RITEM_C;
	public static int				ALT_OLY_GP_PER_POINT;
	public static int				ALT_OLY_MIN_POINT_FOR_EXCH;
	public static int				ALT_OLY_HERO_POINTS;
	public static FastList<Integer>	LIST_OLY_RESTRICTED_ITEMS	= new FastList<Integer>();
	public static int				ALT_OLY_NONCLASSED;
	public static boolean			ALT_OLY_MATCH_HEAL_COUNTS;
	// public static boolean			ALT_OLY_SUMMON_DAMAGE_COUNTS;
	public static boolean			ALT_OLY_REMOVE_CUBICS;
	public static boolean			ALT_OLY_LOG_FIGHTS;
	public static boolean			ALT_OLY_SHOW_MONTHLY_WINNERS;
	public static int				ALT_OLY_ENCHANT_LIMIT;
	public static boolean			ALT_OLY_RESET_SKILL_TIME;
	public static boolean			ALT_OLY_REMOVE_POINTS_ON_TIE;
	public static int				ALT_OLY_START_PCOUNT;
	public static int				ALT_OLY_WEEKLY_PCOUNT;
	public static String			ALT_OLY_DURATION_TYPES;
	public static int				ALT_OLY_DURATION;
	public static boolean			ALT_OLY_INCLUDE_SUMMON_DAMAGE;

	//*********************************************************
	public static void loadOlympiadConfig()
	{
		_log.info("Loading: " + OLYMPIAD_FILE);
		try
		{
			Properties olympiadSettings = new L2Properties("./" + OLYMPIAD_FILE);

			ALT_OLY_SAME_IP = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlySameIp", "true"));
			ALT_OLY_START_TIME = Integer.parseInt(olympiadSettings.getProperty("AltOlyStartTime", "18"));
			ALT_OLY_MIN = Integer.parseInt(olympiadSettings.getProperty("AltOlyMin", "00"));
			ALT_OLY_CPERIOD = Integer.parseInt(olympiadSettings.getProperty("AltOlyCPeriod", "21600000"));
			ALT_OLY_BATTLE = Integer.parseInt(olympiadSettings.getProperty("AltOlyBattle", "360000"));
			ALT_OLY_WPERIOD = Integer.parseInt(olympiadSettings.getProperty("AltOlyWperiod", "604800000"));
			ALT_OLY_VPERIOD = Integer.parseInt(olympiadSettings.getProperty("AltOlyVperiod", "86400000"));
			ALT_OLY_ALLOW_BSS = Boolean.parseBoolean(olympiadSettings.getProperty("OlympiadAllowBSS", "false"));
			ALT_OLY_CLASSED = Integer.parseInt(olympiadSettings.getProperty("AltOlyClassedParticipants", "5"));
			ALT_OLY_NONCLASSED = Integer.parseInt(olympiadSettings.getProperty("AltOlyNonClassedParticipants", "9"));
			ALT_OLY_MATCH_HEAL_COUNTS = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlyMatchHealCounts", "false"));
//			ALT_OLY_SUMMON_DAMAGE_COUNTS = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlySummonDamageCounts", "false"));
			ALT_OLY_BATTLE_REWARD_ITEM = Integer.parseInt(olympiadSettings.getProperty("AltOlyRewardItem", "6651"));
			ALT_OLY_CLASSED_RITEM_C = Integer.parseInt(olympiadSettings.getProperty("AltOlyClassedRewItemCount", "50"));
			ALT_OLY_NONCLASSED_RITEM_C = Integer.parseInt(olympiadSettings.getProperty("AltOlyNonClassedRewItemCount", "30"));
			ALT_OLY_GP_PER_POINT = Integer.parseInt(olympiadSettings.getProperty("AltOlyGPPerPoint", "1000"));
			ALT_OLY_MIN_POINT_FOR_EXCH = Integer.parseInt(olympiadSettings.getProperty("AltOlyMinPointForExchange", "50"));
			ALT_OLY_HERO_POINTS = Integer.parseInt(olympiadSettings.getProperty("AltOlyHeroPoints", "300"));
			ALT_OLY_REMOVE_CUBICS = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlyRemoveCubics", "true"));
			LIST_OLY_RESTRICTED_ITEMS = new FastList<Integer>();
			for (String id : olympiadSettings.getProperty("AltOlyRestrictedItems","0").split(","))
				LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
			ALT_OLY_LOG_FIGHTS = Boolean.parseBoolean(olympiadSettings.getProperty("AlyOlyLogFights","false"));
			ALT_OLY_SHOW_MONTHLY_WINNERS = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlyShowMonthlyWinners","true"));
			ALT_OLY_ENCHANT_LIMIT = Integer.parseInt(olympiadSettings.getProperty("AltOlyEnchantLimit","7"));
			if (ALT_OLY_ENCHANT_LIMIT > 65535)
				ALT_OLY_ENCHANT_LIMIT = 65535;
			ALT_OLY_RESET_SKILL_TIME = Boolean.parseBoolean(olympiadSettings.getProperty("AltOlyResetSkillTime","true"));
			ALT_OLY_START_PCOUNT = Integer.parseInt(olympiadSettings.getProperty("AltOlyStartPointsCount","18"));
			ALT_OLY_WEEKLY_PCOUNT = Integer.parseInt(olympiadSettings.getProperty("AltOlyWeeklyPointsCount","3"));
			ALT_OLY_ENABLED = Boolean.parseBoolean(olympiadSettings.getProperty("OlympiadEnabled","true"));
			ALT_OLY_DURATION_TYPES = olympiadSettings.getProperty("OlympiadDurationType","Month");
			ALT_OLY_DURATION = Integer.parseInt(olympiadSettings.getProperty("OlympiadDuration","1"));
			ALT_OLY_REMOVE_POINTS_ON_TIE = Boolean.parseBoolean(olympiadSettings.getProperty("OlympiadRemovePointsOnTie","true"));
			ALT_OLY_INCLUDE_SUMMON_DAMAGE = Boolean.parseBoolean(olympiadSettings.getProperty("IncludeSummonDamage","true"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + OLYMPIAD_FILE + " File.");
		}
	}

	//======================================================================================
	public static final String	LOTTERY_FILE	= "./config/main/events/lottery.properties";
	//======================================================================================
	public static int			ALT_LOTTERY_PRIZE;
	public static int			ALT_LOTTERY_TICKET_PRICE;
	public static int			ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	public static float			ALT_LOTTERY_5_NUMBER_RATE;
	public static float			ALT_LOTTERY_4_NUMBER_RATE;
	public static float			ALT_LOTTERY_3_NUMBER_RATE;

	//***********************************************************
	public static void loadLotteryConfig()
	{
		_log.info("Loading: " + LOTTERY_FILE);

		try
		{
			Properties LotterySettings = new L2Properties("./" + LOTTERY_FILE);

			ALT_LOTTERY_PRIZE = Integer.parseInt(LotterySettings.getProperty("AltLotteryPrize", "50000"));
			ALT_LOTTERY_TICKET_PRICE = Integer.parseInt(LotterySettings.getProperty("AltLotteryTicketPrice", "2000"));
			ALT_LOTTERY_5_NUMBER_RATE = Float.parseFloat(LotterySettings.getProperty("AltLottery5NumberRate", "0.6"));
			ALT_LOTTERY_4_NUMBER_RATE = Float.parseFloat(LotterySettings.getProperty("AltLottery4NumberRate", "0.2"));
			ALT_LOTTERY_3_NUMBER_RATE = Float.parseFloat(LotterySettings.getProperty("AltLottery3NumberRate", "0.2"));
			ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = Integer.parseInt(LotterySettings.getProperty("AltLottery2and1NumberPrize", "200"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + LOTTERY_FILE + " File.");
		}
	}

	//=================================================================================
	public static final String	INVENTORY_FILE	= "./config/main/inventory.properties";
	//=================================================================================
	public static int			INVENTORY_MAXIMUM_NO_DWARF;
	public static int			INVENTORY_MAXIMUM_DWARF;
	public static int			INVENTORY_MAXIMUM_GM;
	public static int			ALT_INVENTORY_MAXIMUM_PET;
	public static int			MAX_ITEM_IN_PACKET;
	public static boolean		ALLOW_AUTO_LOOT;
	public static boolean		AUTO_LOOT;
	public static boolean		AUTO_LOOT_RAID;
	public static boolean		AUTO_LOOT_HERBS;
	public static boolean		AUTO_LOOT_ADENA;
	public static boolean		DESTROY_PLAYER_INVENTORY_DROP;					// Auto destroy items dropped by players from inventory
	public static boolean		ALLOW_DISCARDITEM;

	//************************************************
	public static void loadInventoryConfig()
	{
		_log.info("Loading: " + INVENTORY_FILE);
		try
		{
			Properties inventorySettings = new L2Properties("./" + INVENTORY_FILE);

			AUTO_LOOT_ADENA = inventorySettings.getProperty("AutoLootAdena").trim().equalsIgnoreCase("true");
			ALLOW_DISCARDITEM = Boolean.parseBoolean(inventorySettings.getProperty("AllowDiscardItem", "true"));
			DESTROY_PLAYER_INVENTORY_DROP = Boolean.parseBoolean(inventorySettings.getProperty("DestroyPlayerInventoryDrop", "false"));
			ALLOW_AUTO_LOOT = inventorySettings.getProperty("AlowAutoLoot").trim().equalsIgnoreCase("true");
			AUTO_LOOT = inventorySettings.getProperty("AutoLootDefault").trim().equalsIgnoreCase("true");
			AUTO_LOOT_RAID = Boolean.parseBoolean(inventorySettings.getProperty("AutoLootRaid", "true"));
			AUTO_LOOT_HERBS = inventorySettings.getProperty("AutoLootHerbs").trim().equalsIgnoreCase("true");
			INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(inventorySettings.getProperty("MaxInventorySlotsForOther", "100"));
			INVENTORY_MAXIMUM_DWARF = Integer.parseInt(inventorySettings.getProperty("MaxInventorySlotsForDwarf", "150"));
			INVENTORY_MAXIMUM_GM = Integer.parseInt(inventorySettings.getProperty("MaxInventorySlotsForGameMaster", "300"));
			ALT_INVENTORY_MAXIMUM_PET = Integer.parseInt(inventorySettings.getProperty("MaximumSlotsForPet", "12"));
			MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + INVENTORY_FILE + " File.");
		}
	}

	//========================================================================================
	public static final String		GRID_FILE				= "./config/main/grid.properties";
	//========================================================================================
	public static int				AUTODESTROY_ITEM_AFTER;							// Time after which item will auto-destroy
	public static int				HERB_AUTO_DESTROY_TIME;							// Auto destroy herb time
	public static int				GRID_NEIGHBOR_TURNON_TIME;
	public static int				GRID_NEIGHBOR_TURNOFF_TIME;
	public static int				GRID_AUTO_DESTROY_ITEM_AFTER;
	public static int				GRID_AUTO_DESTROY_HERB_TIME;
	public static int				SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean			DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean			DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean			SAVE_DROPPED_ITEM;
	public static boolean			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static boolean			CLEAR_DROPPED_ITEM_TABLE;
	public static boolean			GRIDS_ALWAYS_ON;
	public static String			PROTECTED_ITEMS;
	public static FastList<Integer>	LIST_PROTECTED_ITEMS	= new FastList<Integer>();

	//****************************************************************
	public static void loadGridConfig()
	{
		_log.info("Loading: " + GRID_FILE);
		try
		{
			Properties gridSettings = new L2Properties("./" + GRID_FILE);

			GRID_AUTO_DESTROY_ITEM_AFTER = Integer.parseInt(gridSettings.getProperty("AutoDestroyDroppedItemAfter", "0"));
			AUTODESTROY_ITEM_AFTER = Integer.parseInt(gridSettings.getProperty("AutoDestroyDroppedItemAfter", "0"));
			HERB_AUTO_DESTROY_TIME = Integer.parseInt(gridSettings.getProperty("AutoDestroyHerbTime", "15")) * 1000;
			GRID_AUTO_DESTROY_HERB_TIME = Integer.parseInt(gridSettings.getProperty("AutoDestroyHerbTime", "15")) * 1000;
			PROTECTED_ITEMS = gridSettings.getProperty("ListOfProtectedItems");

			LIST_PROTECTED_ITEMS = new FastList<Integer>();
			for (String id : PROTECTED_ITEMS.trim().split(","))
				LIST_PROTECTED_ITEMS.add(Integer.parseInt(id.trim()));

			DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(gridSettings.getProperty("DestroyPlayerDroppedItem", "false"));
			DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(gridSettings.getProperty("DestroyEquipableItem", "false"));
			SAVE_DROPPED_ITEM = Boolean.parseBoolean(gridSettings.getProperty("SaveDroppedItem", "false"));
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(gridSettings.getProperty("EmptyDroppedItemTableAfterLoad", "false"));
			SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(gridSettings.getProperty("SaveDroppedItemInterval", "0")) * 60000;
			CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(gridSettings.getProperty("ClearDroppedItemTable", "false"));
			GRIDS_ALWAYS_ON = Boolean.parseBoolean(gridSettings.getProperty("GridsAlwaysOn", "false"));
			GRID_NEIGHBOR_TURNON_TIME = Integer.parseInt(gridSettings.getProperty("GridNeighborTurnOnTime", "1"));
			GRID_NEIGHBOR_TURNOFF_TIME = Integer.parseInt(gridSettings.getProperty("GridNeighborTurnOffTime", "90"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + GRID_FILE + " File.");
		}
	}

	// ==============================================================================
	public static final String	WEDDING_FILE	= "./config/mods/wedding.properties";
	//===============================================================================
	public static int			WEDDING_PRICE;
	public static int			WEDDING_TELEPORT_PRICE;
	public static int			WEDDING_TELEPORT_INTERVAL;
	public static int			WEDDING_DIVORCE_COSTS;
	public static boolean		ALLOW_WEDDING;
	public static boolean		WEDDING_PUNISH_INFIDELITY;
	public static boolean		WEDDING_TELEPORT;
	public static boolean		WEDDING_SAMESEX;
	public static boolean		WEDDING_FORMALWEAR;
	public static boolean		WEDDING_GIVE_CUPID_BOW;
	public static boolean		WEDDING_HONEYMOON_PORT;
	public static int			WEDDING_PORT_X;
	public static int			WEDDING_PORT_Y;
	public static int			WEDDING_PORT_Z;
	public static boolean		WEDDING_USE_COLOR;
	public static int			WEDDING_NORMAL;
	public static int			WEDDING_GAY;
	public static int			WEDDING_LESBI;

	//*******************************************************************
	public static void loadWeddingConfig()
	{
		_log.info("Loading: " + WEDDING_FILE);
		try
		{
			Properties weddingsSettings = new L2Properties("./" + WEDDING_FILE);

			WEDDING_GIVE_CUPID_BOW = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingGiveBow", "true"));
			WEDDING_HONEYMOON_PORT = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingHoneyMoon", "false"));
			ALLOW_WEDDING = Boolean.parseBoolean(weddingsSettings.getProperty("AllowWedding", "false"));
			WEDDING_PRICE = Integer.parseInt(weddingsSettings.getProperty("WeddingPrice", "500000"));
			WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingPunishInfidelity", "true"));
			WEDDING_TELEPORT = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingTeleport", "true"));
			WEDDING_TELEPORT_PRICE = Integer.parseInt(weddingsSettings.getProperty("WeddingTeleportPrice", "500000"));
			WEDDING_TELEPORT_INTERVAL = Integer.parseInt(weddingsSettings.getProperty("WeddingTeleportInterval", "120"));
			WEDDING_SAMESEX = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingAllowSameSex", "true"));
			WEDDING_FORMALWEAR = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingFormalWear", "true"));
			WEDDING_DIVORCE_COSTS = Integer.parseInt(weddingsSettings.getProperty("WeddingDivorceCosts", "20"));
			WEDDING_PORT_X = Integer.parseInt(weddingsSettings.getProperty("WeddingTeleporX", "0"));
			WEDDING_PORT_Y = Integer.parseInt(weddingsSettings.getProperty("WeddingTeleporY", "0"));
			WEDDING_PORT_Z = Integer.parseInt(weddingsSettings.getProperty("WeddingTeleporZ", "0"));
			WEDDING_USE_COLOR = Boolean.parseBoolean(weddingsSettings.getProperty("WeddingUseNickColor", "true"));
			WEDDING_NORMAL = Integer.valueOf(weddingsSettings.getProperty("WeddingNormalPairNickColor", "BF0000"),16);
			WEDDING_GAY = Integer.valueOf(weddingsSettings.getProperty("WeddingGayPairNickColor", "0000BF"),16);
			WEDDING_LESBI = Integer.valueOf(weddingsSettings.getProperty("WeddingLesbiPairNickColor", "BF00BF"),16);
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + WEDDING_FILE + " File.");
		}
	}

	// ===============================================================================
	public static final String	ENTITIES_FILE	= "./config/main/entities.properties";
	//================================================================================
	// ------------------ Castle Configs -------------------------------------
	public static long			CS_TELE_FEE_RATIO;
	public static int			CS_TELE1_FEE;
	public static int			CS_TELE2_FEE;
	public static long			CS_MPREG_FEE_RATIO;
	public static int			CS_MPREG1_FEE;
	public static int			CS_MPREG2_FEE;
	public static int			CS_MPREG3_FEE;
	public static int			CS_MPREG4_FEE;
	public static long			CS_HPREG_FEE_RATIO;
	public static int			CS_HPREG1_FEE;
	public static int			CS_HPREG2_FEE;
	public static int			CS_HPREG3_FEE;
	public static int			CS_HPREG4_FEE;
	public static int			CS_HPREG5_FEE;
	public static long			CS_EXPREG_FEE_RATIO;
	public static int			CS_EXPREG1_FEE;
	public static int			CS_EXPREG2_FEE;
	public static int			CS_EXPREG3_FEE;
	public static int			CS_EXPREG4_FEE;
	public static long			CS_SUPPORT_FEE_RATIO;
	public static int			CS_SUPPORT1_FEE;
	public static int			CS_SUPPORT2_FEE;
	public static int			CS_SUPPORT3_FEE;
	public static int			CS_SUPPORT4_FEE;

	//---------------- Clan Hall Configs ------------------------------
	public static long			CH_TELE_FEE_RATIO;
	public static int			CH_TELE1_FEE;
	public static int			CH_TELE2_FEE;
	public static long			CH_ITEM_FEE_RATIO;
	public static int			CH_ITEM1_FEE;
	public static int			CH_ITEM2_FEE;
	public static int			CH_ITEM3_FEE;
	public static long			CH_MPREG_FEE_RATIO;
	public static int			CH_MPREG1_FEE;
	public static int			CH_MPREG2_FEE;
	public static int			CH_MPREG3_FEE;
	public static int			CH_MPREG4_FEE;
	public static int			CH_MPREG5_FEE;
	public static long			CH_HPREG_FEE_RATIO;
	public static int			CH_HPREG1_FEE;
	public static int			CH_HPREG2_FEE;
	public static int			CH_HPREG3_FEE;
	public static int			CH_HPREG4_FEE;
	public static int			CH_HPREG5_FEE;
	public static int			CH_HPREG6_FEE;
	public static int			CH_HPREG7_FEE;
	public static int			CH_HPREG8_FEE;
	public static int			CH_HPREG9_FEE;
	public static int			CH_HPREG10_FEE;
	public static int			CH_HPREG11_FEE;
	public static int			CH_HPREG12_FEE;
	public static int			CH_HPREG13_FEE;
	public static long			CH_EXPREG_FEE_RATIO;
	public static int			CH_EXPREG1_FEE;
	public static int			CH_EXPREG2_FEE;
	public static int			CH_EXPREG3_FEE;
	public static int			CH_EXPREG4_FEE;
	public static int			CH_EXPREG5_FEE;
	public static int			CH_EXPREG6_FEE;
	public static int			CH_EXPREG7_FEE;
	public static long			CH_SUPPORT_FEE_RATIO;
	public static int			CH_SUPPORT1_FEE;
	public static int			CH_SUPPORT2_FEE;
	public static int			CH_SUPPORT3_FEE;
	public static int			CH_SUPPORT4_FEE;
	public static int			CH_SUPPORT5_FEE;
	public static int			CH_SUPPORT6_FEE;
	public static int			CH_SUPPORT7_FEE;
	public static int			CH_SUPPORT8_FEE;
	public static long			CH_CURTAIN_FEE_RATIO;
	public static int			CH_CURTAIN1_FEE;
	public static int			CH_CURTAIN2_FEE;
	public static long			CH_FRONT_FEE_RATIO;
	public static int			CH_FRONT1_FEE;
	public static int			CH_FRONT2_FEE;

	//---------------- L2EMU ADD Fort Functios Config ---------------------------
	public static long			FORT_TELE_FEE_RATIO;
	public static int			FORT_TELE1_FEE;
	public static int			FORT_TELE2_FEE;
	
	public static long			FORT_MPREG_FEE_RATIO;
	public static int			FORT_MPREG1_FEE;
	public static int			FORT_MPREG2_FEE;
	
	public static long			FORT_HPREG_FEE_RATIO;
	public static int			FORT_HPREG1_FEE;
	public static int			FORT_HPREG2_FEE;

	public static long			FORT_EXPREG_FEE_RATIO;
	public static int			FORT_EXPREG1_FEE;
	public static int			FORT_EXPREG2_FEE;

	public static long			FORT_SUPPORT_FEE_RATIO;
	public static int			FORT_SUPPORT1_FEE;
	public static int			FORT_SUPPORT2_FEE;

	// ********************************
	public static void loadEntitiesConfig()
	{
		_log.info("Loading: " + ENTITIES_FILE);
		try
		{
			Properties entitiesSettings = new L2Properties("./" + ENTITIES_FILE);

			CS_TELE_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("CastleTeleportFunctionFeeRatio", "604800000"));
			CS_TELE1_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleTeleportFunctionFeeLvl1", "7000"));
			CS_TELE2_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleTeleportFunctionFeeLvl2", "14000"));
			CS_SUPPORT_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("CastleSupportFunctionFeeRatio", "86400000"));
			CS_SUPPORT1_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleSupportFeeLvl1", "7000"));
			CS_SUPPORT2_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleSupportFeeLvl2", "21000"));
			CS_SUPPORT3_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleSupportFeeLvl3", "37000"));
			CS_SUPPORT4_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleSupportFeeLvl4", "52000"));
			CS_MPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("CastleMpRegenerationFunctionFeeRatio", "86400000"));
			CS_MPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleMpRegenerationFeeLvl1", "2000"));
			CS_MPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleMpRegenerationFeeLvl2", "6500"));
			CS_MPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleMpRegenerationFeeLvl3", "13750"));
			CS_MPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleMpRegenerationFeeLvl4", "20000"));
			CS_HPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("CastleHpRegenerationFunctionFeeRatio", "86400000"));
			CS_HPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleHpRegenerationFeeLvl1", "1000"));
			CS_HPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleHpRegenerationFeeLvl2", "1500"));
			CS_HPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleHpRegenerationFeeLvl3", "2250"));
			CS_HPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleHpRegenerationFeeLvl4", "3270"));
			CS_HPREG5_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleHpRegenerationFeeLvl5", "5166"));
			CS_EXPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("CastleExpRegenerationFunctionFeeRatio", "86400000"));
			CS_EXPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleExpRegenerationFeeLvl1", "9000"));
			CS_EXPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleExpRegenerationFeeLvl2", "15000"));
			CS_EXPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleExpRegenerationFeeLvl3", "21000"));
			CS_EXPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("CastleExpRegenerationFeeLvl4", "30000"));

			// ---------------- CH Configs ---------------------------------------------------------------------
			CH_TELE_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallTeleportFunctionFeeRatio", "604800000"));
			CH_TELE1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallTeleportFunctionFeeLvl1", "7000"));
			CH_TELE2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallTeleportFunctionFeeLvl2", "14000"));
			CH_SUPPORT_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallSupportFunctionFeeRatio", "86400000"));
			CH_SUPPORT1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl1", "2500"));
			CH_SUPPORT2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl2", "5000"));
			CH_SUPPORT3_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl3", "7000"));
			CH_SUPPORT4_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl4", "11000"));
			CH_SUPPORT5_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl5", "21000"));
			CH_SUPPORT6_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl6", "36000"));
			CH_SUPPORT7_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl7", "37000"));
			CH_SUPPORT8_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallSupportFeeLvl8", "52000"));
			CH_MPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallMpRegenerationFunctionFeeRatio", "86400000"));
			CH_MPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallMpRegenerationFeeLvl1", "2000"));
			CH_MPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallMpRegenerationFeeLvl2", "3750"));
			CH_MPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallMpRegenerationFeeLvl3", "6500"));
			CH_MPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallMpRegenerationFeeLvl4", "13750"));
			CH_MPREG5_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallMpRegenerationFeeLvl5", "20000"));
			CH_HPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallHpRegenerationFunctionFeeRatio", "86400000"));
			CH_HPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl1", "700"));
			CH_HPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl2", "800"));
			CH_HPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl3", "1000"));
			CH_HPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl4", "1166"));
			CH_HPREG5_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl5", "1500"));
			CH_HPREG6_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl6", "1750"));
			CH_HPREG7_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl7", "2000"));
			CH_HPREG8_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl8", "2250"));
			CH_HPREG9_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl9", "2500"));
			CH_HPREG10_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl10", "3250"));
			CH_HPREG11_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl11", "3270"));
			CH_HPREG12_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl12", "4250"));
			CH_HPREG13_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallHpRegenerationFeeLvl13", "5166"));
			CH_EXPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallExpRegenerationFunctionFeeRatio", "86400000"));
			CH_EXPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl1", "3000"));
			CH_EXPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl2", "6000"));
			CH_EXPREG3_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl3", "9000"));
			CH_EXPREG4_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl4", "15000"));
			CH_EXPREG5_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl5", "21000"));
			CH_EXPREG6_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl6", "23330"));
			CH_EXPREG7_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallExpRegenerationFeeLvl7", "30000"));
			CH_ITEM_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallItemCreationFunctionFeeRatio", "86400000"));
			CH_ITEM1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallItemCreationFunctionFeeLvl1", "30000"));
			CH_ITEM2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallItemCreationFunctionFeeLvl2", "70000"));
			CH_ITEM3_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallItemCreationFunctionFeeLvl3", "140000"));
			CH_CURTAIN_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallCurtainFunctionFeeRatio", "86400000"));
			CH_CURTAIN1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallCurtainFunctionFeeLvl1", "2000"));
			CH_CURTAIN2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallCurtainFunctionFeeLvl2", "2500"));
			CH_FRONT_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("ClanHallFrontPlatformFunctionFeeRatio", "259200000"));
			CH_FRONT1_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "1300"));
			CH_FRONT2_FEE = Integer.parseInt(entitiesSettings.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "4000"));

			// ---------------- Fort Configs ---------------------------------------------------------------------
			FORT_TELE_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("FortTeleportFunctionFeeRatio", "604800000"));
			FORT_TELE1_FEE = Integer.parseInt(entitiesSettings.getProperty("FortTeleportFunctionFeeLvl1", "1000"));
			FORT_TELE2_FEE = Integer.parseInt(entitiesSettings.getProperty("FortTeleportFunctionFeeLvl2", "10000"));
			FORT_SUPPORT_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("FortSupportFunctionFeeRatio", "86400000"));
			FORT_SUPPORT1_FEE = Integer.parseInt(entitiesSettings.getProperty("FortSupportFeeLvl1", "7000"));
			FORT_SUPPORT2_FEE = Integer.parseInt(entitiesSettings.getProperty("FortSupportFeeLvl2", "1700"));
			FORT_MPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("FortMpRegenerationFunctionFeeRatio", "86400000"));
			FORT_MPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("FortMpRegenerationFeeLvl1", "6500"));
			FORT_MPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("FortMpRegenerationFeeLvl2", "9300"));
			FORT_HPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("FortHpRegenerationFunctionFeeRatio", "86400000"));
			FORT_HPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("FortHpRegenerationFeeLvl1", "2000"));
			FORT_HPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("FortHpRegenerationFeeLvl2", "3500"));
			FORT_EXPREG_FEE_RATIO = Long.parseLong(entitiesSettings.getProperty("FortExpRegenerationFunctionFeeRatio", "86400000"));
			FORT_EXPREG1_FEE = Integer.parseInt(entitiesSettings.getProperty("FortExpRegenerationFeeLvl1", "9000"));
			FORT_EXPREG2_FEE = Integer.parseInt(entitiesSettings.getProperty("FortExpRegenerationFeeLvl2", "10000"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + ENTITIES_FILE + " File.");
		}
	}

	//==============================================================================================
	public static final String	SEVEN_SIGNS_FILE	= "./config/main/events/seven_signs.properties";
	//==============================================================================================
	public static boolean		ALT_GAME_CASTLE_DAWN;
	public static boolean		ALT_GAME_CASTLE_DUSK;
	public static boolean		ALT_GAME_REQUIRE_CLAN_CASTLE;							// Alternative gaming - allow clan-based castle ownage check rather than
	public static int			ALT_FESTIVAL_MIN_PLAYER;								// Minimum number of player to participate in SevenSigns Festival
	public static int			ALT_MAXIMUM_PLAYER_CONTRIB;							// Maximum of player contrib during Festival
	public static long			ALT_FESTIVAL_MANAGER_START;							// Festival Manager start time.
	public static long			ALT_FESTIVAL_LENGTH;									// Festival Length
	public static long			ALT_FESTIVAL_CYCLE_LENGTH;								// Festival Cycle Length
	public static long			ALT_FESTIVAL_FIRST_SPAWN;								// Festival First Spawn
	public static long			ALT_FESTIVAL_FIRST_SWARM;								// Festival First Swarm
	public static long			ALT_FESTIVAL_SECOND_SPAWN;								// Festival Second Spawn
	public static long			ALT_FESTIVAL_SECOND_SWARM;								// Festival Second Swarm
	public static long			ALT_FESTIVAL_CHEST_SPAWN;								// Festival Chest Spawn
	public static int			ALT_FESTIVAL_ARCHER_AGGRO;								// Aggro value of Archer in SevenSigns Festival
	public static int			ALT_FESTIVAL_CHEST_AGGRO;								// Aggro value of Chest in SevenSigns Festival
	public static int			ALT_FESTIVAL_MONSTER_AGGRO;							// Aggro value of Monster in SevenSigns Festival
	public static boolean		ANNOUNCE_MAMMON_SPAWN;
	public static boolean		ANNOUNCE_7S_AT_START_UP;
	public static boolean		ALT_STRICT_SEVENSIGNS;
	public static int			ALT_DAWN_JOIN_COST;									// Amount of adena to pay to join Dawn Cabal
	public static double		ALT_SIEGE_DAWN_GATES_PDEF_MULT;
	public static double		ALT_SIEGE_DUSK_GATES_PDEF_MULT;
	public static double		ALT_SIEGE_DAWN_GATES_MDEF_MULT;
	public static double		ALT_SIEGE_DUSK_GATES_MDEF_MULT;

	//***********************************************************************
	public static void loadSevenSignsConfig()
	{
		_log.info("Loading: " + SEVEN_SIGNS_FILE);
		try
		{
			Properties sevenSignsSettings = new L2Properties("./" + SEVEN_SIGNS_FILE);

			ALT_DAWN_JOIN_COST = Integer.parseInt(sevenSignsSettings.getProperty("AltJoinDawnCost", "50000"));
			ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(sevenSignsSettings.getProperty("AltCastleForDawn", "true"));
			ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(sevenSignsSettings.getProperty("AltCastleForDusk", "true"));
			ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(sevenSignsSettings.getProperty("AltRequireClanCastle", "false"));
			ALT_FESTIVAL_MIN_PLAYER = Integer.parseInt(sevenSignsSettings.getProperty("AltFestivalMinPlayer", "5"));
			ALT_MAXIMUM_PLAYER_CONTRIB = Integer.parseInt(sevenSignsSettings.getProperty("AltMaxPlayerContrib", "1000000"));
			ALT_FESTIVAL_MANAGER_START = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalManagerStart", "120000"));
			ALT_FESTIVAL_LENGTH = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalLength", "1080000"));
			ALT_FESTIVAL_CYCLE_LENGTH = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalCycleLength", "2280000"));
			ALT_FESTIVAL_FIRST_SPAWN = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalFirstSpawn", "120000"));
			ALT_FESTIVAL_FIRST_SWARM = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalFirstSwarm", "300000"));
			ALT_FESTIVAL_SECOND_SPAWN = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalSecondSpawn", "540000"));
			ALT_FESTIVAL_SECOND_SWARM = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalSecondSwarm", "720000"));
			ALT_FESTIVAL_CHEST_SPAWN = Long.parseLong(sevenSignsSettings.getProperty("AltFestivalChestSpawn", "900000"));
			ALT_FESTIVAL_ARCHER_AGGRO = Integer.parseInt(sevenSignsSettings.getProperty("AltFestivalArcherAggro", "200"));
			ALT_FESTIVAL_CHEST_AGGRO = Integer.parseInt(sevenSignsSettings.getProperty("AltFestivalChestAggro", "0"));
			ALT_FESTIVAL_MONSTER_AGGRO = Integer.parseInt(sevenSignsSettings.getProperty("AltFestivalMonsterAggro", "200"));
			ANNOUNCE_7S_AT_START_UP = Boolean.parseBoolean(sevenSignsSettings.getProperty("Announce7s", "true"));
			ALT_STRICT_SEVENSIGNS = Boolean.parseBoolean(sevenSignsSettings.getProperty("StrictSevenSigns", "true"));
			ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(sevenSignsSettings.getProperty("AnnounceMammonSpawn", "false"));
			ALT_SIEGE_DAWN_GATES_PDEF_MULT = Double.parseDouble(sevenSignsSettings.getProperty("AltDawnGatesPdefMult", "1.1"));
			ALT_SIEGE_DUSK_GATES_PDEF_MULT = Double.parseDouble(sevenSignsSettings.getProperty("AltDuskGatesPdefMult", "0.8"));
			ALT_SIEGE_DAWN_GATES_MDEF_MULT = Double.parseDouble(sevenSignsSettings.getProperty("AltDawnGatesMdefMult", "1.1"));
			ALT_SIEGE_DUSK_GATES_MDEF_MULT = Double.parseDouble(sevenSignsSettings.getProperty("AltDuskGatesMdefMult", "0.8"));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + SEVEN_SIGNS_FILE + " File.");
		}
	}

	//============================================================================================
	public static final String		PVP_CONFIG_FILE		= "./config/main/pvp_settings.properties";
	//============================================================================================
	public static float				KARMA_RATE;
	public static boolean			CURSED_WEAPON_NPC_INTERACT;
	public static int				KARMA_MIN_KARMA;
	public static int				KARMA_MAX_KARMA;
	public static int				KARMA_XP_DIVIDER;
	public static int				KARMA_PK_LIMIT;												//the minimum ammount of killed ppl to drop equips
	public static int				KARMA_LOST_BASE;
	public static int				PVP_NORMAL_TIME;												// Duration (in ms) while a player stay in PVP mode after hitting an innocent
	public static int				PVP_PVP_TIME;													// Duration (in ms) while a player stay in PVP mode after hitting a purple player
	public static int				PVP_TIME;
	public static boolean			KARMA_AWARD_PK_KILL;
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_USE_GK;								// Allow player with karma to use GK ?
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_TRADE;
	public static boolean			ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
	public static boolean			KARMA_DROP_GM;
	public static String			KARMA_NON_DROPPABLE_PET_ITEMS;
	public static String			KARMA_NON_DROPPABLE_ITEMS;
	public static FastList<Integer>	KARMA_LIST_NONDROPPABLE_PET_ITEMS	= new FastList<Integer>();
	public static FastList<Integer>	KARMA_LIST_NONDROPPABLE_ITEMS		= new FastList<Integer>();

	//******************************************************
	public static void loadPvpConfig()
	{
		_log.info("Loading: " + PVP_CONFIG_FILE);
		try
		{
			Properties pvpSettings = new L2Properties("./" + PVP_CONFIG_FILE);

			KARMA_RATE = Float.parseFloat(pvpSettings.getProperty("KarmaRate", "1."));
			CURSED_WEAPON_NPC_INTERACT = Boolean.parseBoolean(pvpSettings.getProperty("CursedWeaponNpcInteract", "false"));
			ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanUseGK", "false"));
			KARMA_MIN_KARMA = Integer.parseInt(pvpSettings.getProperty("MinKarma", "240"));
			KARMA_MAX_KARMA = Integer.parseInt(pvpSettings.getProperty("MaxKarma", "10000"));
			KARMA_XP_DIVIDER = Integer.parseInt(pvpSettings.getProperty("XpDivider", "260"));
			KARMA_DROP_GM = Boolean.parseBoolean(pvpSettings.getProperty("CanGMDropEquipment", "false"));
			KARMA_PK_LIMIT = Integer.parseInt(pvpSettings.getProperty("MinimumPKRequiredToDrop", "5"));
			KARMA_LOST_BASE = Integer.parseInt(pvpSettings.getProperty("BaseKarmaLost", "0"));
			KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pvpSettings.getProperty("AwardPKKillPVPPoint", "true"));
			PVP_NORMAL_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsNormalTime", "120000"));
			PVP_PVP_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsPvPTime", "60000"));
			PVP_TIME = PVP_NORMAL_TIME;
			ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanBeKilledInPeaceZone", "false"));
			ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanShop", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanTeleport", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanTrade", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(pvpSettings.getProperty("AltKarmaPlayerCanUseWareHouse", "true"));
			KARMA_NON_DROPPABLE_PET_ITEMS = pvpSettings.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
			KARMA_NON_DROPPABLE_ITEMS = pvpSettings.getProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,8181,5575,7694,9388,9389,9390");

			KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
			for (String id : KARMA_NON_DROPPABLE_PET_ITEMS.trim().split(","))
				KARMA_LIST_NONDROPPABLE_PET_ITEMS.add(Integer.parseInt(id.trim()));

			KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
			for (String id : KARMA_NON_DROPPABLE_ITEMS.trim().split(","))
				KARMA_LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id.trim()));
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + PVP_CONFIG_FILE + " File.");
		}
	}

	//===========================================================================================
	public static final String	GM_ACCESS_FILE	= "./config/administration/gm_access.properties";
	//==========================================================================================

	public static boolean		GM_STARTUP_INVISIBLE;
	public static boolean		GM_STARTUP_INVULNERABLE;
	public static boolean		GM_STARTUP_SILENCE;
	public static boolean		GM_STARTUP_AUTO_LIST;
	public static boolean		SHOW_GM_LOGIN;
	public static boolean		GM_ITEM_RESTRICTION;
	public static int			GM_MAX_ENCHANT;
	public static int			STANDARD_RESPAWN_DELAY;
	public static boolean		GM_AUDIT;
	public static int			GM_NAME_COLOR;
	public static int			GM_TITLE_COLOR;

	//******************************************************************
	public static void loadGmAccess()
	{
		_log.info("Loading: " + GM_ACCESS_FILE);
		try
		{
			Properties gmSettings = new L2Properties("./" + GM_ACCESS_FILE);
			GM_STARTUP_INVISIBLE = Boolean.parseBoolean(gmSettings.getProperty("GMStartupInvisible", "true"));
			GM_STARTUP_INVULNERABLE = Boolean.parseBoolean(gmSettings.getProperty("GMStartupInvulnerable", "false"));
			GM_STARTUP_SILENCE = Boolean.parseBoolean(gmSettings.getProperty("GMStartupSilence", "false"));
			GM_STARTUP_AUTO_LIST = Boolean.parseBoolean(gmSettings.getProperty("GMStartupAutoList", "false"));
			SHOW_GM_LOGIN = Boolean.parseBoolean(gmSettings.getProperty("ShowGMLogin", "false"));
			GM_ITEM_RESTRICTION = Boolean.parseBoolean(gmSettings.getProperty("GmItemRestriction", "false"));
			GM_MAX_ENCHANT = Integer.parseInt(gmSettings.getProperty("GMMaxEnchant", "65535"));
			STANDARD_RESPAWN_DELAY = Integer.parseInt(gmSettings.getProperty("StandardRespawnDelay", "60"));			
			GM_AUDIT = Boolean.parseBoolean(gmSettings.getProperty("GMAudit", "false"));
			GM_NAME_COLOR = Integer.decode("0x" + gmSettings.getProperty("GmNameColor", "00FF33"));
			GM_TITLE_COLOR = Integer.decode("0x" + gmSettings.getProperty("GmTitleColor", "FF0000"));
			
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new Error("Failed to Load " + GM_ACCESS_FILE + " File.");
		}
	}
	
	//=======================================================================================================
	public static final String	SIEGE_CONFIGURATION_FILE	= "./config/main/events/castle_siege.properties";
	//=======================================================================================================
	public static int			SIEGE_MAX_ATTACKER;
	public static int			SIEGE_MAX_DEFENDER;
	public static int			SIEGE_BLOODALIANCE_REWARD_CNT;
	public static int			SIEGE_RESPAWN_DELAY_ATTACKER;
	public static int			SIEGE_FLAG_MAX_COUNT;
	public static int			SIEGE_CLAN_MIN_LEVEL;
	public static int			SIEGE_LENGTH_MINUTES;
	public static int			SIEGE_CLAN_MIN_MEMBERCOUNT;
	public static boolean		SPAWN_SIEGE_GUARD;
	public static boolean		SIEGE_ONLY_REGISTERED;
	public static boolean		ALT_FLYING_WYVERN_IN_SIEGE;
	public static boolean		SIEGE_GATE_CONTROL;
	public static boolean		CHANGE_SIEGE_TIME_IS_DISABLES;
	public static boolean		CORECT_SIEGE_DATE_BY_7S;
	public static FastList<String> CL_SET_SIEGE_TIME_LIST;
	public static FastList<Integer> SIEGE_HOUR_LIST_MORNING;
	public static FastList<Integer> SIEGE_HOUR_LIST_AFTERNOON;
	public static int			MAX_GUARD_COUNT_FOR_CASTLE;
	public static int 			CASTLE_REWARD_ID;
	public static int 			CASTLE_REWARD_COUNT;

	//*****************************************************
	public static void loadSiegeConfig()
	{
		_log.info("Loading: " + SIEGE_CONFIGURATION_FILE);
		try
		{
			Properties siegeSettings = new L2Properties("./" + SIEGE_CONFIGURATION_FILE);

			SIEGE_ONLY_REGISTERED = Boolean.parseBoolean(siegeSettings.getProperty("OnlyRegistered", "true"));
			ALT_FLYING_WYVERN_IN_SIEGE = Boolean.parseBoolean(siegeSettings.getProperty("AltFlyingWyvernInSiege", "false"));
			SPAWN_SIEGE_GUARD = Boolean.parseBoolean(siegeSettings.getProperty("SpawnSiegeGuard", "true"));
			SIEGE_GATE_CONTROL = Boolean.parseBoolean(siegeSettings.getProperty("AllowGateControl", "false"));
			SIEGE_CLAN_MIN_MEMBERCOUNT = Integer.parseInt(siegeSettings.getProperty("SiegeClanMinMembersCount", "1"));
			SIEGE_MAX_ATTACKER = Integer.parseInt(siegeSettings.getProperty("AttackerMaxClans", "500"));
			SIEGE_MAX_DEFENDER = Integer.parseInt(siegeSettings.getProperty("DefenderMaxClans", "500"));
			SIEGE_BLOODALIANCE_REWARD_CNT = Integer.parseInt(siegeSettings.getProperty("BloodAllianceReward", "1"));
			SIEGE_RESPAWN_DELAY_ATTACKER = Integer.parseInt(siegeSettings.getProperty("AttackerRespawn", "30000"));
			SIEGE_FLAG_MAX_COUNT = Integer.parseInt(siegeSettings.getProperty("MaxFlags", "1"));
			SIEGE_CLAN_MIN_LEVEL = Integer.parseInt(siegeSettings.getProperty("SiegeClanMinLevel", "5"));
			SIEGE_LENGTH_MINUTES = Integer.parseInt(siegeSettings.getProperty("SiegeLength", "120"));

			CHANGE_SIEGE_TIME_IS_DISABLES = Boolean.parseBoolean(siegeSettings.getProperty("DisableChangeSiegeTime", "false"));
			CORECT_SIEGE_DATE_BY_7S = Boolean.parseBoolean(siegeSettings.getProperty("CorrectDateBy7s", "true"));
			CL_SET_SIEGE_TIME_LIST = new FastList<String>();
			SIEGE_HOUR_LIST_MORNING = new FastList<Integer>();
			SIEGE_HOUR_LIST_AFTERNOON = new FastList<Integer>();
			String[] sstl = siegeSettings.getProperty("CLSetSiegeTimeList", "").split(",");
			if (sstl.length != 0)
			{
				boolean isHour = false;
				for (String st : sstl)
				{
					if (st.equalsIgnoreCase("day") || st.equalsIgnoreCase("hour") || st.equalsIgnoreCase("minute"))
					{
						if (st.equalsIgnoreCase("hour"))
							isHour = true;
						CL_SET_SIEGE_TIME_LIST.add(st.toLowerCase());
					}
					else
						_log.info("[CLSetSiegeTimeList]: invalid config property -> CLSetSiegeTimeList \"" + st + "\"");
				}
				if (isHour)
				{
					String[] shl = siegeSettings.getProperty("SiegeHourList", "").split(",");
					for (String st : shl)
					{
						if (!st.equalsIgnoreCase(""))
						{
							int val = Integer.valueOf(st);
							if (val > 23 || val < 0)
								_log.info("[SiegeHourList]: invalid config property -> SiegeHourList \"" + st + "\"");
							else if (val < 12)
								SIEGE_HOUR_LIST_MORNING.add(val);
							else
							{
								val -= 12;
								SIEGE_HOUR_LIST_AFTERNOON.add(val);
							}
						}
					}
					if (Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty() && Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty())
					{
						_log.info("[SiegeHourList]: invalid config property -> SiegeHourList is empty");
						CL_SET_SIEGE_TIME_LIST.remove("hour");
					}
				}
			}
			MAX_GUARD_COUNT_FOR_CASTLE = Integer.parseInt(siegeSettings.getProperty("MaxGuardCount", "400"));
			CASTLE_REWARD_ID = Integer.parseInt(siegeSettings.getProperty("RewardID","0"));
			CASTLE_REWARD_COUNT = Integer.parseInt(siegeSettings.getProperty("RewardCount","0"));

		}
		catch (Exception e)
		{
			_log.error(e);
			throw new Error("Failed to Load " + SIEGE_CONFIGURATION_FILE + " File.");
		}
	}

	public static final String PC_CAFFE_FILE = "./config/main/events/pccaffe.properties";
	public static boolean PC_CAFFE_ENABLED;
	public static int PC_CAFFE_MIN_LEVEL;
	public static int PC_CAFFE_MAX_LEVEL;
	public static int PC_CAFFE_MIN_SCORE;
	public static int PC_CAFFE_MAX_SCORE;
	public static int PC_CAFFE_INTERVAL;
	public static void loadPCCaffe() {
		_log.info("Loading: " + PC_CAFFE_FILE);
		try {
			Properties p = new L2Properties(PC_CAFFE_FILE);
			PC_CAFFE_ENABLED = Boolean.parseBoolean(p.getProperty("PCCaffeEnabled","true"));
			PC_CAFFE_INTERVAL = Integer.parseInt(p.getProperty("PCCafeInterval","10"));
			PC_CAFFE_MIN_LEVEL =Integer.parseInt(p.getProperty("PCCafeMinLevel","20"));
			PC_CAFFE_MAX_LEVEL =Integer.parseInt(p.getProperty("PCCafeMaxLevel","85"));
			PC_CAFFE_MIN_SCORE = Integer.parseInt(p.getProperty("PCCafeMinScore","0"));
			PC_CAFFE_MAX_SCORE = Integer.parseInt(p.getProperty("PCCafeMaxScore","10"));
		} catch(Exception e) {
			_log.error(e);
			throw new Error("Failed to Load " + PC_CAFFE_FILE+" file");
		}
	}
	
	//=============================================================================================
	public static final String	EQUIP_CONFIGURATION_FILE	= "./config/main/equipment.properties";

	public static final int GMSHOP_BUFF_HP = 0;

	public static boolean ADVANCED_DIAGONAL_STRATEGY = true;
	
	//=============================================================================================
	public static boolean		ONLY_CLANLEADER_CAN_SIT_ON_THRONE;

	//************************************************
	public static void loadEquipmentConfig()
	{
		_log.info("Loading: " + EQUIP_CONFIGURATION_FILE);
		try
		{
			Properties equipSettings = new L2Properties("./" + EQUIP_CONFIGURATION_FILE);

			ONLY_CLANLEADER_CAN_SIT_ON_THRONE = Boolean.parseBoolean(equipSettings.getProperty("OnlyClanleaderCanSitOnThrone", "false"));
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new Error("Failed to Load " + EQUIP_CONFIGURATION_FILE + " File.");
		}
	}

	public static int		FACTION_KILL_RATE	= 1000;
	public static int		FACTION_QUEST_RATE	= 1;
	public static boolean	FACTION_ENABLED		= false;
	public static boolean	FACTION_KILL_REWARD	= false;

	public static void loadAll()
	{
		// loads Network config files
		Console.printSection("Network Configuration");
		loadNetworkConfiguration();

		// loads Main config files
		Console.printSection("Main Configuration");
		loadMainConfig();
		loadSafe();

		// loads Events config files
		Console.printSection("Events Configuration");
		loadEventsConfig();

		// loads Chat config files
		Console.printSection("Chat Configuration");
		loadChatConfig();

		// loads Filters config files
		Console.printSection("Filters Configuration");
		loadFiltersConfig();

		// loads Administration config files
		Console.printSection("Administration Configuration");
		loadAdministrationConfig();

		// loads Developer config files
		loadDevConfig();

		Console.printSection("Announment Configuration");
		loadAnnounces();
		// loads Mods config files
		Console.printSection("Mods Configuration");
		loadModsConfig();
		loadClassProperties();
		loadArmors();
		loadBanking();
		

	}

	public static void loadMainConfig()
	{
		loadAreasConfig();
		loadBanConfig();
		loadDateTimeConfig();
		loadGsConfig();
		loadRespawnsConfig();
		loadRegenSettings();
		loadPetitionSettings();
		loadAltConfig();
		loadCustomConfig();
		loadClansConfig();
		loadEntitiesConfig();
		loadCbConfig();
		loadNicksConfig();
		loadDropsConfig();
		loadEnchantConfig();
		loadPartyConfig();
		loadOptionsConfig();
		loadOtherConfig();
		loadGeoConfig();
		loadWhConfig();
		loadPermissionsConfig();
		loadPvpConfig();
		loadServicesConfig();
		loadSecurityConfig();
		loadRatesConfig();
		loadSkillsConfig();
		loadInventoryConfig();
		loadPvtStoresConfig();
		loadL2topConfig();
		loadManorConfig();
		loadEquipmentConfig();
		loadGridConfig();
		loadIdFactoryConfig();
		if(!ReloadHandler.getInstance().isRegistred("config")) {
			ReloadHandler.getInstance().registerHandler("config", _reloadAll);
		}
	}

	private static IReloadHandler _reloadAll = new IReloadHandler() {
		@Override
		public void reload(L2PcInstance actor) {
			loadMainConfig();
		}
		
	};

	public static int GMSHOP_BUFF_ITEM = 57;
	public static int GMSHOP_BUFF_REMOVE = 100;
	public static int GMSHOP_BUFF_CP = 100;
	public static int GMSHOP_BUFF_MP = 100;
	public static String BUFFER_RESTRICTION;
	public static int BUFFER_RESTORE_DELAY;
	public static boolean BUFFER_ENABLED;
	public static boolean BUFFER_ANIMATION; 
	
	public static void loadBufferConfig() {
		try {
			L2Properties p = new L2Properties("./config/mods/buffer.properties");
			BUFFER_ENABLED = Boolean.parseBoolean(p.getProperty("Enabled","true"));
			GMSHOP_BUFF_ITEM = Integer.parseInt(p.getProperty("BufferPriceItem", "57"));
			GMSHOP_BUFF_REMOVE = Integer.parseInt(p.getProperty("BufferRemoveBuffPrice", "100"));
			GMSHOP_BUFF_CP = Integer.parseInt(p.getProperty("BufferRestoreCPHPPrice", "100"));
			GMSHOP_BUFF_MP = Integer.parseInt(p.getProperty("BufferRestoreMPPrice", "100"));
			BUFFER_RESTRICTION = p.getProperty("BufferRestrictedWhen","");
			BUFFER_RESTORE_DELAY = Integer.parseInt(p.getProperty("BufferRestoreDelay","0"));
			 BUFFER_ANIMATION = Boolean.parseBoolean(p.getProperty("BufferAnimation","false")); 
		} catch(Exception e) {
			_log.warn("GameSever: Failed to Load ./config/mods/buffer.properties File.");
		}
	}

	public static float LOW_WEIGHT = 0.5f;
	public static float MEDIUM_WEIGHT = 2.0f;
	public static float HIGH_WEIGHT = 3.0f;
	public static float DIAGONAL_WEIGHT = 0.707f;
	public static int MAX_POSTFILTER_PASSES = 3;
	public static String  PATHFIND_BUFFERS = "100x6;128x6;192x6;256x4;320x4;384x4;500x2";
	public static int WORLD_X_MIN = 15;
	public static int WORLD_X_MAX = 26;
	public static byte WORLD_Y_MIN = 10;
	public static byte WORLD_Y_MAX = 26;
	public static int INTEREST_MAX_THREAD=10;
	public static long PROTECT_COMPRESSION_WRITEDELAY = 2L;
	public static int GENERAL_PACKET_THREAD_CORE_SIZE = 4;

	public static native void checkConfig(Object config);
	
	public static void loadEventsConfig()
	{
		loadSevenSignsConfig();
		loadSiegeConfig();
		loadOlympiadConfig();
		loadLotteryConfig();
		loadFortSiegeConfig();
		loadPCCaffe();
	}

	public static void loadNetworkConfiguration()
	{
		loadNetworkConfig();
		loadHexidConfig();
	}

	public static void loadFiltersConfig()
	{
		loadFilter();
	}

	public static void loadAdministrationConfig()
	{
		loadGmAccess();
	}

	public static void loadModsConfig()
	{
		loadChampionsConfig();
		loadJailConfig();
		loadWeddingConfig();
		loadFunEventsConfig();
		loadClassMasterConfig();
		loadBufferConfig();
	}

	public static void loadChatConfig()
	{
		loadChatConfiguration();
	}



	public static class ClassMasterSettings
	{
		private FastMap<Integer, FastMap<Integer, Integer>>	_claimItems;
		private FastMap<Integer, FastMap<Integer, Integer>>	_rewardItems;
		private FastMap<Integer, Boolean>					_allowedClassChange;

		public ClassMasterSettings(String _configLine)
		{
			_claimItems = new FastMap<Integer, FastMap<Integer, Integer>>();
			_rewardItems = new FastMap<Integer, FastMap<Integer, Integer>>();
			_allowedClassChange = new FastMap<Integer, Boolean>();
			if (_configLine != null)
				parseConfigLine(_configLine.trim());
		}

		private void parseConfigLine(String _configLine)
		{
			StringTokenizer st = new StringTokenizer(_configLine, ";");

			while (st.hasMoreTokens())
			{
				// get allowed class change
				int job = Integer.parseInt(st.nextToken());

				_allowedClassChange.put(job, true);

				FastMap<Integer, Integer> _items = new FastMap<Integer, Integer>();
				// parse items needed for class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");

					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}

				_claimItems.put(job, _items);

				_items = new FastMap<Integer, Integer>();
				// parse gifts after class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");

					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}

				_rewardItems.put(job, _items);
			}
		}

		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
				return false;
			if (_allowedClassChange.containsKey(job))
				return _allowedClassChange.get(job);
			return false;
		}

		public FastMap<Integer, Integer> getRewardItems(int job)
		{
			if (_rewardItems.containsKey(job))
				return _rewardItems.get(job);
			return null;
		}

		public FastMap<Integer, Integer> getRequireItems(int job)
		{
			if (_claimItems.containsKey(job))
				return _claimItems.get(job);
			return null;
		}
	}

	public static void saveHexid(int serverId, String hexId)
	{
		try
		{
			Properties hexSetting = new L2Properties();
			File file = new File(HEXID_FILE);
			//Create a new empty file only if it doesn't exist
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			_log.warn("Failed to save hex id to " + HEXID_FILE + " File.");
		}
	}

	public static void loadFilter()
	{
		if (!Config.USE_SAY_FILTER)
		{
			_log.info("Chat Filter: Error, filter disabled.");
			return;
		}

		File file = new File("config/chat/sayfilter.txt");
		try
		{
			BufferedReader fread = new BufferedReader(new FileReader(file));
			
			String line = null;
			while((line = fread.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				FILTER_LIST.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE));
			}
			fread.close();
			_log.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
		}
		catch (FileNotFoundException e)
		{
			_log.info("Chat Filter: Error, file not found.");
		}
		catch (IOException e)
		{
			_log.info("Chat Filter: Error while reading sayfilter.txt.");
		}
	}

	public static void unallocateFilterBuffer()
	{
		_log.info("Cleaning Chat Filter..");
		FILTER_LIST.clear();
	}

	public static boolean setParameterValue(String name, String value) {
		return false;
	}

	
	public static final String	CLASS_FILE = "./config/mods/classes.properties";
	public static Map<String, Double> _classModifiers = new FastMap<String, Double>().setShared(true);
	public static void loadClassProperties() {
		_log.info("Loading: " + CLASS_FILE);
		_classModifiers.clear();
		try {
			L2Properties p = new L2Properties(CLASS_FILE);
			for(Object o : p.keySet()) try {
				_classModifiers.put(o.toString(), Double.parseDouble(p.getProperty(o.toString())));
			} catch(NumberFormatException e) { }
		} catch(Exception e) {
			_log.error("Error loading "+CLASS_FILE);
			e.printStackTrace();
		}
	}
	public static final String	ARMOR_FILE = "./config/mods/classes_armor.properties";
	private static void loadArmors() {
		_log.info("Loading: " + ARMOR_FILE);
		try {
			L2Properties p = new L2Properties(ARMOR_FILE);
			for(Object o : p.keySet()) try {
				List<String> list = new FastList<String>();
				_armors.put(o.toString(),list);
				String [] s = p.getProperty(o.toString()).split(",");
				for(String a : s) 
					 list.add(a.trim().toLowerCase());
			} catch(Exception e) { }
		} catch(Exception e) {
			_log.error("Error loading "+CLASS_FILE);
			e.printStackTrace();
		}
		
	}
	private static Map<String,List<String>> _armors = new FastMap<String, List<String>>();
	public static boolean isAllowArmor(L2PcInstance cha, L2Armor armor) {
		try {
			String [] classes = ClassTreeTable.getInstance().getParentClasses(cha.getClassId().ordinal());
			String aname = armor.getItemType().name().toLowerCase();
			if(aname.equals("none") || aname.equals("pet"))
				return true;
			
			for(String s : classes) {
				List<String> a = _armors.get(s);
				if(a!=null) {
					if(!a.contains("all") && !a.contains(aname))
						return false;
				}
			}
		} catch(Exception e) {}
		return true;
	}
	public static double getCharModifier(L2PcInstance cha, Stats param) {
		try {
		
		String [] classes = ClassTreeTable.getInstance().getParentClasses(cha.getClassId().ordinal());
		L2Weapon weapon = cha.getActiveWeaponItem();
		Double val=null;
		if(weapon!=null) {
			for(String s : classes ) {
				
		 		val = _classModifiers.get(s+"."+weapon.getItemId()+"."+param.getValue());
		 		if(val!=null) break;
			}
			if(val!=null)
				return val;
			for(String s : classes ) {
		 		val = _classModifiers.get(s+"."+weapon.getItemType().shortName() +"."+param.getValue());
		 		if(val!=null) break;
			}
			
		 	if(val!=null) {
		 		return val;
		 	}
		}
		for(String s : classes ) {
		 		val = _classModifiers.get(s+"."+param.getValue());
		 		if(val!=null) {
		 			return val;
		 		}
		}
		return 1.0;
		} catch(Exception e) {
			return 1.0;
		}
	}
	public static final String	RUNE_FILE = "./config/mods/runes.properties";
	
	public static final String	BANKING_FILE	= "./config/mods/banking.properties";
	public static boolean BANKING_ENABLED;
	public static int BANKING_GOLDBAR_PRICE;
	public static int BANKING_GOLDBAR_ID;

	public static int MAX_Z_DIFF = 64;

	public static short MIN_LAYER_HEIGHT = 64;

	public static String GEOFILES_PATTERN = "(\\d{2}_\\d{2})\\.l2j";
	
	
	public static void loadBanking() {
		try {
			_log.info("Loading "+BANKING_FILE);
			L2Properties p = new L2Properties(BANKING_FILE);
			BANKING_ENABLED = Boolean.parseBoolean(p.getProperty("Enabled","true"));
			BANKING_GOLDBAR_PRICE = Integer.parseInt(p.getProperty("GoldBarPrice","250000000"));
			BANKING_GOLDBAR_ID = Integer.parseInt(p.getProperty("GoldBarId","3470"));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public static String SAFE_FILE = "./config/main/safe.properties";
	public static boolean RESET_TO_BASE = true;

	
	public static void loadSafe() {
		try {
			_log.info("Loading "+SAFE_FILE);
			L2Properties p = new L2Properties(SAFE_FILE);
			RESET_TO_BASE = Boolean.parseBoolean(p.getProperty("ResetToBaseCalssIfFail","true"));
		} catch(Exception e) {
			
		}
		
	}
}
