package ru.catssoftware.gameserver;

import com.lameguard.LameGuard;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.datatables.*;
import ru.catssoftware.gameserver.datatables.xml.*;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.geodata.pathfinding.PathFinding;
import ru.catssoftware.gameserver.gmaccess.gmCache;
import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.handler.*;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.*;
import ru.catssoftware.gameserver.instancemanager.games.fishingChampionship;
import ru.catssoftware.gameserver.instancemanager.grandbosses.*;
import ru.catssoftware.gameserver.instancemanager.lastimperialtomb.LastImperialTombManager;
import ru.catssoftware.gameserver.instancemanager.leaderboards.ArenaManager;
import ru.catssoftware.gameserver.instancemanager.leaderboards.FishermanManager;
import ru.catssoftware.gameserver.mmocore.SelectorConfig;
import ru.catssoftware.gameserver.mmocore.SelectorThread;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.Hero;
import ru.catssoftware.gameserver.model.entity.events.*;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.quest.QuestMessage;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.network.IOFloodManager;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.L2GamePacketHandler;
import ru.catssoftware.gameserver.network.daemons.SuperDeamon;
import ru.catssoftware.gameserver.script.CoreScriptsLoader;
import ru.catssoftware.gameserver.script.ExtensionLoader;
import ru.catssoftware.gameserver.scripting.L2ScriptEngineManager;
import ru.catssoftware.gameserver.taskmanager.*;
import ru.catssoftware.gameserver.taskmanager.tasks.TaskPcCaffe;
import ru.catssoftware.gameserver.threadmanager.DeadlockDetector;
import ru.catssoftware.gameserver.util.PcAction;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.protection.CatsGuard;
import ru.catssoftware.util.Console;
import ru.catssoftware.util.concurrent.RunnableStatsManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;


public class L2GameServer
{
	private static final Logger						_log				= Logger.getLogger(L2GameServer.class);
	private static final Calendar					_serverStarted		= Calendar.getInstance();
	private static SelectorThread<L2GameClient>		_selectorThread;
	public static long								_upTime				= 0;
	public static double							_intialTime			= 0;
	
	public L2GameServer() throws Throwable
	{

		/* Предстартовая подготовка */
		prepare();
		
		/* Загрузка чат фильтра */
		Console.printSection("Chat Filter");
		Config.loadFilter();
		Message.load();

		/* Инициализация базы данных */
		Console.printSection("Database Engine");
		L2DatabaseFactory.getInstance();
		
		PcAction.clearRestartTask();

		/* Вывод системной инфы */
		Console.printSection("System Info");
		Util.printGeneralSystemInfo();
		Console.printSection("Scripting Engines");
		L2ScriptEngineManager.getInstance();
		/* Инициализация пулов */
		Console.printSection("ThreadPool Manager");
		ThreadPool();
		
		/* Установка основго мира */
		Console.printSection("Lineage 2 World");
		ServerData.getInstance();
		L2World.getInstance();
		
		/* Запуск дедлок детектора */
		Console.printSection("DeadLock Detector");
		if (Config.DEADLOCKCHECK_INTERVAL > 0)
			DeadlockDetector.getInstance();
		else
			_log.info("DeadlockDetector: Manager is disabled");

			
		/* Создание карт мира */
		Console.printSection("MapRegion Manager");
		MapRegionManager.getInstance();

		/* Загрузка анонсов */
		Console.printSection("Announce Manager");
		Announcements.getInstance();

		/* ID Factory менеджер */
		Console.printSection("ID Factory Manager");
		if (!IdFactory.getInstance().isInitialized())
			_log.fatal("IdFactory: Could not read object IDs from DB");
		_log.info("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());

		/* Дополнительные треды */
		RunnableStatsManager.getInstance();

		/* Запуск движка геодаты */
		Console.printSection("Geodata Engine");
		GeoData.getInstance();
		PathFinding.getInstance();
		/* Загрузка статических объектов */
		Console.printSection("Static Objects");
		StaticObjects.getInstance();

		/* Сервер менеджер, основные классы */
		Console.printSection("Server Manager");
		if (Config.ALT_ALLOW_AWAY_STATUS)
			AwayManager.getInstance();
		GameTimeController.getInstance();
		BoatManager.getInstance();
		InstanceManager.getInstance();
		
		
		
		/* Старт серверных задач */
		Console.printSection("TaskManagers");
		AttackStanceTaskManager.getInstance();
		DecayTaskManager.getInstance();
		KnownListUpdateTaskManager.getInstance();
		LeakTaskManager.getInstance();
		SQLQueue.getInstance();

		/* Таблица телепортов */
		Console.printSection("Teleport Table");
		TeleportLocationTable.getInstance();

		/* Загрузка скилов */
		Console.printSection("Skills");
		SkillTreeTable.getInstance();
		SkillTable.getInstance();
		AdditionalSkillTable.getInstance();
		ResidentialSkillTable.getInstance();
		PetSkillsTable.getInstance();
		NobleSkillTable.getInstance();
		HeroSkillTable.getInstance();

		/* Загрущка итемов */
		Console.printSection("Items");
		ItemTable.getInstance();
		ArmorSetsTable.getInstance();
		AugmentationData.getInstance();
		if (Config.SP_BOOK_NEEDED)
			SkillSpellbookTable.getInstance();
		SummonItemsData.getInstance();
		ExtractableItemsData.getInstance();
		if (Config.ALLOW_FISHING)
			FishTable.getInstance();
		ItemsOnGroundManager.getInstance();
		if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
			ItemsAutoDestroy.getInstance();
		EnchantHPBonusData.getInstance();

		/* Кэш  HTML */
		HtmCache.getInstance();

		/* Данные персонажей */
		Console.printSection("Characters");
		CharNameTable.getInstance();
		CharTemplateTable.getInstance();
		LevelUpData.getInstance();
		HelperBuffTable.getInstance();
		HennaTable.getInstance();
		HennaTreeTable.getInstance();
		if (Config.ALLOW_WEDDING)
			CoupleManager.getInstance();
		ClanTable.getInstance();
		CrestCache.getInstance();
		Hero.getInstance();
		BlockListManager.getInstance();
		
		/* Загрузка всех НПЦ */
		Console.printSection("NPC Stats");
		NpcTable.getInstance();
		NpcLikePcTemplates.getInstance();
		PetDataTable.getInstance().loadPetsData();
		
		/* Автоспавн и авто чат */
		Console.printSection("Auto Handlers");
		AutoChatHandler.getInstance();
		AutoSpawnManager.getInstance();
		
		/* Семь печатей */
		Console.printSection("Seven Signs");
		SevenSigns.getInstance();
		SevenSignsFestival.getInstance();
		
		/* Замки, форты, зоны */
		Console.printSection("Entities and zones");
		CrownManager.getInstance();
		TownManager.getInstance();
		ClanHallManager.getInstance();
		DoorTable.getInstance();
		CastleManager.getInstance();
		SiegeManager.getInstance().load();
		FortManager.getInstance();
		FortSiegeManager.getInstance().load();
		ZoneManager.getInstance();
		MercTicketManager.getInstance();
		DoorTable.getInstance().registerToClanHalls();
		DoorTable.getInstance().setCommanderDoors();

		/* Осады элитных КХ */
		Console.printSection("Clan Hall Siege");
		FortResistSiegeManager.load();
		BanditStrongholdSiege.load();
		DevastatedCastleSiege.load();
		FortressOfDeadSiege.load();
		WildBeastFarmSiege.load();
		RainbowSpringSiege.load();
		// make sure that all the scheduled siege dates are in the Seal Validation period
		for (Castle castle : CastleManager.getInstance().getCastles().values())
			castle.getSiege().correctSiegeDateTime();
		
		/* Квесты */
		
		Console.printSection("Events/Script/CoreScript/Engine");
		QuestManager.getInstance();
		CoreScriptsLoader.Register();
		try
		{
			L2ScriptEngineManager.getInstance().loadScripts();
			
		}
		catch (IOException ioe)
		{
			_log.fatal("Failed loading scripts, no script going to be loaded");
		}
		QuestManager.getInstance().report();
		EventsDropManager.getInstance();
		EventDroplist.getInstance();
		if (Config.ARENA_ENABLED)
			ArenaManager.getInstance().engineInit();
		if (Config.FISHERMAN_ENABLED)
			FishermanManager.getInstance().engineInit();
		if (Config.SHOW_NOT_REG_QUEST)
			QuestMessage.showNotRegQuest();
		Console.printSection("HTML");
		_log.info(HtmCache.getInstance());

		/* Спавн, запуск спавна */
		Console.printSection("Spawns");
		SpawnTable.getInstance();
		if (Config.JAIL_SPAWN_SYSTEM)
			JailSpawnTable.getInstance().loadJailSpawns();
		if (Config.ALLOW_WEDDING)
			PcAction.spawnManager();
		DayNightSpawnManager.getInstance().notifyChangeMode();
		RaidBossSpawnManager.getInstance();
		RaidPointsManager.init();
		AutoChatHandler.getInstance();
		AutoSpawnManager.getInstance();
		
		/* Экономика */
		Console.printSection("Economy");
		CursedWeaponsManager.getInstance();
		TradeListTable.getInstance();
		CastleManorManager.getInstance();
		L2Manor.getInstance();
		AuctionManager.getInstance();
		TimedItemControl.getInstance();
		PartyRoomManager.getInstance();
		
		/* Олимпиада */
		Console.printSection("Olympiad");
		Olympiad.getInstance();
		
		Console.printSection("DimensionalRift");
		DimensionalRiftManager.getInstance();
		
		Console.printSection("FourSepulchers");
		FourSepulchersManager.getInstance().init();
		
		Console.printSection("Bosses");
		QueenAntManager.getInstance().init();
		ZakenManager.getInstance().init();
		CoreManager.getInstance().init();
		OrfenManager.getInstance().init();
		SailrenManager.getInstance().init();
		VanHalterManager.getInstance().init();
		
		Console.printSection("GrandBosses");
		AntharasManager.getInstance().init();
		BaiumManager.getInstance().init();
		ValakasManager.getInstance().init();
		LastImperialTombManager.getInstance().init();
		FrintezzaManager.getInstance().init();
		
		Console.printSection("Factions Manager");
		if (Config.FACTION_ENABLED)
		{
			FactionManager.getInstance();
			FactionQuestManager.getInstance();
		}
		else
			_log.info("Faction Manager: disabled.");
		
		Console.printSection("Handlers");
		ItemHandler.getInstance();
		SkillHandler.getInstance();
		UserCommandHandler.getInstance();
		VoicedCommandHandler.getInstance();
		ChatHandler.getInstance();
		
		Console.printSection("Misc");
		ObjectRestrictions.getInstance();
		L2SiegeStatus.getInstance();
		TaskManager.getInstance();
		GmListTable.getInstance();
		PetitionManager.getInstance();
		if (Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL > 0)
			OnlinePlayers.getInstance();
		CommunityBoard.getInstance();
		TimedItemControl.getInstance();
		fishingChampionship.getInstance();

		Console.printSection("Offline Service");
		if (Config.ALLOW_OFFLINE_TRADE)
			OfflineManager.getInstance();
		if (Config.RESTORE_OFFLINE_TRADERS)
			L2PcOffline.loadOffliners();
		else
			L2PcOffline.clearOffliner();

		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		System.gc();

		Console.printSection("ServerThreads");
		LoginServerThread.getInstance().start();
		L2GamePacketHandler gph = new L2GamePacketHandler();

		final SelectorConfig<L2GameClient> sc = new SelectorConfig<L2GameClient>(gph);

		_selectorThread = new SelectorThread<L2GameClient>(sc, gph, gph, gph, IOFloodManager.getInstance());
		_selectorThread.openServerSocket(InetAddress.getByName(Config.GAMESERVER_HOSTNAME), Config.PORT_GAME);
		_selectorThread.start();

		Console.printSection("Daemons");
		SuperDeamon.getInstance();
		if(Config.PC_CAFFE_ENABLED)
			new TaskPcCaffe().schedule(Config.PC_CAFFE_INTERVAL*60000);
		

		Console.printSection("Events");
		GameEventManager.getInstance();
		Console.printSection("Mods");
		Cristmas.startEvent();
        EventMedals.startEvent();
        StarlightFestival.startEvent();
		L2day.startEvent();
		BigSquash.startEvent();

		Console.printSection("Gm System");
		gmController.getInstance();
		gmCache.getInstance();

		Console.printSection("Extensions");
		ExtensionLoader.getInstance();

		CatsGuard.getInstance();

		try
		{
			Class<?> clazz = Class.forName("com.lameguard.LameGuard");
			if(clazz!=null) {
				File f = new File("./lameguard/lameguard.properties");
				if(f.exists()) {
					Console.printSection("LameGuard");
					LameGuard.main(new String []{"ru.catssoftware.protection.LameStub"});
				}
			}
		} catch(Exception e) {
		}
		Console.printSection("Tasks Manager");
		TaskManager.getInstance().startAllTasks();
		onStartup();
        System.gc();

		printInfo();
		
	}

	private static int[] avalibleEvents = null;

	private static String _license = null;
	private static int _revision = 0;

	public static void checkServer(int[] check)
	{
		if (avalibleEvents == null)
			avalibleEvents = check;
	}
	public static boolean isAvalible(int id)
	{
		for(int event : avalibleEvents)
			if (event == id)
				return true;
		return false;
	}

	public static void setRevision(int revision)
	{
		_revision = revision;
	}

	public static int getRevision()
	{
		return _revision;
	}

	private static Set<StartupHook> _startupHooks = new HashSet<StartupHook>();
	
	public synchronized static void addStartupHook(StartupHook hook)
	{
		if (_startupHooks != null)
			_startupHooks.add(hook);
		else
			hook.onStartup();
	}

	private static void printInfo()
	{
		Console.printSection("Server Info");
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		double finalTime = System.currentTimeMillis();
		_log.info("Free memory: " + freeMem + " Mb of " + totalMem + " Mb");
		_log.info("Ready on IP: " + Config.EXTERNAL_HOSTNAME + ":" + Config.PORT_GAME + ".");
		_log.info("Max players: " + Config.MAXIMUM_ONLINE_USERS);
		_log.info("Load time: " + (int) ((finalTime - _intialTime) / 1000) + " Seconds.");

		Console.printSection("lucera2.ru");
		_log.info("License: " + _license + " for " + getRevision() + " rev.");
		_log.info("Build: " + L2PcInstance.checkClass() + " rev.");

		Console.printSection("");
		// set uptime
		_upTime = System.currentTimeMillis();
	}
	
	private synchronized static void onStartup()
	{
		final Set<StartupHook> startupHooks = _startupHooks;
		
		_startupHooks = null;
		
		for (StartupHook hook : startupHooks)
			hook.onStartup();
	}
	
	public interface StartupHook
	{
		public void onStartup();
	}

	public static void printMemUsage()
	{
		Console.printSection("Memory");
		for (String line : Util.getMemUsage())
			_log.info(line);
	}

	public static SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}

	public static Calendar getStartedTime()
	{
		return _serverStarted;
	}

	private void prepare()
	{
		
		System.setProperty("line.separator", "\r\n");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("python.home", ".");

		Console.printSection("Preparations");
		_log.info("Checking folders: preparations");
		new File("log").mkdirs();
		new File("log/java").mkdirs();
		new File("log/error").mkdirs();
		new File("log/audit").mkdirs();
		new File("log/chat").mkdirs();
		new File("log/item").mkdirs();
		new File("data/crests").mkdirs();
		new File("data/serial").mkdirs();
		_log.info("Checking folders: all folders checked.");

		if (Config.CheckSystemParam && System.getProperty("user.name").equals("root") && System.getProperty("user.home").equals("/root"))
		{
			_log.info("Servers can't run under root-account ... exited.");
			System.exit(-1);
		}

		_intialTime = System.currentTimeMillis();
	
	}

	public static void setLicense(String name)
	{
		_license = name;
	}

	private void ThreadPool()
	{
		_log.info("ThreadPoolManager: Initializing.");
		ThreadPoolManager.getInstance();
		_log.info("General threads: ..... " + Config.GENERAL_THREAD_POOL_SIZE + ".");
		_log.info("Effect threads: ...... " + Config.EFFECT_THREAD_POOL_SIZE + ".");
		_log.info("AI threads: .......... " + Config.AI_THREAD_POOL_SIZE + ".");
		_log.info("Packet threads: ...... " + Config.PACKET_THREAD_POOL_SIZE + ".");
		_log.info("Total threads: ....... " + Config.THREAD_POOL_SIZE + ".");
	}
}
