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
package ru.catssoftware.gameserver.model;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.util.concurrent.L2Collection;
import ru.catssoftware.util.concurrent.L2SynchronizedCollection;

public final class L2WorldRegion
{
	private static final Logger _log = Logger.getLogger(L2WorldRegion.class);

	public static final int MAP_MIN_X = -131072;
	public static final int MAP_MAX_X = 228608;
	public static final int MAP_MIN_Y = -262144;
	public static final int MAP_MAX_Y = 262144;

	private final L2Collection<L2PlayableInstance> _playables = new L2SynchronizedCollection<L2PlayableInstance>();
	private final L2Collection<L2Object> _objects = new L2SynchronizedCollection<L2Object>();

	private final int _tileX;
	private final int _tileY;

	private L2WorldRegion[] _surroundingRegions = new L2WorldRegion[0];
	private L2Zone[] _zones = new L2Zone[0];

	private volatile boolean _active = Config.GRIDS_ALWAYS_ON;
	private ScheduledFuture<?> _neighborsTask;

	public L2WorldRegion(int pTileX, int pTileY)
	{
		_tileX = pTileX;
		_tileY = pTileY;
	}

	public L2Zone[] getZones()
	{
		return _zones;
	}
	
	public void addZone(L2Zone zone)
	{
		_zones = Arrays.copyOf(_zones, _zones.length + 1);
		_zones[_zones.length - 1] = zone;
	}

	public void clearZones()
	{
		_zones = new L2Zone[0];
	}

	public void revalidateZones(L2Character character)
	{
		// do NOT update the world region while the character is still in the process of teleporting
		// Once the teleport is COMPLETED, revalidation occurs safely, at that time.

		if (character.isTeleporting())
			return;

		for (L2Zone z : _zones)
			z.revalidateInZone(character);
	}

	public void removeFromZones(L2Character character)
	{
		for (L2Zone z : _zones)
			z.removeCharacter(character);
	}

	public boolean containsZone(int zoneId)
	{
		for (L2Zone z : _zones)
		{
			if (z.getId() == zoneId)
				return true;
		}

		return false;
	}

	public void onDeath(L2Character character)
	{
		for (L2Zone z : _zones)
			z.onDieInside(character);
	}

	public void onRevive(L2Character character)
	{
		for (L2Zone z : _zones)
			z.onReviveInside(character);
	}

	public boolean checkEffectRangeInsidePeaceZone(L2Skill skill, final int x, final int y, final int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;

		for (L2Zone e : _zones)
		{
			if (e.isPeace())
			{
				if (e.isInsideZone(x, up, z))
					return false;

				if (e.isInsideZone(x, down, z))
					return false;

				if (e.isInsideZone(left, y, z))
					return false;

				if (e.isInsideZone(right, y, z))
					return false;

				if (e.isInsideZone(x, y, z))
					return false;
			}
		}

		return true;
	}

	private final class NeighborsTask implements Runnable
	{
		private final boolean _isActivating;

		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}

		public void run()
		{
			if (_isActivating)
			{
				for (L2WorldRegion neighbor : getSurroundingRegions())
					neighbor.setActive(true);
			}
			else
			{
				if (areNeighborsEmpty())
					setActive(false);

				for (L2WorldRegion neighbor : getSurroundingRegions())
				{
					if (neighbor.areNeighborsEmpty())
						neighbor.setActive(false);
				}
			}
		}
	}

	private void setActive(boolean active)
	{
		if (_active == active)
			return;

		_active = active;

		if (!active)
		{
			for (L2Object obj : getVisibleObjects())
			{
				if (obj instanceof L2Attackable)
				{
					L2Attackable mob = (L2Attackable) obj;

					mob.setTarget(null);
					mob.stopMove(null, false);
					mob.stopAllEffects();
					mob.clearAggroList();
					mob.clearDamageContributors();
					mob.resetAbsorbList();
					mob.getKnownList().removeAllKnownObjects();
					mob.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					mob.getAI().stopAITask();
				}
			}
		}
		else
		{
			final L2Object[][] surroundingObjects = getAllSurroundingObjects2DArray();

			for (L2Object obj : getVisibleObjects())
			{
				if (obj == null)
					continue;

				if (obj instanceof L2Attackable)
					((L2Attackable) obj).getStatus().startHpMpRegeneration();

				else if (obj instanceof L2NpcInstance)
					((L2NpcInstance) obj).startRandomAnimationTimer();

				obj.getKnownList().tryAddObjects(surroundingObjects);
			}
		}
	}

	public boolean isActive()
	{
		return _active;
	}

	// check if all 9 neighbors (including self) are inactive or active but with no players.
	// returns true if the above condition is met.
	public boolean areNeighborsEmpty()
	{
		// if this region is occupied, return false.
		if (isActive() && !_playables.isEmpty())
			return false;

		// if any one of the neighbors is occupied, return false
		for (L2WorldRegion neighbor : getSurroundingRegions())
		{
			if (neighbor.isActive() && !neighbor._playables.isEmpty())
				return false;
		}

		// in all other cases, return true.
		return true;
	}

	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on
	 * neighbors in the case when a person just teleported into a region and then teleported out immediately...there is
	 * no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// first set self to active and do self-tasks...
		setActive(true);

		// if the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}

			// then, set a timer to activate the neighbors
			_neighborsTask = ThreadPoolManager.getInstance().schedule(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}

	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the
	 * case when a person just moved out of a region that he may very soon return to. There is no reason to turn self &
	 * neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}

			// start a timer to "suggest" a deactivate to self and neighbors.
			// suggest means: first check if a neighbor has L2PcInstances in it.  If not, deactivate.
			_neighborsTask = ThreadPoolManager.getInstance().schedule(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}

	/**
	 * Add the L2Object in the _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing
	 * L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 */
	public void addVisibleObject(L2Object object)
	{
		if (Config.ASSERT)
			assert object.getWorldRegion() == this;

		if (object == null)
			return;

		_objects.add(object);

		if (object instanceof L2PlayableInstance)
		{
			_playables.add((L2PlayableInstance) object);

			// if this is the first player to enter the region, activate self & neighbors
			if (!Config.GRIDS_ALWAYS_ON && _playables.size() == 1)
				startActivation();
		}
	}

	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion <BR>
	 * <BR>
	 * If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this
	 * L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 */
	public void removeVisibleObject(L2Object object)
	{
		if (object == null)
			return;

		_objects.remove(object);

		if (object instanceof L2PlayableInstance)
		{
			_playables.remove((L2PlayableInstance) object);

			if (!Config.GRIDS_ALWAYS_ON && _playables.isEmpty())
				startDeactivation();
		}
	}

	public void addSurroundingRegion(L2WorldRegion region)
	{
		_surroundingRegions = Arrays.copyOf(_surroundingRegions, _surroundingRegions.length + 1);
		_surroundingRegions[_surroundingRegions.length - 1] = region;
	}

	public L2WorldRegion[] getSurroundingRegions()
	{
		return _surroundingRegions;
	}

	public L2Object[] getVisibleObjects()
	{
		return _objects.toArray(new L2Object[_objects.size()]);
	}

	public L2PlayableInstance[] getVisiblePlayables()
	{
		return _playables.toArray(new L2PlayableInstance[_playables.size()]);
	}

	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}

	/**
	 * Deleted all spawns in the world.
	 */
	public synchronized void deleteVisibleNpcSpawns()
	{
		for (L2Object obj : getVisibleObjects())
		{
			if (obj instanceof L2NpcInstance)
			{
				L2NpcInstance npc = (L2NpcInstance) obj;
				npc.deleteMe();

				L2Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
			}
		}

		if (_log.isDebugEnabled())
				_log.info("All visible NPC's deleted in Region: " + getName());
	}

	public L2Object[][] getAllSurroundingObjects2DArray()
	{
		final L2Object[][] result = new L2Object[_surroundingRegions.length][];

		for (int i = 0; i < _surroundingRegions.length; i++)
			result[i] = _surroundingRegions[i].getVisibleObjects();

		return result;
	}
}