package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;

public final class RequestRecordInfo extends L2GameClientPacket
{
	private static final String _0__CF_REQUEST_RECORD_INFO = "[0] CF RequestRecordInfo";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getActiveChar();

		if (activeChar == null)
			return;

		activeChar.getKnownList().updateKnownObjects();
		activeChar.sendPacket(new UserInfo(activeChar));
	}

	@Override
	public String getType()
	{
		return _0__CF_REQUEST_RECORD_INFO;
	}
}