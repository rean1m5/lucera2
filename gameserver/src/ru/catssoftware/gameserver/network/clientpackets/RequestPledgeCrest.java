package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.network.serverpackets.PledgeCrest;

public class RequestPledgeCrest extends L2GameClientPacket
{
	private static final String	_C__68_REQUESTPLEDGECREST	= "[C] 68 RequestPledgeCrest";

	private int					_crestId;

	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}

	@Override
	protected void runImpl()
	{
		if (_crestId == 0)
			return;

		byte[] data = CrestCache.getInstance().getPledgeCrest(_crestId);

		if (data != null)
		{
			PledgeCrest pc = new PledgeCrest(_crestId, data);
			sendPacket(pc);
		}
	}

	@Override
	public String getType()
	{
		return _C__68_REQUESTPLEDGECREST;
	}
}