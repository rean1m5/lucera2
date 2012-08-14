package ru.catssoftware.gameserver.util;

import org.apache.log4j.xml.DOMConfigurator;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.L2GameServer;
import ru.catssoftware.tools.util.HexUtil;
import ru.catssoftware.util.Console;
import ru.catssoftware.util.JarUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.TimeZone;


public final class BootManager
{

	public static void main(String[] args) throws Throwable
	{
		new BootManager();
	}

	private static String MD5(File f) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(f);				
			byte[] buffer = new byte[8192];
			int read = 0;
			while( (read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}		
			is.close();
			byte[] md5sum = digest.digest();
			return HexUtil.hexToString(md5sum);
		} catch(Exception e) {
			return null;
		}
	}
	@SuppressWarnings("deprecation")
	public BootManager() throws Throwable
	{
		Console.printSection("Boot Manager");

		// create log folder
		new File("log").mkdirs();
		new File("cachedir").mkdirs();
		new File("log/java").mkdirs();
		new File("log/error").mkdirs();
		new File("log/audit").mkdirs();
		new File("log/chat").mkdirs();
		new File("log/ban").mkdirs();
		new File("log/item").mkdirs();
		new File("data/crests").mkdirs();
		new File("data/serial").mkdirs();

		File f = new File("./gameserver.jar");
		if(f.exists())
			JarUtils.addURL(f.toURL());
		else {
			f = new File("./config/administration/developer.properties");
			if(!f.exists()) {
				System.out.println("No gameserver.jar found, exiting...");
				return;
			}
		}
		System.out.println("BootManager: Initializing Logging.");
		initLogging();
		System.out.println("BootManager: Creating Boot Folders and Files.");
		createBootDirs();
		System.out.println("BootManager: Initializing Configs.");
		Config.loadAll();
		TimeZone.setDefault(TimeZone.getTimeZone(Config.TIME_ZONE));
		Console.printSection("Boot Manager");
		System.out.println("BootManager: Config Sucessffully Loaded.");
		System.out.println("BootManager: Preparations Done. Staring GameServer!");
		new L2GameServer();
	}

	private void createBootDirs() throws IOException
	{
		File logFolder = new File(Config.DATAPACK_ROOT, "log");
		File logFolderGame = new File(logFolder, "game");
		logFolderGame.mkdir();
		logFolder.mkdir();

		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/pathnode").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/faenor").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/serial").mkdirs();

		System.out.println("BootManager: All Directories and Files Created!");
	}

	private void initLogging()
	{
		DOMConfigurator.configure("./config/log4j.xml");
	}
}