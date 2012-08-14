//TODO: Check

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

public class SpecialCamera extends L2GameServerPacket
{
	private static final String	_S__C7_SPECIALCAMERA	= "[S] C7 SpecialCamera";
	private int					_id;
	private int					_dist;
	private int					_yaw;
	private int					_pitch;
	private int					_time;
	private int					_duration;
	private int					_turn;
	private int					_rise;
	private int					_widescreen;
	private int					_unknown;

	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration) 
	{ 
		_id = id; 
		_dist = dist; 
		_yaw = yaw; 
		_pitch = pitch; 
		_time = time; 
		_duration = duration; 
		_turn = 0; 
		_rise = 0; 
		_widescreen = 0; 
		_unknown = 0; 
	} 

	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int widescreen, int unk) 
	{ 
		_id = id; 
		_dist = dist; 
		_yaw = yaw; 
		_pitch = pitch; 
		_time = time; 
		_duration = duration; 
		_turn = turn; 
		_rise = rise; 
		_widescreen = widescreen; 
		_unknown = unk; 
	} 

	@Override
	public void writeImpl()
	{
		writeC(0xC7);
		writeD(_id);
		writeD(_dist);
		writeD(_yaw);
		writeD(_pitch);
		writeD(_time);
		writeD(_duration);
		writeD(_turn); 
		writeD(_rise); 
		writeD(_widescreen); 
		writeD(_unknown);		
	}

	@Override
	public String getType()
	{
		return _S__C7_SPECIALCAMERA;
	}
}
