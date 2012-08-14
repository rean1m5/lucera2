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
//TODO: Remove

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 *
 * @author  KenM
 */
public class ExEnchantSkillInfoDetail extends L2GameServerPacket
{
	private final int	_itemId;
	private final int	_itemCount;

	public ExEnchantSkillInfoDetail(int itemId, int itemCount)
	{
		_itemId = itemId;
		_itemCount = itemCount;
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] FE:5E ExEnchantSkillInfoDetail";
	}

	/**
	 * @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xfe);
		writeH(0x5e);

		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(_itemCount); // Count
		writeD(0);
		writeD(_itemId); // ItemId Required
		writeD(0);
	}

}
