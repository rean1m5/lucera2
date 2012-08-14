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

import ru.catssoftware.gameserver.GameTimeController;

public class ClientSetTime extends L2GameServerPacket
{
	private static final String			_S__F2_CLIENTSETTIME	= "[S] f2 ClientSetTime [dd]";
	public static final ClientSetTime	STATIC_PACKET			= new ClientSetTime();

	private ClientSetTime(){}

	@Override
	protected final void writeImpl()
	{
		writeC(0xec);
		writeD(GameTimeController.getInstance().getGameTime()); // time in client minutes
		writeD(6); //constant to match the server time( this determines the speed of the client clock)
	}

	@Override
	public String getType()
	{
		return _S__F2_CLIENTSETTIME;
	}
}
