package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class TradeOtherAdd extends L2GameServerPacket
{
	private static final String	_S__31_TRADEOTHERADD	= "[S] 21 TradeOtherAdd";
	private TradeList.TradeItem	_item;
	public TradeOtherAdd(TradeList.TradeItem item)
	{
		_item = item;
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;

		if (activeChar.getActiveTradeList() == null)
			return;

		if (activeChar.getTrading() && activeChar.getActiveTradeList().getPartner().getTrading())
		{
			writeC(0x21);
			writeH(1); // item count
			writeH(_item.getItem().getType1());
			writeD(_item.getObjectId());
			writeD(_item.getItem().getItemDisplayId());
			writeD(_item.getCount());
			writeH(_item.getItem().getType2());
			writeH(0x00);
			writeD(_item.getItem().getBodyPart());
			writeH(_item.getEnchant()); // enchant level
			writeH(0x00);
			writeH(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__31_TRADEOTHERADD;
	}
}