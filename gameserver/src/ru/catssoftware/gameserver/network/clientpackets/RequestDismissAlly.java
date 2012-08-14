package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestDismissAlly extends L2GameClientPacket
{
	private static final String	_C__86_REQUESTDISMISSALLY	= "[C] 86 RequestDismissAlly";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (!activeChar.isClanLeader())
		{
			activeChar.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}
		activeChar.getClan().dissolveAlly(activeChar);
	}

	@Override
	public String getType()
	{
		return _C__86_REQUESTDISMISSALLY;
	}
}