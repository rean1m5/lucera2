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
package ru.catssoftware.gameserver.model.actor.knownlist;

import java.util.Map;


import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2WorldRegion;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.util.L2Collections;
import ru.catssoftware.util.SingletonMap;


public class CharKnownList extends ObjectKnownList
{
	protected final L2Character _activeChar;
	
	private Map<Integer, L2Object> _knownObjects;
	private Map<Integer, L2PcInstance> _knownPlayers;
	
	public CharKnownList(L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if (object == null || object == getActiveChar())
			return false;
		if (getKnownObjects().containsKey(object.getObjectId()))
			return false;
		
		// Check if object is not inside distance to watch object
		if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), getActiveChar(), object, true))
			return false;
		
		// instance -1 for gms can see everything on all instances
		if (getActiveChar().getInstanceId() != -1 && getActiveChar().getInstanceId() != object.getInstanceId())
			return false;
		
		getKnownObjects().put(object.getObjectId(), object);
		
		if (object instanceof L2PcInstance)
			getKnownPlayers().put(object.getObjectId(), (L2PcInstance)object);
		
		return true;
	}
	
	public final boolean knowsObject(L2Object object)
	{
		return getActiveChar() == object || _knownObjects != null && _knownObjects.containsKey(object.getObjectId());
	}
	
	public final boolean knowsThePlayer(L2PcInstance player)
	{
		return getActiveChar() == player || _knownPlayers != null && _knownPlayers.containsKey(player.getObjectId());
	}
	
	public final L2Object getKnownObject(int objectId)
	{
		return getKnownObjects().get(objectId);
	}
	
	public final L2PcInstance getKnownPlayer(int objectId)
	{
		return getKnownPlayers().get(objectId);
	}
	
	@Override
	public void removeAllKnownObjects()
	{
		for (L2Object object : getKnownObjects().values())
		{
			removeKnownObject(object);
			object.getKnownList().removeKnownObject(getActiveChar());
		}
		
		getKnownObjects().clear();
		
		getKnownPlayers().clear();
		
		// Set _target of the L2Character to null
		// Cancel Attack or Cast
		getActiveChar().setTarget(null);
		
		// Cancel AI Task
		if (getActiveChar().hasAI())
			getActiveChar().setAI(null);
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (object == null)
			return false;
		
		if (getKnownObjects().remove(object.getObjectId()) == null)
			return false;
		
		if (object instanceof L2PcInstance)
			getKnownPlayers().remove(object.getObjectId());
		
		// If object is targeted by the L2Character, cancel Attack or Cast
		if (object == getActiveChar().getTarget())
			getActiveChar().setTarget(null);
		
		return true;
	}
	
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	public int getDistanceToForgetObject(L2Object object)
	{
		return 0;
	}
	
	public int getDistanceToWatchObject(L2Object object)
	{
		return 0;
	}
	
	public Iterable<L2Character> getKnownCharacters()
	{
		return L2Collections.filteredIterable(L2Character.class, getKnownObjects().values());
	}
	
	public Iterable<L2Character> getKnownCharactersInRadius(final int radius)
	{
		return L2Collections.filteredIterable(L2Character.class, getKnownObjects().values(),
			new L2Collections.Filter<L2Character>() {
				public boolean accept(L2Character obj)
				{
					if (!Util.checkIfInRange(radius, getActiveChar(), obj, true))
						return false;
					
					return obj instanceof L2PlayableInstance || obj instanceof L2NpcInstance;
				}
			});
	}
	
	public final Map<Integer, L2Object> getKnownObjects()
	{
		if (_knownObjects == null)
			_knownObjects = new SingletonMap<Integer, L2Object>().setShared();
		
		return _knownObjects;
	}
	
	public final Map<Integer, L2PcInstance> getKnownPlayers()
	{
		if (_knownPlayers == null)
			_knownPlayers = new SingletonMap<Integer, L2PcInstance>().setShared();
		return _knownPlayers;
	}
	
	public final Iterable<L2PcInstance> getKnownPlayersInRadius(final int radius)
	{
		return L2Collections.filteredIterable(L2PcInstance.class, getKnownPlayers().values(),
			new L2Collections.Filter<L2PcInstance>() {
				public boolean accept(L2PcInstance player)
				{
					return Util.checkIfInRange(radius, getActiveChar(), player, true);
				}
			});
	}
	
	@Override
	public final void tryAddObjects(L2Object[][] surroundingObjects)
	{
		if (surroundingObjects == null)
		{
			final L2WorldRegion reg = getActiveChar().getWorldRegion();
			
			if (reg == null)
				return;
			
			surroundingObjects = reg.getAllSurroundingObjects2DArray();
		}
		
		for (L2Object[] regionObjects : surroundingObjects)
		{
			for (L2Object object : regionObjects)
			{
				addKnownObject(object);
				object.getKnownList().addKnownObject(getActiveChar());
			}
		}
	}
	
	@Override
	public final void tryRemoveObjects(boolean forced)
	{
		for (L2Object object : getKnownObjects().values())
		{
			tryRemoveObject(object, forced);
		
			object.getKnownList().tryRemoveObject(getActiveChar(), forced);
		}
	}
	
	@Override
	public final void tryRemoveObject(L2Object obj, boolean forced)
	{
		if (obj!=null && obj.isVisible() && Util.checkIfInShortRadius(getDistanceToForgetObject(obj), getActiveChar(), obj, true) && !forced)
			return;
		
		if (obj instanceof L2BoatInstance && getActiveChar() instanceof L2PcInstance)
		{
			if (((L2BoatInstance)obj).getVehicleDeparture() == null)
				return;
			
			L2PcInstance pc = (L2PcInstance)getActiveChar();
			
			if (pc.isInBoat() && pc.getBoat() == obj)
				return;
		}
		
		removeKnownObject(obj);
	}
	
	private long _lastUpdate;
	
	public void updateKnownObjects()
	{
		updateKnownObjects(false);
	}
	
	public synchronized final void updateKnownObjects(boolean forced)
	{
		if (System.currentTimeMillis() - _lastUpdate < 100 && !forced)
			return;
		
		tryRemoveObjects(forced);
		tryAddObjects(null);
		
		_lastUpdate = System.currentTimeMillis();
	}
}
