package ru.catssoftware.gameserver.network.serverpackets;

import java.util.Map;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.entity.Castle;


public class ExShowCastleInfo extends L2GameServerPacket
{
	private static final String	S_FE_14_EX_SHOW_CASTLE_INFO	= "[S] FE:14 ExShowFortressInfo";

	@Override
	public String getType()
	{
		return S_FE_14_EX_SHOW_CASTLE_INFO;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x14);
		Map<Integer, Castle> castles = CastleManager.getInstance().getCastles();
		writeD(castles.size());
		for (Castle castle : castles.values())
		{
			writeD(castle.getCastleId());
			if (castle.getOwnerId() > 0)
			{
				L2Clan owner = ClanTable.getInstance().getClan(castle.getOwnerId());
				if (owner != null)
					writeS(owner.getName());
				else
				{
					_log.warn("Castle owner with no name! Castle: " + castle.getName() + " has an OwnerId = " + castle.getOwnerId()
							+ " who does not have a  name!");
					writeS("");
				}
			}
			else
				writeS("");
			writeD(castle.getTaxPercent());
			writeD((int) (castle.getSiege().getSiegeDate().getTimeInMillis() / 1000));
		}
	}
}
