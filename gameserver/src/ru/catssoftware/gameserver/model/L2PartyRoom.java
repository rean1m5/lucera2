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

import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExClosePartyRoom;
import ru.catssoftware.gameserver.network.serverpackets.ExManagePartyRoomMember;
import ru.catssoftware.gameserver.network.serverpackets.ExPartyRoomMember;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.PartyMatchDetail;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.lang.L2Math;
import javolution.util.FastList;

/**
 * This class represents a party room, used for party creation and party matching.
 * @author Myzreal
 * @author savormix
 */
public class L2PartyRoom
{
	private final int						_id;
	private final FastList<L2PcInstance>	_members;
	private int								_minLevel;
	private int								_maxLevel;
	private int								_lootDist;
	private int								_maxMembers;
	private String							_title;
	private L2Party							_party;

	public L2PartyRoom(int id, int minLevel, int maxLevel, int maxMembers, int lootDist, String title)
	{
		_id = id;
		setMinLevel(minLevel);
		setMaxLevel(maxLevel);
		setMaxMembers(maxMembers);
		_lootDist = lootDist;
		_title = title;
		_members = new FastList<L2PcInstance>();
		_party = null;
	}

	public final FastList<L2PcInstance> getMembers()
	{
		return _members;
	}

	public int getMemberCount()
	{
		return getMembers().size();
	}

	public L2PcInstance getLeader()
	{
		if (_party == null || _party.getLeader() == null)
			return getMembers().getFirst();
		else
			return _party.getLeader();
	}

	/**
	 * Verifies is player is eligible to join this party room.<BR>
	 * Does not specify a reason, does not send a message.
	 * @param activeChar a player
	 * @return whether the given player can join this room
	 * @see #tryJoin(L2PcInstance, L2PartyRoom, boolean)
	 */
	public final boolean canJoin(L2PcInstance activeChar)
	{
		return (activeChar.getPartyRoom() == null && activeChar.getParty() == null &&
				checkLevel(activeChar.getLevel()) && getMemberCount() < getMaxMembers());
	}

	public void addMember(L2PcInstance player)
	{
		if (getMembers().contains(player))
			return;

		PartyRoomManager.getInstance().removeFromWaitingList(player);
		broadcastPacket(new ExManagePartyRoomMember(ExManagePartyRoomMember.ADDED, player));
		broadcastPacket(new SystemMessage(SystemMessageId.S1_ENTERED_PARTY_ROOM).addPcName(player));
		updateRoomStatus(false);
		getMembers().add(player);
		player.setPartyRoom(this);
		player.sendPacket(new PartyMatchDetail(this));
		player.sendPacket(new ExPartyRoomMember(this, getMemberCount() == 1));
	}

	public void addMembers(L2Party party)
	{
		for (L2PcInstance player : party.getPartyMembersWithoutLeader())
		{
			getMembers().add(player);
			player.setPartyRoom(this);
		}
		updateRoomStatus(true);
	}

	public void removeMember(L2PcInstance member, boolean oust)
	{
		// the last member (leader) cannot be removed
		if (getMemberCount() == 1 || !getMembers().remove(member))
			return;

		member.setPartyRoom(null);
		member.sendPacket(ExClosePartyRoom.STATIC_PACKET);
		if (oust)
			member.sendPacket(SystemMessageId.OUSTED_FROM_PARTY_ROOM);
		else
			member.sendPacket(SystemMessageId.PARTY_ROOM_EXITED);

		SystemMessage sm;
		if (oust)
			sm = new SystemMessage(SystemMessageId.S1_KICKED_FROM_PARTY_ROOM);
		else
			sm = new SystemMessage(SystemMessageId.S1_LEFT_PARTY_ROOM);
		sm.addPcName(member);
		broadcastPacket(new ExManagePartyRoomMember(ExManagePartyRoomMember.REMOVED, member));
		broadcastPacket(sm);
		updateRoomStatus(false);
	}

	public void broadcastPacket(L2GameServerPacket packet)
	{
		for (L2PcInstance player : getMembers())
			player.sendPacket(packet);
	}

	public void broadcastPacket(L2GameServerPacket toLeader, L2GameServerPacket toMember)
	{
		L2PcInstance leader = getLeader();
		for (L2PcInstance player : getMembers())
			if (player == leader)
				player.sendPacket(toLeader);
			else
				player.sendPacket(toMember);
	}

	/** Broadcasts PartyRoomInfo and ExPartyRoomMember packets */
	public void updateRoomStatus(boolean playerList)
	{
		broadcastPacket(new PartyMatchDetail(this));
		if (playerList)
			broadcastPacket(new ExPartyRoomMember(this, true), new ExPartyRoomMember(this));
	}

	public int getId()
	{
		return _id;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public int getMaxMembers()
	{
		return _maxMembers;
	}

	public int getLootDist()
	{
		return _lootDist;
	}

	public String getTitle()
	{
		return _title;
	}

	public L2Party getParty()
	{
		return _party;
	}

	public void setParty(L2Party party)
	{
		_party = party;
		if (party == null)
			return;
		_party.setPartyRoom(this);
		_party.setLootDistribution(getLootDist());
		// room is created while in party
		if (getMemberCount() == 1)
			addMembers(party);
		// otherwise party members already in room
	}

	public void setMinLevel(int minLevel)
	{
		_minLevel = L2Math.limit(1, minLevel, 85);
	}

	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = L2Math.limit(1, maxLevel, 85);
	}

	public void setMaxMembers(int maxMembers)
	{
		_maxMembers = L2Math.limit(2, maxMembers, 12);
	}

	public void setLootDist(int lootDist)
	{
		_lootDist = lootDist;
		if (getParty() != null)
			getParty().setLootDistribution(lootDist);
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	/** @return L2 region ID (1-15) */
	public int getLocation()
	{
		return MapRegionManager.getInstance().getL2Region(getLeader());
	}

	/**
	 * @param restrict whether player enabled level restriction
	 * @param level player's level
	 * @return whether a player should see this room
	 */
	public boolean checkLevel(boolean restrict, int level)
	{
		if (restrict)
			return checkLevel(level);
		else
			return true;
	}

	/**
	 * @param level player's level
	 * @return whether a player is able to join
	 */
	public boolean checkLevel(int level)
	{
		return (level >= getMinLevel() && level <= getMaxLevel()); 
	}

	/**
	 * Verifies if player can join the given party room and either adds the player to the
	 * party room or sends a message why the player could not join the room.<BR>
	 * <I>Parameters may be <CODE>null</CODE>, which guarantees <CODE>false</CODE></I>
	 * @param activeChar a player
	 * @param room a party room
	 * @param checkForParty whether check if the player is in party/[room]
	 * @return true if player joined the given room, false otherwise
	 */
	public static final boolean tryJoin(L2PcInstance activeChar, L2PartyRoom room, boolean checkForParty)
	{
		if (activeChar == null)
			return false;

		if (checkForParty)
		{
			if (activeChar.getPartyRoom() != null || activeChar.getParty() != null)
			{
				activeChar.sendPacket(SystemMessageId.PARTY_ROOM_FORBIDDEN);
				return false;
			}
		}

		if (room == null)
		{
			activeChar.sendPacket(SystemMessageId.PARTY_ROOM_FORBIDDEN);
			return false;
		}
		else if (!room.checkLevel(activeChar.getLevel()))
		{
			activeChar.sendPacket(SystemMessageId.CANT_ENTER_PARTY_ROOM);
			return false;
		}
		else if (room.getMemberCount() >= room.getMaxMembers())
		{
			activeChar.sendPacket(SystemMessageId.PARTY_ROOM_FULL);
			return false;
		}
		room.addMember(activeChar);
		return true;
	}

	public static final int getPartyRoomState(L2PcInstance player)
	{
		L2PartyRoom room = player.getPartyRoom();
		if (room == null)
			return 0;
		if (room.getLeader() == player)
			return 1;
		L2Party party = room.getParty();
		if (party != null && party == player.getParty())
			return 2;
		return 0;
	}
}
