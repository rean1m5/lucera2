package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

import javolution.util.FastList;

public final class BuyListSeed extends L2GameServerPacket
{
	private static final String		_S__E9_BUYLISTSEED	= "[S] E9 BuyListSeed [dd h (hdddhhd)]";

	private int						_manorId, _money;
	private List<L2ItemInstance>	_list				= new FastList<L2ItemInstance>();

	public BuyListSeed(L2TradeList list, int manorId, int currentMoney)
	{
		_money = currentMoney;
		_manorId = manorId;
		_list = list.getItems();
	}

	@Override
	public void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xE8);
		writeD(_money); // current money
		writeD(_manorId); // manor id

		writeH(_list.size()); // list length

		for(L2ItemInstance item : _list)
		{
			writeH(0x04); // item->type1
			writeD(0x00); // objectId
			writeD(item.getItemId()); // item id
			writeD(item.getCount()); // item count
			writeH(0x04); // item->type2
			writeH(0x00); // unknown :)
			writeD(item.getPriceToSell()); // price
		}
	}

	@Override
	public String getType()
	{
		return _S__E9_BUYLISTSEED;
	}
}
