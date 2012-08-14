package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowMemberListAll;

public class RequestPledgeMemberList extends L2GameClientPacket
{
	private static final String	_C__3C_REQUESTPLEDGEMEMBERLIST	= "[C] 3C RequestPledgeMemberList";

	@Override
	protected void readImpl()
	{
		// trigger
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			PledgeShowMemberListAll pm = new PledgeShowMemberListAll(clan, activeChar);
			activeChar.sendPacket(pm);
		}
	}

	@Override
	public String getType()
	{
		return _C__3C_REQUESTPLEDGEMEMBERLIST;
	}
}
