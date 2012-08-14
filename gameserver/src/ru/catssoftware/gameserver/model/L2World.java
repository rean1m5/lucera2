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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.Shutdown.ShutdownModeType;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.tools.geometry.Point3D;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.LinkedBunch;
import ru.catssoftware.util.concurrent.L2Collection;
import ru.catssoftware.util.concurrent.L2ReadWriteCollection;

/**
 * This class ...
 * 
 * @version $Revision: 1.21.2.5.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2World
{
	private static final Logger _log = Logger.getLogger(L2World.class);

	public static final int SHIFT_BY = 12;

	/** Map dimensions */
	public static final int MAP_MIN_X = -163840;
	public static final int MAP_MAX_X = 229375;
	public static final int MAP_MIN_Y = -262144;
	public static final int MAP_MAX_Y = 294911;
	public static final int MAP_MIN_Z = -32768;
	public static final int MAP_MAX_Z = 32767;

	public static final int WORLD_SIZE_X = L2World.MAP_MAX_X - L2World.MAP_MIN_X + 1 >> 15; //SHIFT_BY;
	public static final int WORLD_SIZE_Y = L2World.MAP_MAX_Y - L2World.MAP_MIN_Y + 1 >> 15; // SHIFT_BY;

	public static final int SHIFT_BY_FOR_Z = 9;

	/** calculated offset used so top left region is 0,0 */
	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	// public static final int OFFSET_Z = Math.abs(MAP_MIN_Z >> SHIFT_BY_FOR_Z);

	/** number of regions */
	public static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	public static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;
	//public static final int REGIONS_Z = (MAP_MAX_Z >> SHIFT_BY_FOR_Z) + OFFSET_Z;

	private static L2World _instance; 

	public static L2World getInstance()
	{
		if(_instance == null)
			_instance = new L2World();
		return _instance;
	}

	/** all visible objects */
	private final L2Collection<L2Object> _objects = new L2ReadWriteCollection<L2Object>();

	/** all the players in game */
	private Map<String, L2PcInstance> _players;

	/** pets and their owner id */
	private Map<Integer, L2PetInstance> _pets;

	private L2WorldRegion[][] _worldRegions ;

/*	private class AutoSaveTask implements Runnable {
		@Override
		public void run() {
			for(L2PcInstance pc : getAllPlayers()) try {
				if(pc.isOfflineTrade() || pc.isOnline()==0 || pc.getClient()==null)
					continue;
				long period = Config.CHAR_STORE_INTERVAL * 60000L;
				long delay = pc._lastStore + period - System.currentTimeMillis();
				if (delay <= 0) try	{
						pc.store();
					} catch (Exception e) {
						_log.warn("AutoSaveTask:", e);
					}
			} catch(Exception e) { }
			ThreadPoolManager.getInstance().schedule(this, Config.CHAR_STORE_INTERVAL * 60000L);
		}
		
	}
	*/
	private L2World()
	{
		
		_log.info("L2World: Setting up World Regions");
		_players =  new FastMap<String, L2PcInstance>().setShared(true);
		_pets = new FastMap<Integer, L2PetInstance>().setShared(true);
		_worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];
		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
				_worldRegions[i][j] = new L2WorldRegion(i, j);
		}

		for (int x = 0; x <= REGIONS_X; x++)
		{
			for (int y = 0; y <= REGIONS_Y; y++)
			{
				for (int a = -1; a <= 1; a++)
				{
					for (int b = -1; b <= 1; b++)
					{
						if (validRegion(x + a, y + b))
							_worldRegions[x + a][y + b].addSurroundingRegion(_worldRegions[x][y]);
					}
				}
			}
		}

		_log.info("L2World: (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
	}

	/**
	 * Add L2Object object in _objects.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Withdraw an item from the warehouse, create an item</li>
	 * <li> Spawn a L2Character (PC, NPC, Pet)</li>
	 * <BR>
	 */
	public void storeObject(L2Object object)
	{
		if(object==null) 
			return;
		L2Object obj = _objects.get(object.getObjectId());
		if(obj!=null) {
			if (obj !=object)
			{
				if (_log.isDebugEnabled())
					_log.warn("[L2World] objectId " + object.getObjectId() + " already exist in OID map!");
				removeObject(object);
			} else
				return;
		}

		_objects.add(object);
	}

	/**
	 * Remove L2Object object from _objects of L2World.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Delete item from inventory, tranfer Item from inventory to warehouse</li>
	 * <li> Crystallize item</li>
	 * <li> Remove NPC/PC/Pet from the world</li>
	 * <BR>
	 *
	 * @param object L2Object to remove from _objects of L2World
	 */
	public void removeObject(L2Object object)
	{
		_objects.remove(object); // suggestion by whatev
	}

	public void removeObjects(List<L2Object> list)
	{
		for (L2Object o : list)
			removeObject(o); // suggestion by whatev
	}

	public void removeObjects(L2Object[] objects)
	{
		for (L2Object o : objects)
			removeObject(o); // suggestion by whatev
	}

	public L2Object findObject(int objectId)
	{
		return _objects.get(objectId);
	}

	public boolean findObject(L2Object obj)
	{
		if (obj!=null && _objects.contains(obj))
			return true;
		return false;
	}
	
	public L2Character findCharacter(int objectId)
	{
		L2Object obj = _objects.get(objectId);

		if (obj instanceof L2Character)
			return (L2Character)obj;

		return null;
	}

	public L2PcInstance findPlayer(int objectId)
	{
		L2Object obj = _objects.get(objectId);

		if (obj instanceof L2PcInstance)
			return (L2PcInstance)obj;

		return null;
	}

	public L2Object[] getAllVisibleObjects()
	{
		return _objects.toArray(new L2Object[_objects.size()]);
	}

	/**
	 * Get the count of all visible objects in world.<br>
	 * <br>
	 *
	 * @return count off all L2World objects
	 */
	public final int getAllVisibleObjectsCount()
	{
		return _objects.size();
	}

	/**
	 * Return a collection containing all players in game.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Read-only, please! </B></FONT><BR>
	 * <BR>
	 */
	public Collection<L2PcInstance> getAllPlayers()
	{
		return _players.values();
	}

	/**
	 * Return how many players are online.<BR>
	 * <BR>
	 * 
	 * @return number of online players.
	 */
	public int getAllPlayersCount()
	{
		return _players.size();
	}

	/**
	 * Return the player instance corresponding to the given name.<BR>
	 * <BR>
	 *
	 * @param name Name of the player to get Instance
	 */
	public L2PcInstance getPlayer(String name)
	{
		return _players.get(name.toLowerCase());
	}

	/**
	 * Return the player instance corresponding to the given objectId.<BR>
	 * <BR>
	 *
	 * @param objectId ID of the player to get Instance
	 */
	public L2PcInstance getPlayer(int objectId)
	{
		L2Object object = _objects.get(objectId);
		return object instanceof L2PcInstance ? (L2PcInstance)object : null;
	}

	/**
	 * Return a collection containing all pets in game.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Read-only, please! </B></FONT><BR>
	 * <BR>
	 */
	public Collection<L2PetInstance> getAllPets()
	{
		return _pets.values();
	}

	/**
	 * Return the pet instance from the given ownerId.<BR>
	 * <BR>
	 *
	 * @param ownerId ID of the owner
	 */
	public L2PetInstance getPet(int ownerId)
	{
		return _pets.get(ownerId);
	}

	/**
	 * Add the given pet instance from the given ownerId.<BR>
	 * <BR>
	 *
	 * @param ownerId ID of the owner
	 * @param pet L2PetInstance of the pet
	 */
	public L2PetInstance addPet(int ownerId, L2PetInstance pet)
	{
		return _pets.put(ownerId, pet);
	}

	/**
	 * Remove the given pet instance.<BR>
	 * <BR>
	 *
	 * @param ownerId ID of the owner
	 */
	public void removePet(int ownerId)
	{
		_pets.remove(ownerId);
	}

	/**
	 * Remove the given pet instance.<BR>
	 * <BR>
	 *
	 * @param pet the pet to remove
	 */
	public void removePet(L2PetInstance pet)
	{
		_pets.values().remove(pet);
	}

	/**
	 * Add a L2Object in the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in
	 * <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_players</B> of L2World, in <B>_players</B> of his current
	 * L2WorldRegion and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the L2Object object in _players* of L2World </li>
	 * <li>Add the L2Object object in _gmList** of GmListTable </li>
	 * <li>Add object in _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters </li>
	 * <BR>
	 * <li>If object is a L2Character, add all surrounding L2Object in its _knownObjects and all surrounding
	 * L2PcInstance in its _knownPlayer </li>
	 * <BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object in _visibleObjects and _players*
	 * of L2WorldRegion (need synchronisation)</B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _objects and _players* of
	 * L2World (need synchronisation)</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Drop an Item </li>
	 * <li> Spawn a L2Character</li>
	 * <li> Apply Death Penalty of a L2PcInstance </li>
	 * <BR>
	 * <BR>
	 *
	 * @param object L2object to add in the world
	 * @param newRegion L2WorldRegion in wich the object will be add (not used)
	 * @param dropper L2Character who has dropped the object (if necessary)
	 */

	public void addVisibleObject(L2Object object, L2Character dropper)
	{
		//FIXME: this code should be obsoleted by protection in putObject func...
		if (object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			L2PcInstance old = getPlayer(player.getName());

			if (old != null && old != player)
			{
				_log.warn("Duplicate character!? Closing both characters (" + player.getName() + ")");

				new Disconnection(player).defaultSequence(true);
				new Disconnection(old).defaultSequence(true);
				return;
			}

			addToAllPlayers(player);
		}

		if (!object.getPosition().getWorldRegion().isActive())
			return;

		for (L2Object element : getVisibleObjects(object, 2000))
		{
			element.getKnownList().addKnownObject(object, dropper);
			object.getKnownList().addKnownObject(element, dropper);
		}
	}

	/**
	 * Add the L2PcInstance to _players of L2World.<BR>
	 * <BR>
	 */
	public void addToAllPlayers(L2PcInstance cha)
	{
		_players.put(cha.getName().toLowerCase(), cha);
	}

	/**
	 * Remove the L2PcInstance from _players of L2World.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Remove a player fom the visible objects </li>
	 * <BR>
	 */
	public void removeFromAllPlayers(L2PcInstance cha)
	{
		if (cha != null && !cha.isTeleporting())
			_players.remove(cha.getName().toLowerCase());
	}

	/**
	 * Remove a L2Object from the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in
	 * <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_players</B> of L2World, in <B>_players</B> of his current
	 * L2WorldRegion and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Object object from _players* of L2World </li>
	 * <li>Remove the L2Object object from _visibleObjects and _players* of L2WorldRegion </li>
	 * <li>Remove the L2Object object from _gmList** of GmListTable </li>
	 * <li>Remove object from _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters </li>
	 * <BR>
	 * <li>If object is a L2Character, remove all L2Object from its _knownObjects and all L2PcInstance from its
	 * _knownPlayer </li>
	 * <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _objects of L2World</B></FONT><BR>
	 * <BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Pickup an Item </li>
	 * <li> Decay a L2Character</li>
	 * <BR>
	 * <BR>
	 *
	 * @param object L2object to remove from the world
	 * @param oldRegion L2WorldRegion in wich the object was before removing
	 */
	public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion)
	{
		if (object == null || oldRegion == null)
			return;

		oldRegion.removeVisibleObject(object);

		// Go through all surrounding L2WorldRegion L2Characters
		for (L2WorldRegion reg : oldRegion.getSurroundingRegions())
		{
			for (L2Object obj : reg.getVisibleObjects())
			{
				obj.getKnownList().removeKnownObject(object);
				object.getKnownList().removeKnownObject(obj);
			}
		}

		object.getKnownList().removeAllKnownObjects();

		if (object instanceof L2PcInstance)
			removeFromAllPlayers((L2PcInstance)object);
	}

	/**
	 * Return all visible objects of the L2WorldRegions in the circular area (radius) centered on the object.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in
	 * order to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Define the aggrolist of monster </li>
	 * <li> Define visible objects of a L2Object </li>
	 * <li> Skill : Confusion... </li>
	 * <BR>
	 *
	 * @param object L2object that determine the center of the circular area
	 * @param radius Radius of the circular area
	 */
	public L2Object[] getVisibleObjects(L2Object object, int radius)
	{
		if (object == null)
			return L2Object.EMPTY_ARRAY;

		final L2WorldRegion selfRegion = object.getWorldRegion();

		if (selfRegion == null)
			return L2Object.EMPTY_ARRAY;

		final int x = object.getX();
		final int y = object.getY();
		final int sqRadius = radius * radius;

		LinkedBunch<L2Object> result = new LinkedBunch<L2Object>();

		for (L2WorldRegion region : selfRegion.getSurroundingRegions())
		{
			for (L2Object obj : region.getVisibleObjects())
			{
				if (obj == null || obj == object || !obj.isVisible())
					continue;

				final int dx = obj.getX() - x;
				final int dy = obj.getY() - y;

				if (dx * dx + dy * dy < sqRadius)
					result.add(obj);
			}
		}

		return result.moveToArray(new L2Object[result.size()]);
	}

	/**
	 * Return all visible objects of the L2WorldRegions in the spheric area (radius) centered on the object.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in
	 * order to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Define the target list of a skill </li>
	 * <li> Define the target list of a polearme attack </li>
	 * <BR>
	 * <BR>
	 *
	 * @param object L2object that determine the center of the circular area
	 * @param radius Radius of the spheric area
	 */
	public L2Object[] getVisibleObjects3D(L2Object object, int radius) {
		return getVisibleObjects3D(object,radius,0);
	}
	
	public L2Object[] getVisibleObjects3D(L2Object object, int radius, int instanceId)
	{
		if (object == null)
			return L2Object.EMPTY_ARRAY;

		final L2WorldRegion selfRegion = object.getWorldRegion();

		if (selfRegion == null)
			return L2Object.EMPTY_ARRAY;

		final int x = object.getX();
		final int y = object.getY();
		final int z = object.getZ();
		final int sqRadius = radius * radius;

		LinkedBunch<L2Object> result = new LinkedBunch<L2Object>();

		for (L2WorldRegion region : selfRegion.getSurroundingRegions())
		{
			for (L2Object obj : region.getVisibleObjects())
			{
				if (obj == null || obj == object || !obj.isVisible() || obj.getInstanceId()!=instanceId)
					continue;
				

				final int dx = obj.getX() - x;
				final int dy = obj.getY() - y;
				final int dz = obj.getZ() - z;

				if (dx * dx + dy * dy + dz * dz < sqRadius)
					result.add(obj);
			}
		}

		return result.moveToArray(new L2Object[result.size()]);
	}

	/**
	 * Return all visible players of the L2WorldRegion object's and of its surrounding L2WorldRegion.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in
	 * order to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Find Close Objects for L2Character </li>
	 * <BR>
	 *
	 * @param object L2object that determine the current L2WorldRegion
	 */
	public L2PlayableInstance[] getVisiblePlayable(L2Object object)
	{
		if (object == null)
			return L2PlayableInstance.EMPTY_ARRAY;

		final L2WorldRegion selfRegion = object.getWorldRegion();

		if (selfRegion == null)
			return L2PlayableInstance.EMPTY_ARRAY;

		LinkedBunch<L2PlayableInstance> result = new LinkedBunch<L2PlayableInstance>();

		for (L2WorldRegion region : selfRegion.getSurroundingRegions())
		{
			for (L2PlayableInstance obj : region.getVisiblePlayables())
			{
				if (obj == null || obj == object || !obj.isVisible())
					continue;

				result.add(obj);
			}
		}

		return result.moveToArray(new L2PlayableInstance[result.size()]);
	}

	/**
	 * Calculate the current L2WorldRegions of the object according to its position (x,y).<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Set position of a new L2Object (drop, spawn...) </li>
	 * <li> Update position of a L2Object after a mouvement </li>
	 * <BR>
	 *
	 * @param point position of the object
	 */
	public L2WorldRegion getRegion(Point3D point)
	{
		return _worldRegions[(point.getX() >> SHIFT_BY) + OFFSET_X][(point.getY() >> SHIFT_BY) + OFFSET_Y];
	}

	public L2WorldRegion getRegion(int x, int y)
	{
		return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
	}

	/**
	 * Returns the whole 2d array containing the world regions
	 *
	 * @return
	 */
	public L2WorldRegion[][] getAllWorldRegions()
	{
		return _worldRegions;
	}

	/**
	 * Check if the current L2WorldRegions of the object is valid according to its position (x,y).<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Init L2WorldRegions </li>
	 * <BR>
	 *
	 * @param x X position of the object
	 * @param y Y position of the object
	 * @return True if the L2WorldRegion is valid
	 */
	private boolean validRegion(int x, int y)
	{
		return (0 <= x && x <= REGIONS_X && 0 <= y && y <= REGIONS_Y);
	}

	/**
	 * Deleted all spawns in the world.
	 */
	public synchronized void deleteVisibleNpcSpawns()
	{
		_log.info("Deleting all visible NPC's.");

		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
				_worldRegions[i][j].deleteVisibleNpcSpawns();
		}

		_log.info("All visible NPC's deleted.");
	}

	public L2Object[] getObjectsInRadius(int x, int y,  int radius, int instanceId) {
		LinkedBunch<L2Object> result = new LinkedBunch<L2Object>();

		final L2WorldRegion selfRegion = getRegion(x,y);
		for (L2WorldRegion region : selfRegion.getSurroundingRegions())
		{
			for (L2Object obj : region.getVisibleObjects())
			{
				if (obj == null || !obj.isVisible() || obj.getInstanceId()!=instanceId)
					continue;

				final int dx = obj.getX() - x;
				final int dy = obj.getY() - y;

				if (dx * dx + dy * dy < radius)
					result.add(obj);
			}
		}

		return result.moveToArray(new L2Object[result.size()]);
	}
}