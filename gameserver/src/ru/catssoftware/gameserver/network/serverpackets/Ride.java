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

import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class Ride extends L2GameServerPacket
{
	private static final String	_S__8c_Ride		= "[S] 8C Ride";
	public static final int		ACTION_MOUNT	= 1;
	public static final int		ACTION_DISMOUNT	= 0;
	private final int			_id;
	private final int			_bRide;
	private final int			_rideType;
	private final int			_rideClassID;
	private final int			_x, _y, _z;

	/**
	 * 0x86 UnknownPackets         dddd
	 * @param _
	 */
	public Ride(L2PcInstance cha, boolean mount, int npcId)
	{
		_id = cha.getObjectId();
		_bRide = mount ? 1 : 0;
		_rideClassID = npcId + 1000000; // npcID

		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();

		// 1 for Strider ; 2 for wyvern
		if (PetDataTable.isStrider(npcId))
			_rideType = 1;
		else if (PetDataTable.isWyvern(npcId))
			_rideType = 2;
		else
			_rideType = 0;
	}

	public int getMountType()
	{
		return _rideType;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x86);
		writeD(_id);
		writeD(_bRide);
		writeD(_rideType);
		writeD(_rideClassID);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__8c_Ride;
	}
}