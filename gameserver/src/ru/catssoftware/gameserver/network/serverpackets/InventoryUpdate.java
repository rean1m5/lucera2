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

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.ItemInfo;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

import java.util.ArrayList;
import java.util.List;



public class InventoryUpdate extends L2GameServerPacket
{
	private static final String	_S__37_INVENTORYUPDATE	= "[S] 27 InventoryUpdate";

	private List<ItemInfo>		_items;

	public InventoryUpdate()
	{
		_items = new ArrayList<ItemInfo>();
	}

	/**
	 * @param items
	 */
	public InventoryUpdate(List<ItemInfo> items)
	{
		_items = items;
	}

	public void addItem(L2ItemInstance item)
	{
		if (item != null)
			_items.add(new ItemInfo(item));
	}

	public void addNewItem(L2ItemInstance item)
	{
		if (item != null)
			_items.add(new ItemInfo(item, 1));
	}

	public void addModifiedItem(L2ItemInstance item)
	{
		if (item != null)
			_items.add(new ItemInfo(item, 2));
	}

	public void addRemovedItem(L2ItemInstance item)
	{
		if (item != null)
			_items.add(new ItemInfo(item, 3));
	}

	public void addItems(List<L2ItemInstance> items)
	{
		if (items != null)
			for (L2ItemInstance item : items)
				if (item != null)
					_items.add(new ItemInfo(item));
	}

	public void addEquipItems(L2ItemInstance[] items)
	{
		if (items != null)
			for (L2ItemInstance item : items)
				if (item != null)
					_items.add(new ItemInfo(item, 2));
		}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		writeC(0x27);
		int count = _items.size();
		writeH(count);
		for (ItemInfo item : _items)
		{
			writeH(item.getChange());					// Update type : 01-add, 02-modify, 03-remove
			writeH(item.getItem().getType1());			// Item Type 1 : 00-weapon/ring/earring/necklace, 01-armor/shield, 04-item/questitem/adena

			writeD(item.getObjectId());					// ObjectId
			writeD(item.getItem().getItemDisplayId());	// ItemId
			writeD(item.getCount());					// Quantity
			writeH(item.getItem().getType2());			// Item Type 2 : 00-weapon, 01-shield/armor, 02-ring/earring/necklace, 03-questitem, 04-adena, 05-item
			writeH(item.getCustomType1());				// Filler (always 0)
			writeH(item.getEquipped());					// Equipped : 00-No, 01-yes
			writeD(item.getItem().getBodyPart());		// Slot : 0006-lr.ear, 0008-neck, 0030-lr.finger, 0040-head, 0100-l.hand, 0200-gloves, 0400-chest, 0800-pants, 1000-feet, 4000-r.hand, 8000-r.hand

			// Небольшое дополнение для олимпиады, если персонаж на олимпиаде и заточка больше допустимой - отсылает допустимую заточку.
			// !!! Влияет только на отображение.
			// !!! Обязательно отправлять пакет с соответствующей вещицей перед и после олимпиады.
			int enchant = item.getEnchant();
			if (Config.ALT_OLY_ENCHANT_LIMIT >= 0)
				if (client != null && client.getActiveChar() != null && client.getActiveChar().isInOlympiadMode())
					if (item.getEnchant() > Config.ALT_OLY_ENCHANT_LIMIT )
						enchant = Config.ALT_OLY_ENCHANT_LIMIT;

			writeH(enchant);					// Enchant level (pet level shown in control item)
			writeH(item.getCustomType2());				// Pet name exists or not shown in control item
			writeD(item.getAugemtationBonus());
			writeD(item.getMana());
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__37_INVENTORYUPDATE;
	}
}
