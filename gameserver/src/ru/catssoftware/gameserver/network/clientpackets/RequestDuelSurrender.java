package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.DuelManager;

public final class RequestDuelSurrender extends L2GameClientPacket
{
	private static final String	_C__D0_30_REQUESTDUELSURRENDER	= "[C] D0:30 RequestDuelSurrender";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		DuelManager.getInstance().doSurrender(getClient().getActiveChar());
	}

	@Override
	public String getType()
	{
		return _C__D0_30_REQUESTDUELSURRENDER;
	}
}
