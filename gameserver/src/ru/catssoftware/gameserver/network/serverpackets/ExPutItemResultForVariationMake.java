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

/**
 * Format: (ch)ddd
 *
 */
public class ExPutItemResultForVariationMake extends L2GameServerPacket
{
	private static final String	_S__FE_53_EXPUTITEMRESULTFORVARIATIONMAKE	= "[S] FE:52 ExPutItemResultForVariationMake";

	private int					_itemObjId;
	private int					_unk1;
	private int					_unk2;

	public ExPutItemResultForVariationMake(int itemObjId)
	{
		_itemObjId = itemObjId;
		_unk1 = 1;
		_unk2 = 1;
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x52);
		writeD(_itemObjId);
		writeD(_unk1);
		writeD(_unk2);
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_53_EXPUTITEMRESULTFORVARIATIONMAKE;
	}

}
