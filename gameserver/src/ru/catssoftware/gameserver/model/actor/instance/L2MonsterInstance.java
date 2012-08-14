/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.knownlist.MonsterKnownList;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.MinionList;
import ru.catssoftware.tools.random.Rnd;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


/**
 * This class manages all Monsters.
 * 
 * L2MonsterInstance :<BR><BR>
 * <li>L2MinionInstance</li>
 * <li>L2RaidBossInstance </li>
 * 
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public class L2MonsterInstance extends L2Attackable
{
	protected final MinionList		_minionList;

	protected ScheduledFuture<?>	_minionMaintainTask				= null;

	private static final int		MONSTER_MAINTENANCE_INTERVAL	= 1000;

	private boolean					_isKillable						= true;
	private boolean					_questDropable					= true;


	/**
	 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 *  
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 * 
	 * @param objectId Identifier of the object to initialized
	 * @param L2NpcTemplate Template to apply to the NPC
	 */
	public L2MonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		_minionList = new MinionList(this);
	}

	@Override
	public final MonsterKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new MonsterKnownList(this);

		return (MonsterKnownList) _knownList;
	}

	/**
	 * Return true if the attacker is not another L2MonsterInstance.<BR><BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker instanceof L2MonsterInstance)
			return false;

		return true;
	}

	/**
	 * Return true if the L2MonsterInstance is Agressive (aggroRange > 0).<BR><BR>
	 */
	@Override
	public boolean isAggressive()
	{
		return (getTemplate().getAggroRange() > 0);
	}

	@Override
	public void firstSpawn()
	{
		super.firstSpawn();

		if (getTemplate().getMinionData() != null)
		{
			if (getSpawnedMinions() != null)
			{
				for (L2MinionInstance minion : getSpawnedMinions())
				{
					if (minion == null)
						continue;

					getSpawnedMinions().remove(minion);
					minion.deleteMe();
				}
				_minionList.clearRespawnList();

				manageMinions();
			}
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if (getTemplate().getMinionData() != null)
		{
			if (getSpawnedMinions() != null)
			{
				for (L2MinionInstance minion : getSpawnedMinions())
				{
					if (minion == null)
						continue;

					getSpawnedMinions().remove(minion);
					minion.deleteMe();
				}
				_minionList.clearRespawnList();

				manageMinions();
			}
		}
	}

	protected int getMaintenanceInterval()
	{
		return MONSTER_MAINTENANCE_INTERVAL;
	}

	/**
	 * Spawn all minions at a regular interval
	 *
	 */
	protected void manageMinions()
	{
		_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				_minionList.spawnMinions();
			}
		}, getMaintenanceInterval());
	}

	public void callMinions(boolean turnBackToStartLocation)
	{
		if (_minionList.hasMinions())
		{
			for (L2MinionInstance minion : _minionList.getSpawnedMinions())
			{
				// Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance
				if (!isInsideRadius(minion, 200, false, false))
				{
					// Get the coords of the master to use as a base to move the minion to
					int masterX = getX();
					int masterY = getY();
					int masterZ = getZ();

					// Calculate a new random coord for the minion based on the master's coord
					int minionX = masterX + (Rnd.nextInt(401) - 200);
					int minionY = masterY + (Rnd.nextInt(401) - 200);
					int minionZ = masterZ;
					while (((minionX != (masterX + 30)) && (minionX != (masterX - 30))) || ((minionY != (masterY + 30)) && (minionY != (masterY - 30))))
					{
						minionX = masterX + (Rnd.nextInt(401) - 200);
						minionY = masterY + (Rnd.nextInt(401) - 200);
					}

					// Move the minion to the new coords
					if (minion != null && !minion.isInCombat() && !minion.isDead() && !minion.isMovementDisabled())
						if (turnBackToStartLocation && (!minion.isDead()))
							minion.teleToLocation(minionX, minionY, minionZ);
						else
							minion.moveToLocation(minionX, minionY, minionZ, 0);
				}
			}
		}
	}

	public void callMinionsToAssist(L2Character attacker)
	{
		if (_minionList.hasMinions())
		{
			List<L2MinionInstance> spawnedMinions = _minionList.getSpawnedMinions();
			if (spawnedMinions != null && !spawnedMinions.isEmpty())
			{
				Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
				if (itr != null)
				{
					L2MinionInstance minion;
					while (itr.hasNext())
					{
						minion = itr.next();
						// Trigger the aggro condition of the minion
						if (minion != null && !minion.isDead() && !minion.isInCombat())
						{
							if (isRaidBoss() && !isRaidMinion())
								minion.addDamage(attacker, 100, null);
							else
								minion.addDamage(attacker, 1, null);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!_isKillable)
			return false;

		if (!super.doDie(killer))
			return false;

		if (_minionMaintainTask != null)
			_minionMaintainTask.cancel(true); // doesn't do it?

		if (isRaidBoss())
			deleteSpawnedMinions();
		return true;
	}

	public List<L2MinionInstance> getSpawnedMinions()
	{
		return _minionList.getSpawnedMinions();
	}

	public int getTotalSpawnedMinionsInstances()
	{
		return _minionList.countSpawnedMinions();
	}

	public int getTotalSpawnedMinionsGroups()
	{
		return _minionList.lazyCountSpawnedMinionsGroups();
	}

	public void notifyMinionDied(L2MinionInstance minion)
	{
		_minionList.moveMinionToRespawnList(minion);
	}

	public void notifyMinionSpawned(L2MinionInstance minion)
	{
		_minionList.addSpawnedMinion(minion);
	}

	public boolean hasMinions()
	{
		return _minionList.hasMinions();
	}

	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if (attacker.isAutoAttackable(this))
			super.addDamageHate(attacker, damage, aggro);
	}

	@Override
	public void deleteMe()
	{
		if (hasMinions())
		{
			if (_minionMaintainTask != null)
				_minionMaintainTask.cancel(true);

			deleteSpawnedMinions();
		}

		super.deleteMe();
	}

	public void setKillable(boolean b)
	{
		_isKillable = b;
	}

	public boolean getKillable()
	{
		return _isKillable;
	}

	public void setQuestDropable(boolean b)
	{
		_questDropable = b;
	}

	public boolean getQuestDropable()
	{
		return _questDropable;
	}
	
	public void deleteSpawnedMinions()
	{
		for (L2MinionInstance minion : getSpawnedMinions())
		{
			if (minion == null)
				continue;

			minion.abortAttack();
			minion.abortCast();
			minion.deleteMe();
			getSpawnedMinions().remove(minion);
		}

		_minionList.clearRespawnList();
	}

	@Override
	public L2MonsterInstance getMonster()
	{
		return this;
	}

	@Override
	public boolean isMonster()
	{
		return true;
	}
}