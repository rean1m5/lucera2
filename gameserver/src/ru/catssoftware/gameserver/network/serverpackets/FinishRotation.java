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
 *
 * format  dd
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class FinishRotation extends L2GameServerPacket
{
	private static final String	_S__78_FINISHROTATION	= "[S] 63 FinishRotation";
	private int					_heading;
	private int					_charObjId;

	public FinishRotation(L2Character cha)
	{
		_charObjId = cha.getObjectId();
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x61);
		writeD(_charObjId);
		writeD(_heading);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__78_FINISHROTATION;
	}
}
