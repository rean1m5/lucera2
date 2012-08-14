/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.network.serverpackets;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.item.L2Item;


/**
 * 0x53 WareHouseDepositList  dh (h dddhh dhhh d)
 *
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class WareHouseDepositList extends L2GameServerPacket
{
	private static final String			_S__41_WAREHOUSEDEPOSITLIST	= "[S] 41 WareHouseDepositList";

	public static final int				PRIVATE						= 1;
	public static final int				CLAN						= 2;
	public static final int				CASTLE						= 3;														//not sure
	public static final int				FREIGHT						= 4;														//not sure

	private L2PcInstance				_activeChar;
	private int							_activeCharAdena;
	private List<L2ItemInstance>		_items;
	private int							_whType;

	public WareHouseDepositList(L2PcInstance player, int type)
	{
		//TODO: make it one loop
		_activeChar = player;
		_whType = type;
		_activeCharAdena = _activeChar.getAdena();

		_items = _activeChar.getInventory().getAvailableItems(true);

		// non-tradeable, augmented and shadow items can be stored in private wh
		if (_whType == PRIVATE)
		{
			for (L2ItemInstance temp : player.getInventory().getItems())
			{
				if (temp != null && !temp.isEquipped()
						&& (temp.isShadowItem() || temp.isAugmented() || !temp.isTradeable())
						&& temp.getItem().getType2() != L2Item.TYPE2_QUEST) // exclude quest items
					_items.add(temp);
			}
		}
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		writeC(0x41);
		writeH(_whType);
		writeD(_activeCharAdena);
		int count = _items.size();

		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug("count:" + count);

		writeH(count);

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
			writeH(0x00); // ? 200
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

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__41_WAREHOUSEDEPOSITLIST;
	}
}
