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
package ru.catssoftware.gameserver.instancemanager;

import javolution.util.FastMap;
import javolution.util.FastSet;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExClosePartyRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Party room and party matching waiting list manager.
 * @author Myzreal
 * @author savormix
 */
public class PartyRoomManager
{
	public static final int ENTRIES_PER_PAGE = 64;

	private volatile int						_nextId;
	private final FastSet<L2PcInstance>			_waitingList;
	private final FastMap<Integer, L2PartyRoom>	_rooms;

	public PartyRoomManager()
	{
		_nextId = 1;
		_waitingList = new FastSet<L2PcInstance>();
		_rooms = new FastMap<Integer, L2PartyRoom>();
	}

	public static final PartyRoomManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private final FastSet<L2PcInstance> getWaitingList()
	{
		return _waitingList;
	}

	private final FastMap<Integer, L2PartyRoom> getPartyRooms()
	{
		return _rooms;
	}

	public final L2PartyRoom getPartyRoom(int roomId)
	{
		return getPartyRooms().get(roomId);
	}

	public void addToWaitingList(L2PcInstance player)
	{
		if (getWaitingList().add(player))
		{
			player.setLookingForParty(true);
			player.broadcastUserInfo(true);
		}
	}

	public void removeFromWaitingList(L2PcInstance player)
	{
		getWaitingList().remove(player);
		player.setLookingForParty(false);
		player.broadcastUserInfo(true);
	}

	public List<L2PcInstance> getWaitingList(int minLevel, int maxLevel)
	{
		ArrayList<L2PcInstance> list = new ArrayList<L2PcInstance>();
		for (L2PcInstance pc : getWaitingList())
			if (pc.getLevel() >= minLevel && pc.getLevel() <= maxLevel)
				list.add(pc);
		return list;
	}

	/**
	 * Creates a party room without checking conditions. Removes leader from waiting list
	 * and adds him to the party room.
	 * @param leader Party room's leader
	 * @param minLevel Minimum level to join
	 * @param maxLevel Maximum level to join
	 * @param maxMembers Maximum member count
	 * @param lootDist Loot distribution type
	 * @param title Party room's title
	 * @return the newly created party room with leader as the only member
	 */
	public void createRoom(L2PcInstance leader, int minLevel, int maxLevel, int maxMembers, int lootDist, String title)
	{
		L2PartyRoom room = new L2PartyRoom(_nextId++, minLevel, maxLevel, maxMembers, lootDist, title);
		room.addMember(leader);
		leader.setLookingForParty(true);
		leader.broadcastUserInfo(true);
		room.setParty(leader.getParty());
		getPartyRooms().put(room.getId(), room);
	}

	/**
	 * Disbands the party room with the given ID, if it exists.
	 * @param roomId Party Room's ID
	 */
	public void removeRoom(int roomId)
	{
		L2PartyRoom room = getPartyRooms().remove(roomId);
		if (room == null)
			return;

		L2Party party = room.getParty();
		if (party != null)
		{
			room.setParty(null);
			party.setPartyRoom(null);
		}

		for (L2PcInstance member : room.getMembers())
		{
			member.setPartyRoom(null);
			member.sendPacket(ExClosePartyRoom.STATIC_PACKET);
			member.sendPacket(SystemMessageId.PARTY_ROOM_DISBANDED);
		}
	}

	public List<L2PartyRoom> getRooms(L2PcInstance player)
	{
		return getRooms(player.getPartyMatchingRegion(),
				MapRegionManager.getInstance().getL2Region(player),
				player.getPartyMatchingLevelRestriction(), player.getLevel());
	}

	public List<L2PartyRoom> getRooms(int reqRegion, int curRegion, boolean lvlRestrict, int lvl)
	{
		if (reqRegion == -2) // find rooms near player
			return getRoomsNearby(curRegion, lvlRestrict, lvl);

		ArrayList<L2PartyRoom> list = new ArrayList<L2PartyRoom>();
		for (L2PartyRoom room : getPartyRooms().values())
		{
			// find rooms in a specific region
			if (reqRegion > 0 && room.getLocation() != reqRegion)
				continue;
			else if (!room.checkLevel(lvlRestrict, lvl))
				continue;
			else
				list.add(room);
		}
		return list;
	}

	public List<L2PartyRoom> getRoomsNearby(int region, boolean lvlRestrict, int lvl)
	{
		ArrayList<L2PartyRoom> list = new ArrayList<L2PartyRoom>();
		for (L2PartyRoom room : getPartyRooms().values())
		{
			if (room.getLocation() != region || !room.checkLevel(lvlRestrict, lvl))
				continue;
			else
				list.add(room);
		}
		return list;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PartyRoomManager _instance = new PartyRoomManager();
	}
}
