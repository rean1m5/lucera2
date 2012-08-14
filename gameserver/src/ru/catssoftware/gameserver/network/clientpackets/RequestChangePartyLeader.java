package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestChangePartyLeader extends L2GameClientPacket
{
	private static final String	_C__EE_REQUESTCHANGEPARTYLEADER	= "[C] EE RequestChangePartyLeader";

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
			activeChar.getParty().changePartyLeader(_name);
	}

	@Override
	public String getType()
	{
		return _C__EE_REQUESTCHANGEPARTYLEADER;
	}
}