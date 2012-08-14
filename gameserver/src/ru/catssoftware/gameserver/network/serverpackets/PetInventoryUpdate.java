package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.gameserver.model.ItemInfo;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

import javolution.util.FastList;

public class PetInventoryUpdate extends L2GameServerPacket
{
	private static final String	_S__37_INVENTORYUPDATE	= "[S] b3 InventoryUpdate";
	private List<ItemInfo>		_items;

	public PetInventoryUpdate(List<ItemInfo> items)
	{
		_items = items;
	}

	public PetInventoryUpdate()
	{
		this(new FastList<ItemInfo>());
	}

	public void addItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item));
	}

	public void addNewItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 1));
	}

	public void addModifiedItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 2));
	}

	public void addRemovedItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 3));
	}

	public void addItems(List<L2ItemInstance> items)
	{
		for (L2ItemInstance item : items)
			_items.add(new ItemInfo(item));
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xb3);
		int count = _items.size();
		writeH(count);
		for (ItemInfo item : _items)
		{
			writeH(item.getChange());
			writeH(item.getItem().getType1()); // item type1
			writeD(item.getObjectId());
			writeD(item.getItem().getItemDisplayId());
			writeD(item.getCount());
			writeH(item.getItem().getType2()); // item type2
			writeH(0x00); // ?
			writeH(item.getEquipped());
			writeD(item.getItem().getBodyPart()); // rev 415   slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(item.getEnchant()); // enchant level
			writeH(0x00); // ?
		}
	}

	@Override
	public String getType()
	{
		return _S__37_INVENTORYUPDATE;
	}
}