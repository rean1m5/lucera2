package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class ItemList extends L2GameServerPacket
{
	private static final String	_S__27_ITEMLIST	= "[S] 1b ItemList";
	private L2ItemInstance[]	_items;
	private boolean				_showWindow;

	public ItemList(L2PcInstance cha, boolean showWindow)
	{
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
		cha.sendPacket(new UserInfo(cha));
	}

	public ItemList(L2ItemInstance[] items, boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x1b);
		writeH(_showWindow ? 0x01 : 0x00);

		int count = _items.length;
		writeH(count);

		int items = 0;
		for (L2ItemInstance temp : _items)
		{
			if (temp == null || temp.getItem() == null)
				continue;

			writeH(temp.getItem().getType1()); // item type1

			writeD(temp.getObjectId());
			writeD(temp.getItemDisplayId());
			
			writeD(temp.getCount());
			writeH(temp.getItem().getType2()); // item type2
			writeH(temp.getCustomType1()); // item type3
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(temp.getItem().getBodyPart());

			writeH(temp.getEnchantLevel()); // enchant level
			//race tickets
			writeH(temp.getCustomType2()); // item type3

			if (temp.isAugmented())
				writeD(temp.getAugmentation().getAugmentationId());
			else
				writeD(0x00);

			writeD(temp.getMana());
			items++;
			if(items>400) {
				_log.warn("Player "+getClient().getActiveChar()+" has more what 400 items, packet overflow");
				break;
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__27_ITEMLIST;
	}
}