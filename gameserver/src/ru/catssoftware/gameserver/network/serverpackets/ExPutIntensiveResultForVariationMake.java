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

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * Format: (ch)ddddd
 *
 */
public class ExPutIntensiveResultForVariationMake extends L2GameServerPacket
{
	private static final String	S_FE_54_EXPUTINTENSIVERESULTFORVARIATIONMAKE	= "[S] FE:53 ExPutIntensiveResultForVariationMake";

	private int					_refinerItemObjId;
	private int					_lifestoneItemId;
	private int					_gemstoneItemId;
	private int					_gemstoneCount;
	private int					_unk2;

	public ExPutIntensiveResultForVariationMake(int refinerItemObjId, int lifeStoneId, int gemstoneItemId, int gemstoneCount)
	{
		_refinerItemObjId = refinerItemObjId;
		_lifestoneItemId = lifeStoneId;
		_gemstoneItemId = gemstoneItemId;
		_gemstoneCount = gemstoneCount;
		_unk2 = 1;
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xFE);
		writeH(0x53);
		writeD(_refinerItemObjId);
		writeD(_lifestoneItemId);
		writeD(_gemstoneItemId);
		writeD(_gemstoneCount);
		writeD(_unk2);
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return S_FE_54_EXPUTINTENSIVERESULTFORVARIATIONMAKE;
	}

}
