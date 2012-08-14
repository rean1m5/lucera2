//TODO: Remove?

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

import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;

/**
 * @author Kerberos
 * 
 */
public class VehicleStarted extends L2GameServerPacket
{
	private L2BoatInstance	_boat;

	private int				_state;

	/**
	 * @param instance
	 */
	public VehicleStarted(L2BoatInstance boat, int state)
	{
		_boat = boat;
		_state = state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xC0);
		writeD(_boat.getObjectId());
		writeD(_state);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] C0 VehicleStarted";
	}
}