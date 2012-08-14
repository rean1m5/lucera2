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

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;


/**
 * @author  -Wooden-
 */
public class PackageSendableList extends L2GameServerPacket
{
	private static final String	_S__C3_PACKAGESENDABLELIST	= "[S] C3 PackageSendableList";

	private final List<L2ItemInstance> _items;
	private final int _playerObjId;
	private final int _adena;

	public PackageSendableList(L2PcInstance sender, int playerOID)
	{
		_items = sender.getInventory().getAvailableItems(true);
		_playerObjId = playerOID;
		_adena = sender.getAdena();
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xC3);

		writeD(_playerObjId);
		writeD(_adena);
		writeD(_items.size());
		for (L2ItemInstance item : _items) // format inside the for taken from SellList part use should be about the same
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
			writeD(item.getObjectId()); // Will be used in RequestPackageSend response packet
		}
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__C3_PACKAGESENDABLELIST;
	}
}