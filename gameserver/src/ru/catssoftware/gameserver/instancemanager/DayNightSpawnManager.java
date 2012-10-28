package ru.catssoftware.gameserver.instancemanager;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RaidBossInstance;

/*
 * @author  godson
 */
public class DayNightSpawnManager
{

	private final static Logger						_log	= Logger.getLogger(DayNightSpawnManager.class.getName());
	private static DayNightSpawnManager				_instance;
	private static FastMap<L2Spawn, L2NpcInstance>	_dayCreatures;
	private static FastMap<L2Spawn, L2NpcInstance>	_nightCreatures;
	private static FastMap<L2Spawn, L2Boss>			_bosses;

	public static DayNightSpawnManager getInstance()
	{
		if (_instance == null)
			_instance = new DayNightSpawnManager();
		return _instance;
	}

	private DayNightSpawnManager()
	{
		_dayCreatures = new FastMap<L2Spawn, L2NpcInstance>();
		_nightCreatures = new FastMap<L2Spawn, L2NpcInstance>();
		_bosses = new FastMap<L2Spawn, L2Boss>();

		_log.info("DayNightSpawnManager: Day/Night handler initialized");
	}

	public void addDayCreature(L2Spawn spawnDat)
	{
		if (_dayCreatures.containsKey(spawnDat))
		{
			_log.warn("DayNightSpawnManager: Spawn already added into day map");
			return;
		}
		_dayCreatures.put(spawnDat, null);
	}

	public void addNightCreature(L2Spawn spawnDat)
	{
		if (_nightCreatures.containsKey(spawnDat))
		{
			_log.warn("DayNightSpawnManager: Spawn already added into night map");
			return;
		}
		_nightCreatures.put(spawnDat, null);
	}

	/*
	 * Spawn Day Creatures, and Unspawn Night Creatures
	 */
	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}

	/*
	 * Spawn Night Creatures, and Unspawn Day Creatures
	 */
	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}

	/*
	 * Manage Spawn/Respawn
	 * Arg 1 : Map with L2NpcInstance must be unspawned
	 * Arg 2 : Map with L2NpcInstance must be spawned
	 * Arg 3 : String for log info for unspawned L2NpcInstance
	 * Arg 4 : String for log info for spawned L2NpcInstance
	 */
	private void spawnCreatures(FastMap<L2Spawn, L2NpcInstance> UnSpawnCreatures, FastMap<L2Spawn, L2NpcInstance> SpawnCreatures, String UnspawnLogInfo,
			String SpawnLogInfo)
	{
		try
		{
			if (!UnSpawnCreatures.isEmpty())
			{
				for (L2NpcInstance dayCreature : UnSpawnCreatures.values())
				{
					if (dayCreature == null)
						continue;

					dayCreature.deleteMe();
					dayCreature.getSpawn().stopRespawn();
					
				}
			}

			L2NpcInstance creature = null;
			for (L2Spawn spawnDat : SpawnCreatures.keySet())
			{
				if (SpawnCreatures.get(spawnDat) == null)
				{

					creature = spawnDat.doSpawn();
					if (creature == null)
						continue;

					SpawnCreatures.remove(spawnDat);
					SpawnCreatures.put(spawnDat, creature);
					creature.getStatus().setCurrentHp(creature.getMaxHp());
					creature.getStatus().setCurrentMp(creature.getMaxMp());
					creature.getSpawn().startRespawn();
					if (creature.isDecayed())
						creature.setDecayed(false);
					if (creature.isDead())
						creature.doRevive();
				}
				else
				{
					creature = SpawnCreatures.get(spawnDat);
					if (creature == null)
						continue;
					
					if (creature.isDecayed()) 
						creature.setDecayed(false);
					if (creature.isDead())
						creature.doRevive();
					creature.getStatus().setCurrentHp(creature.getMaxHp());
					creature.getStatus().setCurrentMp(creature.getMaxMp());
					creature.getSpawn().startRespawn();
					creature.spawnMe();
					
				}

			}
			_log.info("Despawned "+UnSpawnCreatures.size()+" creature(s), spawned "+SpawnCreatures.size());
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void changeMode(int mode)
	{
		if (_nightCreatures.size() == 0 && _dayCreatures.size() == 0)
			return;

		switch (mode)
		{
		case 0:
			spawnDayCreatures();
			specialNightBoss(0);
			break;
		case 1:
			spawnNightCreatures();
			specialNightBoss(1);
			break;
		default:
			_log.warn("DayNightSpawnManager: Wrong mode sent");
			break;
		}
	}

	public void notifyChangeMode()
	{
		try
		{
			if (GameTimeController.getInstance().isNowNight())
				changeMode(1);
			else
				changeMode(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void cleanUp()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}

	private void specialNightBoss(int mode)
	{
		try
		{
			for (L2Spawn spawn : _bosses.keySet())
			{
				L2Boss boss = _bosses.get(spawn);
				if (boss == null && mode == 1)
				{
					L2NpcInstance npc = spawn.doSpawn();
					if (npc instanceof L2RaidBossInstance)
					{
						boss = (L2Boss) npc;
						RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					}
					else
						continue;

					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}
				if (boss.getNpcId() == 25328 && boss.getRaidStatus().equals(BossSpawnManager.StatusEnum.ALIVE))
					handleHellmans(boss, mode);
				
				if (boss == null || mode == 0)
					continue;
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void handleHellmans(L2Boss boss, int mode)
	{
		switch (mode)
		{
		case 0:
			boss.deleteMe();
			break;
		case 1:
			boss.spawnMe();
			break;
		}
	}

	public L2Boss handleBoss(L2Spawn spawnDat)
	{
		if (_bosses.containsKey(spawnDat))
			return _bosses.get(spawnDat);

		if (GameTimeController.getInstance().isNowNight())
		{
			L2Boss raidboss = (L2Boss) spawnDat.doSpawn();
			_bosses.put(spawnDat, raidboss);
			return raidboss;
		}
		_bosses.put(spawnDat, null);
		return null;
	}
}