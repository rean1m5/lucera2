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
 * @author nBd
 */
public class ExPutEnchantTargetItemResult extends L2GameServerPacket
{
	private static final String _S__81_EXPUTENCHANTTARGETITEMRESULT = "[S] 81 ExPutEnchantTargetItemResult";

	private int _result;
	private int _crystal;
	private int _count;

	public ExPutEnchantTargetItemResult(int result, int crystal, int count)
	{
		_result = result;
		_crystal = crystal;
		_count = count;
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__81_EXPUTENCHANTTARGETITEMRESULT;
	}

	/**
	 * @see ru.catssoftwaregameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0x81);
		writeD(_result);
		writeD(_crystal);
		writeD(_count);
	}
}