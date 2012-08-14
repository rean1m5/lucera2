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
 * @author -Wooden-
 */
public class ExPledgeCrestLarge extends L2GameServerPacket
{
	private static final String	_S__FE_1B_EXPLEDGECRESTLARGE	= "[S] FE:1b ExPledgeCrestLarge [ddd b]";
	private int					_crestId;
	private byte[]				_data;

	public ExPledgeCrestLarge(int crestId, byte[] data)
	{
		_crestId = crestId;
		_data = data;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x28);

		writeD(0x00); //???
		writeD(_crestId);
		writeD(_data.length);

		writeB(_data);
	}

	@Override
	public String getType()
	{
		return _S__FE_1B_EXPLEDGECRESTLARGE;
	}
}