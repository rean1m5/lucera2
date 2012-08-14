package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MerchantInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import javolution.util.FastList;

public class SellList extends L2GameServerPacket
{
	private static final String			_S__10_SELLLIST	= "[S] 10 SellList";
	private final L2PcInstance			_activeChar;
	private final L2MerchantInstance	_lease;
	private int							_money;
	private FastList<L2ItemInstance>	_selllist		= new FastList<L2ItemInstance>();

	public SellList(L2PcInstance player)
	{
		_activeChar = player;
		_lease = null;
		_money = _activeChar.getAdena();
		doLease();
	}

	public SellList(L2PcInstance player, L2MerchantInstance lease)
	{
		_activeChar = player;
		_lease = lease;
		_money = _activeChar.getAdena();
		doLease();
	}

	private void doLease()
	{
		if (_lease == null)
		{
			for (L2ItemInstance item : _activeChar.getInventory().getItems())
			{
				if (!item.isEquipped()						// Not equipped
						&& item.isSellable()				// Item is sellable
						&& (_activeChar.getPet() == null	// Pet not summoned or
								|| item.getObjectId() != _activeChar.getPet().getControlItemId()))	// Pet is summoned and not the item that summoned the pet
				{
					_selllist.add(item);
				}
			}
		}
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x10);
		writeD(_money);
		writeD(0x00);
		writeH(_selllist.size());

		for (L2ItemInstance item : _selllist)
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
			writeH(0x00);
			writeD(item.getItem().getReferencePrice() / 2);
		}
	}

	@Override
	public String getType()
	{
		return _S__10_SELLLIST;
	}
}