package ru.catssoftware.gameserver.cache;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.tools.cache.FastMRUCache;

/**
 * @author Layane
 */
public class CrestCache
{
	private final static Logger				_log				= Logger.getLogger(CrestCache.class.getName());

	private static CrestCache				_instance;

	private FastMRUCache<Integer, byte[]>	_cachePledge		= new FastMRUCache<Integer, byte[]>();

	private FastMRUCache<Integer, byte[]>	_cachePledgeLarge	= new FastMRUCache<Integer, byte[]>();

	private FastMRUCache<Integer, byte[]>	_cacheAlly			= new FastMRUCache<Integer, byte[]>();

	private int								_loadedFiles;

	private long							_bytesBuffLen;

	public static CrestCache getInstance()
	{
		if (_instance == null)
			_instance = new CrestCache();
		return _instance;
	}

	public CrestCache()
	{
		convertOldPedgeFiles();
		reload();
	}

	public void reload()
	{
		FileFilter filter = new BmpFilter();

		File dir = new File("data/crests/");

		File[] files = dir.listFiles(filter);
		byte[] content;
		synchronized (this)
		{
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			_cachePledge.clear();
			_cachePledgeLarge.clear();
			_cacheAlly.clear();
		}

		FastMap<Integer, byte[]> _mapPledge = _cachePledge.getContentMap();
		FastMap<Integer, byte[]> _mapPledgeLarge = _cachePledgeLarge.getContentMap();
		FastMap<Integer, byte[]> _mapAlly = _cacheAlly.getContentMap();

		for (File file : files)
		{
			RandomAccessFile f = null;
			synchronized (this)
			{
				try
				{
					f = new RandomAccessFile(file, "r");
					content = new byte[(int) f.length()];
					f.readFully(content);

					if (file.getName().startsWith("Crest_Large_"))
						_mapPledgeLarge.put(Integer.valueOf(file.getName().substring(12, file.getName().length() - 4)), content);
					else if (file.getName().startsWith("Crest_"))
						_mapPledge.put(Integer.valueOf(file.getName().substring(6, file.getName().length() - 4)), content);
					else if (file.getName().startsWith("AllyCrest_"))
						_mapAlly.put(Integer.valueOf(file.getName().substring(10, file.getName().length() - 4)), content);
					_loadedFiles++;
					_bytesBuffLen += content.length;
				}
				catch (Exception e)
				{
					_log.warn("problem with crest bmp file " + e);
				}
				finally
				{
					try
					{
						if (f != null)
							f.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		_log.info("Cache[Crest]: " + String.format("%.3f", getMemoryUsage()) + "MB on " + getLoadedFiles() + " files loaded.");
		_log.info("Cache[Crest]: (Forget Time: " + (_cachePledge.getForgetTime() / 1000) + "s , Capacity: " + _cachePledge.capacity() + ")");
	}

	public void convertOldPedgeFiles()
	{
		File dir = new File("data/crests/");

		File[] files = dir.listFiles(new OldPledgeFilter());

		for (File file : files)
		{
			int clanId = Integer.parseInt(file.getName().substring(7, file.getName().length() - 4));

			_log.info("Found old crest file \"" + file.getName() + "\" for clanId " + clanId);

			int newId = IdFactory.getInstance().getNextId();

			L2Clan clan = ClanTable.getInstance().getClan(clanId);

			if (clan != null)
			{
				removeOldPledgeCrest(clan.getCrestId());

				file.renameTo(new File("data/crests/Crest_" + newId + ".bmp"));
				_log.info("Renamed Clan crest to new format: Crest_" + newId + ".bmp");

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection(con);
					PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
					statement.setInt(1, newId);
					statement.setInt(2, clan.getClanId());
					statement.executeUpdate();
					statement.close();
				}
				catch (SQLException e)
				{
					_log.warn("could not update the crest id:" + e.getMessage());
				}
				finally
				{
					try
					{
						if (con != null)
							con.close();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
				}

				clan.setCrestId(newId);
				clan.setHasCrest(true);
			}
			else
			{
				_log.info("Clan Id: " + clanId + " does not exist in table.. deleting.");
				file.delete();
			}
		}
	}

	public float getMemoryUsage()
	{
		return ((float) _bytesBuffLen / 1048576);
	}

	public int getLoadedFiles()
	{
		return _loadedFiles;
	}

	public byte[] getPledgeCrest(int id)
	{
		return _cachePledge.get(id);
	}

	public byte[] getPledgeCrestLarge(int id)
	{
		return _cachePledgeLarge.get(id);
	}

	public byte[] getAllyCrest(int id)
	{
		return _cacheAlly.get(id);
	}

	public void removePledgeCrest(int id)
	{
		File crestFile = new File("data/crests/Crest_" + id + ".bmp");
		_cachePledge.remove(id);
		try
		{
			crestFile.delete();
		}
		catch (Exception e)
		{
		}
	}

	public void removePledgeCrestLarge(int id)
	{
		File crestFile = new File("data/crests/Crest_Large_" + id + ".bmp");
		_cachePledgeLarge.remove(id);
		try
		{
			crestFile.delete();
		}
		catch (Exception e)
		{
		}
	}

	public void removeOldPledgeCrest(int id)
	{
		File crestFile = new File("data/crests/Pledge_" + id + ".bmp");
		try
		{
			crestFile.delete();
		}
		catch (Exception e)
		{
		}
	}

	public void removeAllyCrest(int id)
	{
		File crestFile = new File("data/crests/AllyCrest_" + id + ".bmp");
		_cacheAlly.remove(id);
		try
		{
			crestFile.delete();
		}
		catch (Exception e)
		{
		}
	}

	public boolean savePledgeCrest(int newId, byte[] data)
	{
		File crestFile = new File("data/crests/Crest_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cachePledge.getContentMap().put(newId, data);
			return true;
		}
		catch (IOException e)
		{
			_log.info("Error saving pledge crest" + crestFile + ":", e);
			return false;
		}
	}

	public boolean savePledgeCrestLarge(int newId, byte[] data)
	{
		File crestFile = new File("data/crests/Crest_Large_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cachePledgeLarge.getContentMap().put(newId, data);
			return true;
		}
		catch (IOException e)
		{
			_log.info("Error saving Large pledge crest" + crestFile + ":", e);
			return false;
		}
	}

	public boolean saveAllyCrest(int newId, byte[] data)
	{
		File crestFile = new File("data/crests/AllyCrest_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cacheAlly.getContentMap().put(newId, data);
			return true;
		}
		catch (IOException e)
		{
			_log.info("Error saving ally crest" + crestFile + ":", e);
			return false;
		}
	}

	class BmpFilter implements FileFilter
	{
		public boolean accept(File file)
		{
			return (file.getName().endsWith(".bmp"));
		}
	}

	class OldPledgeFilter implements FileFilter
	{
		public boolean accept(File file)
		{
			return (file.getName().startsWith("Pledge_"));
		}
	}
}