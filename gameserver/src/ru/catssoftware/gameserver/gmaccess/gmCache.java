package ru.catssoftware.gameserver.gmaccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * Данный класс кэширует права администраторов
 * @author m095 "CatsSoftware"
 * @version 1.0
 */

public class gmCache
{
	private static gmCache					_instance		= null;
	private Map<Integer, GmPlayer> 			_access 		= new HashMap<Integer, GmPlayer>();
	public static Logger						_log			= Logger.getLogger(gmCache.class.getName());

	public static gmCache getInstance()
	{
		if (_instance == null)
			_instance = new gmCache();
		return _instance;
	}
	
	private gmCache()
	{
		loadAccess(false);
	}

	public void setGm(int charId) {
		GmPlayer gm = new GmPlayer(charId);
		gm.setRoot(true);
		_access.put(charId, gm);
		gmController.getInstance().checkPrivs(L2World.getInstance().findPlayer(charId));
		gm.setIsTemp(true);
		
	}
	public void removeGM(int charId) {
		_access.remove(charId);
	}
	public void loadAccess(boolean reload)
	{
		/* Папка хранения конфигов */
		File dir = new File("./config/administration/gmaccess");
		/* Проверка папки доступа */
		if(!dir.exists())
		{
			dir.mkdirs();
			return;
		}
		/* Очистка текущего кэша гм */
		_access.clear();
		_access = new HashMap<Integer, GmPlayer>();
		/* Сбор списка файлов гмов */
		File[] files = dir.listFiles();
		GmPlayer gm = null;
		/* Перебор полученых файлов */
		for(File file : files)
		{
			if (checkFile(file))
			{
				try
				{
					/* конфиг гма */
					Properties cfg = new L2Properties("./config/administration/gmaccess/" + file.getName());
					/* создаем гма */
					gm = new GmPlayer(Integer.parseInt(cfg.getProperty("CharId")));
					/* задаем параметры */
					gm.setName(file.getName().substring(0, (file.getName().length()-4)));
					gm.setGm(Boolean.parseBoolean(cfg.getProperty("isAdmin")));
					gm.seFixRes(Boolean.parseBoolean(cfg.getProperty("FixedRes")));
					gm.seAltG(Boolean.parseBoolean(cfg.getProperty("AllowAltG")));
					gm.setPeaceAtk(Boolean.parseBoolean(cfg.getProperty("AllowPeaceAtk")));
					gm.setRoot(Boolean.parseBoolean(cfg.getProperty("isRoot")));
					gm.setCheckIp(Boolean.parseBoolean(cfg.getProperty("CheckIp")));
					gm.setIP(cfg.getProperty("SecureIp").split(";"));
					/* загружаем команды */
					loadCommands(gm, file);
					/* проверка параметров */
					if (!gm.isRoot() && !gm.isGm())
					{
						_log.info("GmController: Acces for player: " + gm.getName() + " incorrect");
						continue;
					}
					/* добавляем гма */
					_access.put(gm.getObjId(), gm);
				}
				catch (Exception e)
				{
					_log.info("GmController: Acces for player: " + gm.getName() + " incorrect");
					continue;
				}
			}
		}
		_log.info("GmController: Loaded " + _access.size() + " admin players.");
		
		/* При Вкл. дебаге выводим инфу о гмах */
		if (Config.DEBUG)
			info();
		int max = _access.size();
		int cur = 0;
		if(reload)
		{
			gmController.getInstance().checkAdmins();
			
			for (L2PcInstance pl : L2World.getInstance().getAllPlayers())
			{
				if (pl == null || pl.isGM() || pl.isOfflineTrade())
					continue;
				if(_access.get(pl.getObjectId())!=null)
				{
					gmController.getInstance().checkPrivs(pl);
					cur++;
				}
				if (cur == max)
					break;
			}
		}
	}

	/**
	 * Вернуть Gm class
	 * @param id
	 * @return
	 */
	public GmPlayer getGmPlayer(int id)
	{
	
		return _access.get(id);
	}

	/**
	 * Дебаг метод
	 * Выводит гмов в консоль, также их права
	 */
	public void info()
	{
		_log.info("============= GM LIST =============");
		int y = _access.size();
		for (GmPlayer gm : _access.values())
		{
			_log.info("GM title: " + gm.getName());
			_log.info("GM objId: " + gm.getObjId());
			_log.info("GM admin: " + (gm.isRoot() ? "True" : "False"));
			_log.info("GM event: " + (gm.isGm() ? "True" : "False"));
			_log.info("GM fix.r: " + (gm.allowFixRes() ? "True" : "False"));
			_log.info("Gm alt.g: " + (gm.allowAltG() ? "True" : "False"));
			y--;
			if (y != 0)
				_log.info("-----------------------------------");
		}
		_log.info("============= GM LIST =============");
	}
	/**
	 * Вернуть статус Gm or Player
	 * @param id
	 * @return
	 */
	public boolean isGm(int id)
	{
		if(!_access.containsKey(id))
			return false;
		return _access.get(id).isGm();
	}
	
	/**
	 * Проверка файла
	 * @param file
	 * @return
	 */
	private boolean checkFile(File file)
	{
		if (file == null)
			return false;
		if (file.isDirectory())
			return false;
		if (file.isHidden())
			return false;
		if (!file.getName().endsWith(".cfg"))
			return false;
		if (file.getName().startsWith("example.cfg"))
			return false;

		return true;
	}

	/**
	 * Загрузка списка допустимых команд
	 * @param gm
	 * @param file
	 */
	private void loadCommands(GmPlayer gm, File file)
	{
		BufferedReader in = null;
		try
		{
			in = new BufferedReader(new FileReader(file));
			String str;
			
			while((str = in.readLine()) != null)
			{
				// Пропускаем закоментированые строки
				if(str.startsWith("#") || str.length() == 0)
					continue;
				
				if(str.contains("#"))
					str = str.split("#")[0];
				
				if(str.startsWith("//"))
					gm.putCommand(str.substring("//".length()));
			}
		}
		catch (FileNotFoundException e)
		{
			_log.info("GmController: Error on read acces for Gm: " + gm.getName());
		}
		catch (IOException e)
		{
			_log.info("GmController: Error on read acces for Gm: " + gm.getName());
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}