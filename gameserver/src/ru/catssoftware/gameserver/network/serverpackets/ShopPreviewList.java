//TODO: Remove

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

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.item.L2Item;


public class ShopPreviewList extends L2GameServerPacket
{
	private static final String	S_F5_SHOPPREVIEWLIST	= "[S] F5 ShopPreviewList";
	private int					_listId;
	private L2ItemInstance[]	_list;
	private int					_money;
	private int					_expertise;

	public ShopPreviewList(L2TradeList list, int currentMoney, int expertiseIndex)
	{
		_listId = list.getListId();
		List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_expertise = expertiseIndex;
	}

	public ShopPreviewList(List<L2ItemInstance> lst, int listId, int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xf5);
		writeC(0xc0); // ?
		writeC(0x13); // ?
		writeC(0x00); // ?
		writeC(0x00); // ?
		writeD(_money); // current money
		writeD(_listId);

		int newlength = 0;
		for (L2ItemInstance item : _list)
		{
			if (item.getItem().getCrystalType() <= _expertise && item.isEquipable())
				newlength++;
		}
		writeH(newlength);

		for (L2ItemInstance item : _list)
		{
			if (item.getItem().getCrystalType() <= _expertise && item.isEquipable())
			{
				writeD(item.getItemDisplayId());
				writeH(item.getItem().getType2()); // item type2

				if (item.getItem().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					writeH(item.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
				}
				else
				{
					writeH(0x00); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
				}
				writeD(Config.WEAR_PRICE);
			}
		}
	}

	@Override
	public String getType()
	{
		return S_F5_SHOPPREVIEWLIST;
	}
}
