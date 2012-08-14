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

public class PlaySound extends L2GameServerPacket
{
	private static final String	_S__9E_PlaySound	= "[S] 9E PlaySound [dSddddd]";
	private int					_mode;
	private String				_soundFile;
	private int					_unknown1;
	private int					_unknown2;
	private int					_x;
	private int					_y;
	private int					_z;

	public PlaySound(String soundFile)
	{
		_mode = 0;
		_soundFile = soundFile;
		_unknown1 = 0;
		_unknown2 = 0;
		_x = 0;
		_y = 0;
		_z = 0;
	}

	public PlaySound(int mode, String soundFile)
	{
		_mode = mode;
		_soundFile = soundFile;
		_unknown1 = 0;
		_unknown2 = 0;
		_x = 0;
		_y = 0;
		_z = 0;
	}

	public PlaySound(int mode, String soundFile, int unknown1, int unknown2, int x, int y, int z)
	{
		_mode = mode;
		_soundFile = soundFile;
		_unknown1 = unknown1;
		_unknown2 = unknown2;
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x98);
		writeD(_mode); // 0 for quest sounds, 1 for music
		writeS(_soundFile);
		writeD(_unknown1); //unknown 0 for quest; 1 for ship;
		writeD(_unknown2); //0 for quest; objectId of ship
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
		return _S__9E_PlaySound;
	}
}
