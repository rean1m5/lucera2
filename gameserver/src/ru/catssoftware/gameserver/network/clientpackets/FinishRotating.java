package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.StopRotation;

public class FinishRotating extends L2GameClientPacket
{
	private static final String	_C__4B_FINISHROTATING	= "[C] 4B FinishRotating";

	private int					_degree;
	@SuppressWarnings("unused")
	private int					_unknown;

	@Override
	protected void readImpl()
	{
		_degree = readD();
		_unknown = readD();
	}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() == null)
			return;
		StopRotation sr = new StopRotation(getClient().getActiveChar().getObjectId(), _degree, 0);
		getClient().getActiveChar().broadcastPacket(sr);
	}

	@Override
	public String getType()
	{
		return _C__4B_FINISHROTATING;
	}
}
