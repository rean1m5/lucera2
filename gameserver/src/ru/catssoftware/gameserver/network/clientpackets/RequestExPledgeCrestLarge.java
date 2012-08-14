package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.network.serverpackets.ExPledgeCrestLarge;

public class RequestExPledgeCrestLarge extends L2GameClientPacket
{
	private static final String	_C__D0_10_REQUESTEXPLEDGECRESTLARGE	= "[C] D0:10 RequestExPledgeCrestLarge";
	private int					_crestId;

	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}

	@Override
	protected void runImpl()
	{
		byte[] data = CrestCache.getInstance().getPledgeCrestLarge(_crestId);

		if (data != null)
		{
			ExPledgeCrestLarge pcl = new ExPledgeCrestLarge(_crestId, data);
			sendPacket(pcl);
		}

	}

	@Override
	public String getType()
	{
		return _C__D0_10_REQUESTEXPLEDGECRESTLARGE;
	}
}
