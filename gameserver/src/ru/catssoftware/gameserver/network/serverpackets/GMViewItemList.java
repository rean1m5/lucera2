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

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class GMViewItemList extends L2GameServerPacket
{
	//private static Logger _log = Logger.getLogger(GMViewItemList.class.getName());
	private static final String	_S__AD_GMVIEWITEMLIST	= "[S] 94 GMViewItemList";
	private L2ItemInstance[]	_items;
	private L2PcInstance		_cha;
	private String				_playerName;

	public GMViewItemList(L2PcInstance cha)
	{
		_items = cha.getInventory().getItems();
		_playerName = cha.getName();
		_cha = cha;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x94);
		writeS(_playerName);
		writeD(_cha.getInventoryLimit()); // inventory limit
		writeH(0x01); // show window ??
		writeH(_items.length);

		for (L2ItemInstance temp : _items)
		{
			if (temp == null || temp.getItem() == null)
				continue;

			writeH(temp.getItem().getType1());

			writeD(temp.getObjectId());
			writeD(temp.getItemDisplayId());
			writeD(temp.getCount());
			writeH(temp.getItem().getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(temp.getItem().getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			if (temp.isAugmented())
			{
				writeD(temp.getAugmentation().getAugmentationId());
			}
			else
			{
				writeD(0x00);
			}
			writeD(temp.getMana());
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__AD_GMVIEWITEMLIST;
	}
}
