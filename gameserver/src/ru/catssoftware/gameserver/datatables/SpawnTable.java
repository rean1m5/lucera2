package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.instancemanager.DayNightSpawnManager;
import ru.catssoftware.gameserver.instancemanager.RaidBossSpawnManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortResistSiegeManager;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.util.ArrayUtils;

public class SpawnTable
{
	private final static Logger			_log			= Logger.getLogger(SpawnTable.class.getName());
	private static final SpawnTable		_instance		= new SpawnTable();
	private Map<Integer, L2Spawn>		_spawntable		= new FastMap<Integer, L2Spawn>().setShared(true);
	private int							_npcSpawnCount	= 0;
	private int							_cSpawnCount	= 0;
	private int							_cNotSpawned	= 0;
	private int							_highestDbId;
	private int							_highestCustomDbId;

	public static SpawnTable getInstance()
	{
		return _instance;
	}

	private SpawnTable()
	{
		if (!Config.ALT_DEV_NO_SPAWNS)
			fillSpawnTable();
		else
			_log.info("Spawns Disabled Fast Load Mode Enabled.");
	}

	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	private void fillSpawnTable()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, random_zone, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist ORDER BY id");
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.getType().equalsIgnoreCase("L2SiegeGuard"))
					{
					}
					else if (rset.getInt("npc_templateid") == 35370 || rset.getInt("npc_templateid") == 35371 || rset.getInt("npc_templateid") == 35368)
					{
						FortResistSiegeManager.getInstance().addSiegeMob(rset.getInt("npc_templateid"),rset.getInt("locx"),rset.getInt("locy"),rset.getInt("locz"),rset.getInt("respawn_delay"));
					}
					else if (template1.getType().equalsIgnoreCase("L2RaidBoss"))
					{
					}
					else if (!Config.SPAWN_CLASS_MASTER && template1.getType().equals("L2ClassMaster"))
					{
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(_npcSpawnCount);
						spawnDat.setDbId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						if (template1.getType().equalsIgnoreCase("L2Monster") && Config.MON_RESPAWN_RANDOM_ENABLED)
						{
							int rndZone = rset.getInt("random_zone");
							if (rndZone<0)
								rndZone=-1;
							switch (rndZone)
							{
								case -1:
									spawnDat.setRndRespawnRange(Config.MON_RESPAWN_RANDOM_ZONE);
									spawnDat.enableRndRangeRespawn(true);
									break;
								case 0: // Нет рандомного спавна
									break;
								default:
									spawnDat.setRndRespawnRange(rndZone);
									spawnDat.enableRndRangeRespawn(true);
							}
						}
						int loc_id = rset.getInt("loc_id");
						spawnDat.setLocation(loc_id);

						switch (rset.getInt("periodOfDay"))
						{
							case 0: // default
								_npcSpawnCount += spawnDat.init(true);
								break;
							case 1: // Day
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								_npcSpawnCount++;
								break;
							case 2: // Night
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								_npcSpawnCount++;
								break;
						}
						if (spawnDat.getDbId() > _highestDbId)
							_highestDbId = spawnDat.getDbId();
						_spawntable.put(spawnDat.getId(), spawnDat);
						if (template1.getNpcId()>=21187 && template1.getNpcId()<=21207) //7 signs Angels
							SevenSigns.ANGELS.add(spawnDat);
						if (template1.getNpcId()>=21166 && template1.getNpcId()<=21186) //7 signs Demons
							SevenSigns.DEMONS.add(spawnDat);
					}
				}
				else
					_log.warn("SpawnTable: Data missing or incorrect in NPC/Custom NPC table for ID: " + rset.getInt("npc_templateid") + ".");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("SpawnTable: Spawn could not be initialized: " + e);
		}
		_log.info("SpawnTable: Loaded " + _spawntable.size() + " Npc Spawn Locations.");
		_log.info("SpawnTable: " + _cNotSpawned + " Npc Not Spawned.");

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, random_zone, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist ORDER BY id");
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			_cSpawnCount = _spawntable.size();

			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.getType().equalsIgnoreCase("L2SiegeGuard"))
					{
					}
					else if (template1.getType().equalsIgnoreCase("L2RaidBoss"))
					{
					}
					else if (!Config.SPAWN_CLASS_MASTER && template1.getType().equals("L2ClassMaster"))
					{
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(_npcSpawnCount);
						spawnDat.setDbId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						if (template1.getType().equalsIgnoreCase("L2Monster") && Config.MON_RESPAWN_RANDOM_ENABLED)
						{
							int rndZone = rset.getInt("random_zone");
							if (rndZone<0)
								rndZone=-1;
							switch (rndZone)
							{
								case -1:
									spawnDat.setRndRespawnRange(Config.MON_RESPAWN_RANDOM_ZONE);
									spawnDat.enableRndRangeRespawn(true);
									break;
								case 0: // Нет рандомного спавна
									break;
								default:
									spawnDat.setRndRespawnRange(rndZone);
									spawnDat.enableRndRangeRespawn(true);
							}
						}
						spawnDat.setCustom();
						int loc_id = rset.getInt("loc_id");
						spawnDat.setLocation(loc_id);

						switch (rset.getInt("periodOfDay"))
						{
							case 0: // default
								_npcSpawnCount += spawnDat.init();
								break;
							case 1: // Day
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								_npcSpawnCount++;
								break;
							case 2: // Night
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								_npcSpawnCount++;
								break;
						}

						if (spawnDat.getDbId() > _highestCustomDbId)
							_highestCustomDbId = spawnDat.getDbId();
						_spawntable.put(spawnDat.getId(), spawnDat);
						if (template1.getNpcId()>=21187 && template1.getNpcId()<=21207) //7 signs Angels
							SevenSigns.ANGELS.add(spawnDat);
						if (template1.getNpcId()>=21166 && template1.getNpcId()<=21186) //7 signs Demons
							SevenSigns.DEMONS.add(spawnDat);
					}
				}
				else
					_log.warn("SpawnTable: Data missing or incorrect in NPC/Custom NPC table for ID: " + rset.getInt("npc_templateid") + ".");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("SpawnTable: Custom spawn could not be initialized: " + e);
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
		_cSpawnCount = _spawntable.size() - _cSpawnCount;

		if (_cSpawnCount > 0)
			_log.info("SpawnTable: Loaded " + _cSpawnCount + " Custom Spawn Locations.");
	}

	public Map<Integer, L2Spawn> getAllTemplates()
	{
		return _spawntable;
	}

	public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
	{
		_npcSpawnCount++;
		if (spawn.isCustom())
		{
			_highestCustomDbId++;
			spawn.setDbId(_highestCustomDbId);
		}
		else if (spawn.isJail())
		{
			JailSpawnTable._highestJailDbId++;
			spawn.setDbId(JailSpawnTable._highestJailDbId);
		}
		else
		{
			_highestDbId++;
			spawn.setDbId(_highestDbId);
		}

		spawn.setId(_npcSpawnCount);

		_spawntable.put(spawn.getId(), spawn);
		if (spawn.getNpcId()>=21187 && spawn.getNpcId()<=21207) //7 signs Angels
			SevenSigns.ANGELS.add(spawn);
		if (spawn.getNpcId()>=21166 && spawn.getNpcId()<=21186) //7 signs Demons
			SevenSigns.DEMONS.add(spawn);

		if (storeInDb)
		{
			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("INSERT INTO " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " (id,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id) values(?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, spawn.getDbId());
				statement.setInt(2, spawn.getAmount());
				statement.setInt(3, spawn.getNpcId());
				statement.setInt(4, spawn.getLocx());
				statement.setInt(5, spawn.getLocy());
				statement.setInt(6, spawn.getLocz());
				statement.setInt(7, spawn.getHeading());
				statement.setInt(8, spawn.getRespawnDelay() / 1000);
				statement.setInt(9, spawn.getLocation());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("SpawnTable: Could not store spawn in the DB:" + e);
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
		}
	}

	public void updateSpawn(L2Spawn spawn)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("update " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " set count=?,npc_templateid=?,locx=?,locy=?,locz=?,heading=?,respawn_delay=?,loc_id=? where id =?");
			statement.setInt(1, spawn.getAmount());
			statement.setInt(2, spawn.getNpcId());
			statement.setInt(3, spawn.getLocx());
			statement.setInt(4, spawn.getLocy());
			statement.setInt(5, spawn.getLocz());
			statement.setInt(6, spawn.getHeading());
			statement.setInt(7, spawn.getRespawnDelay() / 1000);
			statement.setInt(8, spawn.getLocation());
			statement.setInt(9, spawn.getDbId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("SpawnTable: Could not update spawn in the DB:" + e);
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
	}

	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		if (_spawntable.remove(spawn.getId()) == null)
			return;
		if (spawn.getNpcId()>=21187 && spawn.getNpcId()<=21207) //7 signs Angels
			SevenSigns.ANGELS.remove(spawn);
		if (spawn.getNpcId()>=21166 && spawn.getNpcId()<=21186) //7 signs Demons
			SevenSigns.DEMONS.remove(spawn);

		if (updateDb)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("DELETE FROM " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " WHERE id=?");
				statement.setInt(1, spawn.getDbId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("SpawnTable: Spawn " + spawn.getDbId() + " could not be removed from DB: " + e);
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
		}
	}

	public void reloadAll()
	{
		cleanUp();
		fillSpawnTable();
	}

	private void cleanUp()
	{
		for(L2Spawn spawn : _spawntable.values()) try {
			spawn.stopRespawn();
			if(spawn.getLastSpawn()!=null)
				spawn.getLastSpawn().deleteMe();
		} catch(Exception e ) { continue; }
		_spawntable.clear();
		SevenSigns.ANGELS.clear();
		SevenSigns.DEMONS.clear();
	}

	public L2Spawn getTemplate(int id)
	{
		L2Spawn result = _spawntable.get(id);
		if(result==null)
			result = RaidBossSpawnManager.getInstance().getSpawns().get(id);
		return  result;
	}
	
	public void findNPCInstances(L2PcInstance activeChar, int npcId, int teleportIndex)
	{
		int index = 0;
		for (L2Spawn spawn : _spawntable.values())
		{
			if (npcId == spawn.getNpcId())
			{
				index++;
				if (teleportIndex > -1)
				{
					if (teleportIndex == index)
						activeChar.teleToLocation(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), true);
				}
				else
					activeChar.sendMessage(index + " - " + spawn.getTemplate().getName() + " (" + spawn.getId() + "): " + spawn.getLocx() + " " + spawn.getLocy() + " " + spawn.getLocz());
			}
		}
		if (index == 0)
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_NO_SPAWN_FOUND));
	}
	public L2Spawn [] findAllNpc(int npcId) {
		L2Spawn [] result = new L2Spawn[] {};
		for (L2Spawn spawn : _spawntable.values())
			if (npcId == spawn.getNpcId())
				result = ArrayUtils.add(result, spawn);
		return result;
	}
	
}