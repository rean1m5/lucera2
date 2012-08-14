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

import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager.SiegeSpawn;
import ru.catssoftware.gameserver.model.entity.Fort;
import javolution.util.FastList;


/**
 * @author  KenM
 */
public class ExShowFortressSiegeInfo extends L2GameServerPacket
{
	private final static String S_FE_17_EXSHOWFORTRESSSIEGEINFO = "[S] FE:17 ExShowFortressSiegeInfo";

	private int _fortId;
	private int _size;
	private Fort _fort;

	/**
	 * @param fort
	 */
	public ExShowFortressSiegeInfo(Fort fort)
	{
		_fort = fort;
		_fortId = fort.getFortId();
		_size = fort.getFortSize();
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return S_FE_17_EXSHOWFORTRESSSIEGEINFO;
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x17);

		writeD(_fortId); // Fortress Id
		writeD(_size); // Total Barracks Count
		FastList<SiegeSpawn> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(_fortId);
		if (commanders != null && !commanders.isEmpty())
		{
			switch (commanders.size())
			{
			case 3:
				switch (_fort.getSiege().getCommanders().get(_fortId).size())
				{
				case 0:
					writeD(0x03);
					break;
				case 1:
					writeD(0x02);
					break;
				case 2:
					writeD(0x01);
					break;
				case 3:
					writeD(0x00);
					break;
				}
				break;
			case 4: // TODO: change 4 to 5 once control room supported
				switch (_fort.getSiege().getCommanders().get(_fortId).size()) // TODO: once control room supported, update writeD(0x0x) to support 5th room
				{
				case 0:
					writeD(0x05);
					break;
				case 1:
					writeD(0x04);
					break;
				case 2:
					writeD(0x03);
					break;
				case 3:
					writeD(0x02);
					break;
				case 4:
					writeD(0x01);
					break;
				}
				break;
			}
		}
		else
		{
			for (int i = 0; i < _size; i++)
				writeD(0x00);
		}
	}
}
