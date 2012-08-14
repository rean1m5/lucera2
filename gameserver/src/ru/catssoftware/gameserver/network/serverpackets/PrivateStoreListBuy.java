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

import ru.catssoftware.gameserver.model.TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.2.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PrivateStoreListBuy extends L2GameServerPacket
{
	private static final String		_S__BE_PRIVATESTORELISTBUY	= "[S] be PrivateStoreListBuy";
	private int _objId;
	private int _playerAdena;
	private TradeList.TradeItem[] _items;

	public PrivateStoreListBuy(L2PcInstance player, L2PcInstance storePlayer)
	{
		_objId = storePlayer.getObjectId();
		_playerAdena = player.getAdena();
		storePlayer.getSellList().updateItems(); // Update SellList for case inventory content has changed
		_items = storePlayer.getBuyList().getAvailableItems(player.getInventory());
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xB8);
		writeD(_objId);
		writeD(_playerAdena);	

		writeD(_items.length);

		for (TradeList.TradeItem item : _items)
		{
			writeD(item.getObjectId());
			writeD(item.getItem().getItemDisplayId());
			writeH(item.getEnchant());
			writeD(item.getCount());
			writeD(item.getItem().getReferencePrice());
			writeH(0);

			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
			writeD(item.getPrice());//buyers price
			writeD(item.getCount()); // maximum possible tradecount
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__BE_PRIVATESTORELISTBUY;
	}
}
