package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class WareHouseWithdrawalList extends L2GameServerPacket
{
	private static final String	_S__54_WAREHOUSEWITHDRAWALLIST	= "[S] 42 WareHouseWithdrawalList";

	public static final int		PRIVATE							= 1;
	public static final int		CLAN							= 2;
	public static final int		CASTLE							= 3;															//not sure
	public static final int		FREIGHT							= 4;															//not sure
	private L2PcInstance		_activeChar;
	private int					_activeCharAdena;
	private L2ItemInstance[]	_items;
	private int					_whType;

	public WareHouseWithdrawalList(L2PcInstance player, int type)
	{
		_activeChar = player;
		_whType = type;

		_activeCharAdena = _activeChar.getAdena();
		if (_activeChar.getActiveWarehouse() == null)
		{
			// Something went wrong!
			_log.warn("error while sending withdraw request to: " + _activeChar.getName());
			return;
		}
		_items = _activeChar.getActiveWarehouse().getItems();

		if (_log.isDebugEnabled() || Config.DEBUG)
		{
			for (L2ItemInstance item : _items)
				_log.info("item:" + item.getItem().getName() + " type1:" + item.getItem().getType1() + " type2:" + item.getItem().getType2());
		}
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		writeC(0x42);
		writeH(_whType);
		writeD(_activeCharAdena);
		writeH(_items.length);

		for (L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemDisplayId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			writeH(0x00); // ?
			writeD(item.getObjectId());
			if (item.isAugmented())
			{
				writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
				writeD(item.getAugmentation().getAugmentationId() >> 16);
			}
			else
				writeQ(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__54_WAREHOUSEWITHDRAWALLIST;
	}
}
