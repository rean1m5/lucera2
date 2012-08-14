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
//TODO: Remove?
/**
 * Format:(ch)
 * @author  Crion/kombat
 */
public class ExClosePartyRoom extends L2GameServerPacket
{
	public static final ExClosePartyRoom	STATIC_PACKET	= new ExClosePartyRoom();

	private ExClosePartyRoom()
	{
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x09);
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "FE_09_ExClosePartyRoom";
	}
}
