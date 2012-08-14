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

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.L2GameClient;
import javolution.util.FastMap;

/**
 * format(packet 0xFE)
 * ch dd [dddc]
 * c  - id
 * h  - sub id
 *
 * d  - crop id
 * d  - size
 *
 * [
 * d  - manor name
 * d  - buy residual
 * d  - buy price
 * c  - reward type
 * ]
 *
 * @author l3x
 */
public class ExShowProcureCropDetail extends L2GameServerPacket
{
	private static final String				_S__FE_22_EXSHOWPROCURECROPDETAIL	= "[S] FE:22 ExShowProcureCropDetail";

	private int								_cropId;
	private FastMap<Integer, CropProcure>	_castleCrops;

	public ExShowProcureCropDetail(int cropId)
	{
		_cropId = cropId;
		_castleCrops = new FastMap<Integer, CropProcure>();

		for (Castle c : CastleManager.getInstance().getCastles().values())
		{
			CropProcure cropItem = c.getCrop(_cropId, CastleManorManager.PERIOD_CURRENT);
			if (cropItem != null && cropItem.getAmount() > 0)
				_castleCrops.put(c.getCastleId(), cropItem);
		}
	}

	@Override
	public void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xFE);
		writeH(0x22);

		writeD(_cropId); // crop id
		writeD(_castleCrops.size()); // size

		for (int manorId : _castleCrops.keySet())
		{
			CropProcure crop = _castleCrops.get(manorId);
			writeD(manorId); // manor name
			writeD(crop.getAmount()); // buy residual
			writeD(crop.getPrice()); // buy price
			writeC(crop.getReward()); // reward type
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_22_EXSHOWPROCURECROPDETAIL;
	}
}