package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.TradeList.TradeItem;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class TradeUpdate extends L2GameServerPacket
{
    private static final String _S__74_TRADEUPDATE = "[S] 74 TradeUpdate";
    private final L2ItemInstance[] _items;
    private final TradeItem[] _trade_items;

    public TradeUpdate(TradeList trade, L2PcInstance activeChar)
    {
        _items = activeChar.getInventory().getItems();
        _trade_items = trade.getItems();
    }

    private int getItemCount( int objectId)
    {
        for (L2ItemInstance item : _items)
            if (item.getObjectId() == objectId)
                return item.getCount();
        return 0;
    }

    @Override
    public String getType()
    {
        return _S__74_TRADEUPDATE;
    }

    @Override
    protected final void writeImpl()
    {
        writeC(0x74);

        writeH(_trade_items.length);
        for (TradeItem item : _trade_items)
        {
            int count = getItemCount(item.getObjectId())
                    - item.getCount();
            boolean stackable = item.getItem().isStackable();
            if (count == 0)
            {
                count = 1;
                stackable = false;
            }
            writeH(stackable ? 3 : 2);
            writeH(item.getItem().getType1()); // item type1
            writeD(item.getObjectId());
            writeD(item.getItem().getItemId());
            writeD(count);
            writeH(item.getItem().getType2()); // item type2
            writeH(0x00); // ?
            writeD(item.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
            writeH(item.getEnchant()); // enchant level
            writeH(0x00); // ?
            writeH(0x00);
        }
    }
}