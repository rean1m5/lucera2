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

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ValidateLocationInVehicle extends L2GameServerPacket
{
	private static final String	_S__73_ValidateLocationInVehicle	= "[S] 73 ValidateLocationInVehicle";
	private L2Character			_activeChar;

	/**
	 * 0x73 ValidateLocationInVehicle         hdd
	 * @param _characters
	 */
	public ValidateLocationInVehicle(L2Character player)
	{
		_activeChar = player;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x73);
		writeD(_activeChar.getObjectId());
		writeD(1343225858); //FIXME: verify vehicle object id ??
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__73_ValidateLocationInVehicle;
	}
}
