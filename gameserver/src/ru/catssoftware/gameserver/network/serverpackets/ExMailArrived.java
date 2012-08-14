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
public class ExMailArrived extends L2GameServerPacket
{
	private static final String			_S__FE_2E_EXMAILARRIVED	= "[S] FE:2e ExMailArrived []";
	public static final ExMailArrived	STATIC_PACKET			= new ExMailArrived();

	private ExMailArrived()
	{
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2d);
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_2E_EXMAILARRIVED;
	}
}