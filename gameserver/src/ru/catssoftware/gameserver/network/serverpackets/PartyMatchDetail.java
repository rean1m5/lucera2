//TODO: Check 

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
package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2PartyRoom;

/**
 * Format:(c) dddddds
 * @author  Crion/kombat
 */
public class PartyMatchDetail extends L2GameServerPacket
{
	private static final String	_S__B0_PARTYMATCHDETAIL	= "[S] 9D PartyMatchDetail";

	private final L2PartyRoom _room;

	public PartyMatchDetail(L2PartyRoom room)
	{
		_room = room;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x97);

		writeD(_room.getId());
		writeD(_room.getMaxMembers());
		writeD(_room.getMinLevel());
		writeD(_room.getMaxLevel());
		writeD(_room.getLootDist());
		writeD(_room.getLocation()); // region
		writeS(_room.getTitle());
	}

	@Override
	public String getType()
	{
		return _S__B0_PARTYMATCHDETAIL;
	}
}
