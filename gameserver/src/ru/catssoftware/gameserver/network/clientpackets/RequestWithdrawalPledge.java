package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestWithdrawalPledge extends L2GameClientPacket
{
	private static final String	_C__26_REQUESTWITHDRAWALPLEDGE	= "[C] 26 RequestWithdrawalPledge";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
			return;
		}
		if (activeChar.isClanLeader())
		{
			activeChar.sendPacket(SystemMessageId.CLAN_LEADER_CANNOT_WITHDRAW);
			return;
		}
		if (activeChar.isInCombat())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_LEAVE_DURING_COMBAT);
			return;
		}

		L2Clan clan = activeChar.getClan();

		clan.removeClanMember(activeChar.getObjectId(), System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L); //24*60*60*1000 = 86400000

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN);
		sm.addString(activeChar.getName());
		clan.broadcastToOnlineMembers(sm);
		sm = null;

		// Remove the Player From the Member list
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(activeChar.getName()));

		activeChar.sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_CLAN);
		activeChar.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
	}

	@Override
	public String getType()
	{
		return _C__26_REQUESTWITHDRAWALPLEDGE;
	}
}