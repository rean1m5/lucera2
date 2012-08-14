package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PartyMemberPosition;

public class Appearing extends L2GameClientPacket
{
	private static final String	_C__30_APPEARING	= "[C] 30 Appearing";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		activeChar._inWorld = true;
		if (activeChar.isTeleporting())
			activeChar.onTeleported();

		activeChar.broadcastFullInfo();
		if (activeChar.getParty() != null)
			activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
	}

	@Override
	public String getType()
	{
		return _C__30_APPEARING;
	}
}