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
import java.util.Map;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

import javolution.util.FastList;
import javolution.util.FastMap;


public class SellListProcure extends L2GameServerPacket
{
	private static final String				_S__E9_SELLLISTPROCURE	= "[S] E9 SellListProcure";

	private final L2PcInstance				_activeChar;
	private int								_money;
	private Map<L2ItemInstance, Integer>	_sellList				= new FastMap<L2ItemInstance, Integer>();
	private List<CropProcure>				_procureList			= new FastList<CropProcure>();
	private int								_castle;

	public SellListProcure(L2PcInstance player, int castleId)
	{
		_money = player.getAdena();
		_activeChar = player;
		_castle = castleId;
		_procureList = CastleManager.getInstance().getCastleById(_castle).getCropProcure(0);
		for (CropProcure c : _procureList)
		{
			L2ItemInstance item = _activeChar.getInventory().getItemByItemId(c.getId());
			if (item != null && c.getAmount() > 0)
				_sellList.put(item, c.getAmount());
		}
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xE9);
		writeD(_money); // money
		writeD(0x00); // lease ?
		writeH(_sellList.size()); // list size

		for (L2ItemInstance item : _sellList.keySet())
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemDisplayId());
			writeD(_sellList.get(item)); // count
			writeH(item.getItem().getType2());
			writeH(0); // unknown
			writeD(0); // price, u shouldnt get any adena for crops, only raw materials
		}
	}

	@Override
	public String getType()
	{
		return _S__E9_SELLLISTPROCURE;
	}
}