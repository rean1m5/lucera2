package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class PetItemList extends L2GameServerPacket
{
	private static final String	_S__cb_PETITEMLIST	= "[S] b2  PetItemList";
	private L2PetInstance		_activeChar;

	public PetItemList(L2PetInstance character)
	{
		_activeChar = character;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xB2);

		L2ItemInstance[] items = _activeChar.getInventory().getItems();
		int count = items.length;
		writeH(count);

		for (L2ItemInstance temp : items)
		{
			writeH(temp.getItem().getType1()); // item type1
			writeD(temp.getObjectId());
			writeD(temp.getItemDisplayId());
			writeD(temp.getCount());
			writeH(temp.getItem().getType2()); // item type2
			writeH(0xff); // ?
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(temp.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(temp.getEnchantLevel()); // enchant level
			writeH(0x00); // ?
		}
	}

	@Override
	public String getType()
	{
		return _S__cb_PETITEMLIST;
	}
}