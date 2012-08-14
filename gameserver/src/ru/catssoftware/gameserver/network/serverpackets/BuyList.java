package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.item.L2Item;


public final class BuyList extends L2GameServerPacket
{
	private static final String	_S__07_BUYLIST	= "[S] 07 BuyList [ddh (hdddhhdhhhdddddddd)]";
	private int					_listId, _money;
	private L2ItemInstance[]	_list;
	private double				_taxRate		= 1.;

	public BuyList(L2TradeList list, int currentMoney)
	{
		_listId = list.getListId();
		List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	public BuyList(L2TradeList list, int currentMoney, double taxRate)
	{
		_listId = list.getListId();
		List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_taxRate = taxRate;
	}

	public BuyList(List<L2ItemInstance> lst, int listId, int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		L2TradeList lst = TradeListTable.getInstance().getBuyList(_listId);
		if(lst==null)
			return;
		writeC(0x11);
		writeD(_money); // current money
		writeD(_listId);
		writeH(_list.length);
		
		for(L2ItemInstance item : _list)
		{
			if(item.getCount() > 0 || item.getCount() == -1)
			{
				writeH(item.getItem().getType1()); // item type1
				writeD(item.getObjectId());
				writeD(item.getItemId());
				if(item.getCount() < 0)
				{
					writeD(0x00); // max amount of items that a player can buy at a time (with this itemid)
				}
				else
				{
					writeD(item.getCount());
				}
				writeH(item.getItem().getType2()); // item type2
				writeH(0x00); // ?

				if(item.getItem().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					writeD(item.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
					writeH(item.getEnchantLevel()); // enchant level
					writeH(0x00); // ?
					writeH(0x00);
				}
				else
				{
					writeD(0x00); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
					writeH(0x00); // enchant level
					writeH(0x00); // ?
					writeH(0x00);
				}

				if(lst.isGm() && getClient().getActiveChar().isGM())
					writeD(0);
				else {
					if(item.getItemId() >= 3960 && item.getItemId() <= 4026)
					{
						writeD((int) (item.getPriceToSell() * Config.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
					}
					else
					{
						writeD((int) (item.getPriceToSell() * (_taxRate)));
					}
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__07_BUYLIST;
	}
}
