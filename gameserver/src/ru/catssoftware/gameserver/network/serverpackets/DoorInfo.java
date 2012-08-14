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

import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;

public class DoorInfo extends L2GameServerPacket
{
	private static final String	_S__4C_DOORINFO	= "[S] 4c DoorInfo [ddd]";
	private L2DoorInstance		_door;
	private final boolean _showHp;

	public DoorInfo(L2DoorInstance door, boolean showHp)
	{
		_door = door;
		_showHp = showHp;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4c);
		writeD(_door.getObjectId());
		writeD(_door.getDoorId());
		writeD(1);
		writeD(1);
		writeD(_door.isEnemy() ? 1 : 0);
		writeD((int)_door.getCurrentHp());
		writeD(_door.getMaxHp());
		writeD(_showHp ? 1 : 0);
		writeD(_door.getDamage());
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__4C_DOORINFO;
	}
}
