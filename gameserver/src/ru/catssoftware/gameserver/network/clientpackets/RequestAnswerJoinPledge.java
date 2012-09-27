package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.JoinPledge;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowMemberListAll;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestAnswerJoinPledge extends L2GameClientPacket
{
	private static final String	_C__25_REQUESTANSWERJOINPLEDGE	= "[C] 25 RequestAnswerJoinPledge";
	private int					_answer;

	@Override
	protected void readImpl()
	{
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		L2PcInstance requestor = activeChar.getRequest().getPartner();
		if (requestor == null)
			return;
		if (_answer == 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION);
			sm.addString(requestor.getName());
			activeChar.sendPacket(sm);
			sm = null;

			sm = new SystemMessage(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION);
			sm.addString(activeChar.getName());
			requestor.sendPacket(sm);
			sm = null;
		}
		else
		{
			if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinPledge))
				return; // hax

			RequestJoinPledge requestPacket = (RequestJoinPledge) requestor.getRequest().getRequestPacket();
			L2Clan clan = requestor.getClan();
			if (clan.checkClanJoinCondition(requestor, activeChar, requestPacket.getSubPledgeType()))
			{
				JoinPledge jp = new JoinPledge(requestor.getClanId());
				activeChar.sendPacket(jp);

				activeChar.setSubPledgeType(requestPacket.getSubPledgeType());
				if (requestPacket.getSubPledgeType() == L2Clan.SUBUNIT_ACADEMY)
				{
					activeChar.setPledgeRank(9); // adademy
					activeChar.setLvlJoinedAcademy(activeChar.getLevel());
				}
				else
					activeChar.setPledgeRank(5); // new member starts at 5, not confirmed

				clan.addClanMember(activeChar);
				activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPledgeRank()));
				activeChar.sendPacket(SystemMessageId.ENTERED_THE_CLAN);

				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN);
				sm.addString(activeChar.getName());
				clan.broadcastToOnlineMembers(sm);
				sm = null;

				if (activeChar.getClan().getHasFort() > 0)
					FortManager.getInstance().getFortByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
				if (activeChar.getClan().getHasCastle() > 0)
					CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
				activeChar.sendSkillList();

				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
				activeChar.setClanJoinExpiryTime(0);
				activeChar.broadcastUserInfo(true);
			}
		}
		activeChar.getRequest().onRequestResponse();
	}

	@Override
	public String getType()
	{
		return _C__25_REQUESTANSWERJOINPLEDGE;
	}
}