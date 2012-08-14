package ru.catssoftware.loginserver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;



import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Util;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.extension.ExtensionManager;
import ru.catssoftware.loginserver.manager.BanManager;
import ru.catssoftware.loginserver.manager.GameServerManager;
import ru.catssoftware.loginserver.manager.LoginManager;
import ru.catssoftware.loginserver.mmocore.SelectorConfig;
import ru.catssoftware.loginserver.mmocore.SelectorThread;
import ru.catssoftware.loginserver.thread.GameServerListener;

public class L2LoginServer
{
	public static final int					PROTOCOL_REV	= 0x102;
	private static Logger 					_log			= Logger.getLogger(L2LoginServer.class);
	private static double					_intialTime		= 0;
	private GameServerListener				_gameServerListener;
	private SelectorThread<L2LoginClient>	_selectorThread;
	private static L2LoginServer			_instance;

	public static L2LoginServer getInstance()
	{
		return _instance;
	}

	public static void main(String[] args) throws Throwable
	{
		for(int i =0; i<args.length;i++) {
			if(args[i].startsWith("--config-dir")) try {
				String cdir=args[i].split("=")[1];
				File f = new File(cdir);
				if(f.exists() && f.isDirectory()) {
					System.out.println("Using configuration folder "+cdir);
					L2Properties.CONFIG_DIR = cdir;
				}
			} catch(Exception e) {
			}
		}
		_instance = new L2LoginServer();
	}

	public L2LoginServer() throws Throwable
	{
		/**
		 * Создание директории для логов
		 * Dir name: log и поддериктории
		 **/
		new File("log").mkdirs();
		new File("log/error").mkdirs();
		new File("log/java").mkdirs();
		new File("log/game").mkdirs();

		/**
		 * Чтение конфигурации логирования
		 * common logins params
		 **/
		DOMConfigurator.configure("./config/log4j.xml");

		/**
		 * Обновление версии и описания ревизии сервера
		 * Если файл отсутствует, то создаем новый
		 **/
		File versionFile = new File("./config/versionning/build-time.properties");
		if (versionFile.exists())
			versionFile.delete();
		versionFile.createNewFile();

		/**
		 * Время начала загрузки сервера
		 **/
		_intialTime = System.currentTimeMillis();

		/**
		 * Загрузка конфигурации сервера
		 * Загрузка параметров базы данных
		 **/
		Config.load();
		L2DatabaseFactory.getInstance();
		
		/**
		 * Выводим информацию системы
		 * Описание CPU, MEM, OS, JAVA SYS
		 **/
		Util.printGeneralSystemInfo();

		/**
		 * Инициализация основных сервисов сервера
		 **/
		GameServerManager.getInstance();
		ClientManager.getInstance();
		LoginManager.load();
		
		
		BanManager.getInstance();
		ExtensionManager.getInstance();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				_log.info("Login server shutting down");
			}
		});
		initNetworkLayer();
		initGSListener();
		startServer();

		/**
		 * Основная инфомация по состоянию сервера
		 **/
		Util.printSection("Server Info");
		printInfo();
	}

	private static void printInfo()
	{
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		double finalTime = System.currentTimeMillis();
		if(Config.CRYPT_TOKEN)
			_log.info("Token-based brut-proof enabled");
		_log.info("Free memory: " + freeMem + " Mb of " + totalMem + " Mb");
		_log.info("Ready on IP: " + Config.LOGIN_SERVER_HOSTNAME + ":" + Config.LOGIN_SERVER_PORT + ".");
		_log.info("Load time: " + (int) ((finalTime - _intialTime) / 1000) + " Seconds.");
		Util.printSection("Server Info");
		_log.info("Login Server successfully started.");
	}

	private void startServer()
	{
		try
		{
			_selectorThread.openServerSocket(InetAddress.getByName(Config.LOGIN_SERVER_HOSTNAME), Config.LOGIN_SERVER_PORT);
		}
		catch (IOException e)
		{
			_log.fatal("FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
			System.exit(1);
		}
		_selectorThread.start();
	}


	private void initGSListener()
	{
		_gameServerListener = new GameServerListener();
		_gameServerListener.start();
		_log.info("Listening for GameServers on " + Config.LOGIN_HOSTNAME + ":" + Config.LOGIN_PORT);
	}

	private void initNetworkLayer()
	{
		L2LoginPacketHandler loginPacketHandler = new L2LoginPacketHandler();
		SelectorHelper sh = new SelectorHelper();
		SelectorConfig<L2LoginClient> ssc = new SelectorConfig<L2LoginClient>(null, null, sh, loginPacketHandler);
		try
		{
			_selectorThread = new SelectorThread<L2LoginClient>(ssc, sh, sh, sh);
		}
		catch (IOException e)
		{
			_log.fatal("FATAL: Failed to open Selector. Reason: " + e.getMessage(), e);
			System.exit(1);
		}
	}

	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
}