package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;

public final class Logout extends L2GameClientPacket
{
	private static final String _C__09_LOGOUT = "[C] 09 Logout";

	@Override
	protected void readImpl()
	{
		// Empty
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		if (!activeChar.canLogout())
		{
			ActionFailed();
			return;
		}
		new Disconnection(getClient(), activeChar).defaultSequence(false);
	}

	@Override
	public String getType()
	{
		return _C__09_LOGOUT;
	}
}