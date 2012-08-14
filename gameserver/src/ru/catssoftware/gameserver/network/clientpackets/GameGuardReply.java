package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.protection.CatsGuard;

public class GameGuardReply extends L2GameClientPacket
{
	private static final String	_C__CA_GAMEGUARDREPLY	= "[C] CA GameGuardReply";


	private int[]				_reply					= new int[4];

	@Override
	protected void readImpl()
	{
		if(CatsGuard.getInstance().isEnabled() && getClient().getHWid()==null)
		{
			_reply[0] = readD();
			_reply[1] = readD();
			_reply[2] = readD();
			_reply[3] = readD();
		}
		else
		{
			byte [] b = new byte[getByteBuffer().remaining()];
			readB(b);
		}
	}

	@Override
	protected void runImpl()
	{
	}

	@Override
	public String getType()
	{
		return _C__CA_GAMEGUARDREPLY;
	}
}