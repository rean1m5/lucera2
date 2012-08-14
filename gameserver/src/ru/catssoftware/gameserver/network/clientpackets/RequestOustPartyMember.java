package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestOustPartyMember extends L2GameClientPacket
{
	private static final String	_C__2C_REQUESTOUSTPARTYMEMBER	= "[C] 2C RequestOustPartyMember";

	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.isInParty() && activeChar.getParty().isLeader(activeChar))
		{
			if (activeChar.getParty().isInDimensionalRift() && !activeChar.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(activeChar))
				sendPacket(SystemMessageId.COULD_NOT_OUST_FROM_PARTY);
			else
				activeChar.getParty().removePartyMember(_name,true);
		}
	}

	@Override
	public String getType()
	{
		return _C__2C_REQUESTOUSTPARTYMEMBER;
	}
}