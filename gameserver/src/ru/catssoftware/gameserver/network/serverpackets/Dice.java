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

public class Dice extends L2GameServerPacket
{
	private static final String	_S__DA_Dice	= "[S] da Dice [dddddd]";
	private int					_charObjId;
	private int					_itemId;
	private int					_number;
	private int					_x;
	private int					_y;
	private int					_z;

	public Dice(int charObjId, int itemId, int number, int x, int y, int z)
	{
		_charObjId = charObjId;
		_itemId = itemId;
		_number = number;
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xD4);
		writeD(_charObjId); //object id of player
		writeD(_itemId); //	item id of dice (spade)  4625,4626,4627,4628
		writeD(_number); //number rolled
		writeD(_x); //x
		writeD(_y); //y
		writeD(_z); //z
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__DA_Dice;
	}
}
