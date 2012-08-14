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

import ru.catssoftware.gameserver.model.L2Character;

public class MoveToLocation extends L2GameServerPacket
{
	private static final String	_S__2F_CHARMOVETOLOCATION	= "[S] 2f CharMoveToLocation [ddddddd]";

	private int					_charObjId, _x, _y, _z, _xDst, _yDst, _zDst;

	public MoveToLocation(L2Character cha)
	{
		_charObjId = cha.getObjectId();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_xDst = cha.getXdestination();
		_yDst = cha.getYdestination();
		_zDst = cha.getZdestination();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x01);

		writeD(_charObjId);

		writeD(_xDst);
		writeD(_yDst);
		writeD(_zDst);

		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	@Override
	public String getType()
	{
		return _S__2F_CHARMOVETOLOCATION;
	}
}
