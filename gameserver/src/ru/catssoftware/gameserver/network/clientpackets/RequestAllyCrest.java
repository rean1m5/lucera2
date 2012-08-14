package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.network.serverpackets.AllyCrest;


public class RequestAllyCrest extends L2GameClientPacket
{
	private static final String	_C__88_REQUESTALLYCREST	= "[C] 88 RequestAllyCrest";
	private int					_crestId;

	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}

	@Override
	protected void runImpl()
	{
		byte[] data = CrestCache.getInstance().getAllyCrest(_crestId);

		if (data != null)
		{
			AllyCrest ac = new AllyCrest(_crestId, data);
			sendPacket(ac);
		}
	}

	@Override
	public String getType()
	{
		return _C__88_REQUESTALLYCREST;
	}
}