package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class TradeOwnAdd extends L2GameServerPacket
{
	private static final String	_S__30_TRADEOWNADD	= "[S] 20 TradeOwnAdd";
	private TradeList.TradeItem	_item;
	public TradeOwnAdd(TradeList.TradeItem item)
	{
		_item = item;
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;

		writeC(0x20);
		writeH(1); // item count
		writeH(_item.getItem().getType1()); // item type1
		writeD(_item.getObjectId());
		writeD(_item.getItem().getItemDisplayId());
		writeD(_item.getCount());
		writeH(_item.getItem().getType2());
		writeH(0x00);
		writeD(_item.getItem().getBodyPart()); // see Armor or Weapon Table
		writeH(_item.getEnchant()); // enchant level
		writeH(0x00);
		writeH(0x00);
	}

	@Override
	public String getType()
	{
		return _S__30_TRADEOWNADD;
	}
}
