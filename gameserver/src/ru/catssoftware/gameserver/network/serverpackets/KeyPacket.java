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
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public final class KeyPacket extends L2GameServerPacket
{
	private static final String	_S__01_KEYPACKET	= "[S] 01 KeyPacket";

	private byte[]				_key;
	@SuppressWarnings("unused")
	private int					_id;
	private byte[] _data;
	private boolean _isLAMEPacket = false;
    public KeyPacket(byte data[])
    {
    	_data = data;
    	_isLAMEPacket = true;
    }

	public KeyPacket(byte[] key, int id)
	{
		_key = key;
		_id = id;
	}

	@Override
	public void writeImpl()
	{
		if(_isLAMEPacket) {
			writeC(0x00);
			writeC(_data == null ? 0x00 : 0x01);
	        if (_data != null)
	        {
	            writeB(_data);
	            writeD(0x01);
	            writeD(0x01);
	        }
	        return;
		}
		writeC(0x00);
		writeC(0x01); //0 - wrong protocol, 1 - protocol ok
		writeB(_key);
		writeD(0x01); // server id
		writeC(0x01);
		
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__01_KEYPACKET;
	}
}