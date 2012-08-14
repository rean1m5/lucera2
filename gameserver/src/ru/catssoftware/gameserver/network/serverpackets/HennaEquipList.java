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

import java.util.List;

import ru.catssoftware.gameserver.datatables.HennaTreeTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.item.L2Henna;


public class HennaEquipList extends L2GameServerPacket
{
	private static final String	_S__E2_HennaEquipList	= "[S] E2 HennaEquipList";

	private final L2PcInstance	_player;
	private final List<L2Henna>	_hennaEquipList;

	public HennaEquipList(L2PcInstance player)
	{
		_player = player;
		_hennaEquipList = HennaTreeTable.getInstance().getAvailableHenna(player);
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xe2);
		writeD(_player.getAdena());
		if(activeChar.getLevel()<40)
			writeD(2);
		else
			writeD(3); //available equip slot
		
		//writeD(10);    // total amount of symbol available which depends on difference classes
		writeD(_hennaEquipList.size());

		for (L2Henna element : _hennaEquipList)
		{
			/*
			 * Player must have at least one dye in inventory
			 * to be able to see the henna that can be applied with it.
			 */
			/*
			 * Why? Remove comment if you want, but imho it's stupidity.
			 */
			if ((_player.getInventory().getItemByItemId(element.getItemId())) != null)
			{
				writeD(element.getSymbolId()); //symbolid
				writeD(element.getItemId()); //itemid of dye
				writeD(element.getAmount()); //amount of dye require
				writeD(element.getPrice()); //amount of aden require
				writeD(1); //meet the requirement or not
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
			}
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__E2_HennaEquipList;
	}
}
