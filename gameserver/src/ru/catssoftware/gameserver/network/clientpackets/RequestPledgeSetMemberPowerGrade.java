package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestPledgeSetMemberPowerGrade extends L2GameClientPacket
{
	private static final String	_C__D0_1C_REQUESTPLEDGESETMEMBERPOWERGRADE	= "[C] D0:1C RequestPledgeSetMemberPowerGrade";
	private int					_pledgeRank;
	private String				_member;

	@Override
	protected void readImpl()
	{
		_member = readS();
		_pledgeRank = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		L2Clan clan = activeChar.getClan();
		if (clan == null)
			return;
		L2ClanMember member = clan.getClanMember(_member);
		if (member == null)
			return;
		if (member.getSubPledgeType() == L2Clan.SUBUNIT_ACADEMY)
		{
			// also checked from client side
			activeChar.sendMessage("Неприменимо для академии");
			return;
		}
		member.setPledgeRank(_pledgeRank);
		clan.broadcastClanStatus();
	}

	@Override
	public String getType()
	{
		return _C__D0_1C_REQUESTPLEDGESETMEMBERPOWERGRADE;
	}
}
