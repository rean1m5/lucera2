package ru.catssoftware.gameserver.util;

/**
 * @author luisantonioa
 */
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2MinionData;
import ru.catssoftware.gameserver.model.actor.instance.L2MinionInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.SingletonList;
import ru.catssoftware.util.SingletonMap;

import javolution.util.FastSet;

public class MinionList
{
	/** List containing the current spawned minions for this L2MonsterInstance */
	private final List<L2MinionInstance>	minionReferences = new SingletonList<L2MinionInstance>();
	private final Map<Long, Integer>		_respawnTasks = new SingletonMap<Long,Integer>().setShared();
	private final L2MonsterInstance			master;

	public MinionList(L2MonsterInstance pMaster)
	{
		master = pMaster;
	}

	public int countSpawnedMinions()
	{
		return minionReferences.size();
	}

	private int countSpawnedMinionsById(int minionId)
	{
		int count = 0;
		for (L2MinionInstance minion : getSpawnedMinions())
		{
			if (minion.getNpcId() == minionId)
				count++;
		}

		return count;
	}

	public boolean hasMinions()
	{
		return !getSpawnedMinions().isEmpty();
	}

	public List<L2MinionInstance> getSpawnedMinions()
	{
		return minionReferences;
	}

	public void addSpawnedMinion(L2MinionInstance minion)
	{
		minionReferences.add(minion);
	}

	public int lazyCountSpawnedMinionsGroups()
	{
		Set<Integer> seenGroups = new FastSet<Integer>();
		for (L2MinionInstance minion : getSpawnedMinions())
			seenGroups.add(minion.getNpcId());

		return seenGroups.size();
	}

	public void moveMinionToRespawnList(L2MinionInstance minion)
	{
		Long current = System.currentTimeMillis();
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
			if (_respawnTasks.get(current) == null)
				_respawnTasks.put(current, minion.getNpcId());
			else
			{
				// nice AoE
				for (int i = 1; i < 30; i++)
				{
					if (_respawnTasks.get(current + i) == null)
					{
						_respawnTasks.put(current + i, minion.getNpcId());
						break;
					}
				}
			}
		}
	}

	public void clearRespawnList()
	{
		_respawnTasks.clear();
	}

	/**
	 * Manage respawning of minions for this RaidBoss.<BR><BR>
	 */
	public void maintainMinions()
	{
		if (master == null || master.isAlikeDead())
			return;

		Long current = System.currentTimeMillis();
		if (_respawnTasks != null)
		{
			for (long deathTime : _respawnTasks.keySet())
			{
				double delay = Config.RAID_MINION_RESPAWN_TIMER;
				if ((current - deathTime) > delay)
				{
					spawnSingleMinion(_respawnTasks.get(deathTime), master.getInstanceId());
					_respawnTasks.remove(deathTime);
				}
			}
		}
	}

	/**
	 * Manage the spawn of all Minions of this RaidBoss.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Minion data of all Minions that must be spawn </li>
	 * <li>For each Minion type, spawn the amount of Minion needed </li><BR><BR>
	 *
	 * @param player The L2PcInstance to attack
	 *
	 */
	public void spawnMinions()
	{
		if (master == null || master.isAlikeDead())
			return;

		List<L2MinionData> minions = master.getTemplate().getMinionData();
		if (minions == null)
			return;

		synchronized (minionReferences)
		{
			int minionCount, minionId, minionsToSpawn;
			for (L2MinionData minion : minions)
			{
				minionCount = minion.getAmount();
				minionId = minion.getMinionId();

				minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);

				for (int i = 0; i < minionsToSpawn; i++)
					spawnSingleMinion(minionId, master.getInstanceId());
			}
		}
	}

	/**
	 * Init a Minion and add it in the world as a visible object.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the template of the Minion to spawn </li>
	 * <li>Create and Init the Minion and generate its Identifier </li>
	 * <li>Set the Minion HP, MP and Heading </li>
	 * <li>Set the Minion leader to this RaidBoss </li>
	 * <li>Init the position of the Minion and add it in the world as a visible object </li><BR><BR>
	 *
	 * @param minionid The I2NpcTemplate Identifier of the Minion to spawn
	 *
	 */
	private void spawnSingleMinion(int minionid, int instanceId)
	{
		// Get the template of the Minion to spawn
		L2NpcTemplate minionTemplate = NpcTable.getInstance().getTemplate(minionid);

		// Create and Init the Minion and generate its Identifier
		L2MinionInstance monster = new L2MinionInstance(IdFactory.getInstance().getNextId(), minionTemplate);

		if (Config.CHAMPION_ENABLE && Config.CHAMPION_MINIONS && master.isChampion())
			monster.setChampion(true);

		// Set the Minion HP, MP and Heading
		monster.getStatus().setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp());
		monster.setHeading(master.getHeading());

		// Set the Minion leader to this RaidBoss
		monster.setLeader(master);

		//move monster to masters instance
		monster.setInstanceId(instanceId);

		// Init the position of the Minion and add it in the world as a visible object
		int spawnConstant;
		int randSpawnLim = 170;
		int randPlusMin = 1;
		spawnConstant = Rnd.nextInt(randSpawnLim);
		//randomize +/-
		randPlusMin = Rnd.nextInt(2);
		if (randPlusMin == 1)
			spawnConstant *= -1;
		int newX = master.getX() + Math.round(spawnConstant);
		spawnConstant = Rnd.nextInt(randSpawnLim);
		//randomize +/-
		randPlusMin = Rnd.nextInt(2);
		if (randPlusMin == 1)
			spawnConstant *= -1;
		int newY = master.getY() + Math.round(spawnConstant);

		monster.spawnMe(newX, newY, master.getZ());
	}
}