package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;


public class TradeStart extends L2GameServerPacket
{
	private static final String	_S__2E_TRADESTART	= "[S] 1E TradeStart";
	private L2PcInstance			_activeChar;
	private List<L2ItemInstance>	_itemList;

	public TradeStart(L2PcInstance player)
	{
		_activeChar = player;
		_itemList = _activeChar.getInventory().getAvailableItems(true);
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if (_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null)
			return;

		if (_activeChar.getTrading())
		{
			L2PcInstance partner = _activeChar.getActiveTradeList().getPartner();
			_activeChar.clearActiveTradeList(partner);
		}
		else
			_activeChar.setTrading(true);

		writeC(0x1E);
		writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());

		writeH(_itemList.size());
		for (L2ItemInstance item : _itemList)
		{
			writeH(item.getItem().getType1()); // item type1
			writeD(item.getObjectId());
			writeD(item.getItemDisplayId());
			writeD(item.getCount());
			writeH(item.getItem().getType2()); // item type2
			writeH(0x00); // ?

			writeD(item.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(item.getEnchantLevel()); // enchant level
			writeH(0x00);
			writeH(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__2E_TRADESTART;
	}
}