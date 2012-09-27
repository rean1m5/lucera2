package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestAnswerJoinAlly extends L2GameClientPacket
{
	private static final String	_C__83_REQUESTANSWERJOINALLY	= "[C] 83 RequestAnswerJoinAlly";

	private int					_response;

	@Override
	protected void readImpl()
	{
		_response = readD();
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
		if (_response == 0)
		{
			activeChar.sendPacket(SystemMessageId.YOU_DID_NOT_RESPOND_TO_ALLY_INVITATION);
			requestor.sendPacket(SystemMessageId.NO_RESPONSE_TO_ALLY_INVITATION);
		}
		else
		{
			L2Clan clan = requestor.getClan();

			if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinAlly))
			{
				return;
			}
			if (clan.checkAllyJoinCondition(requestor, activeChar))
			{
				requestor.sendPacket(SystemMessageId.YOU_INVITED_FOR_ALLIANCE);
				activeChar.sendPacket(SystemMessageId.YOU_ACCEPTED_ALLIANCE);
				activeChar.getClan().setAllyId(clan.getAllyId());
				activeChar.getClan().setAllyName(clan.getAllyName());
				activeChar.getClan().setAllyPenaltyExpiryTime(0, 0);
				activeChar.getClan().setAllyCrestId(clan.getAllyCrestId());
				activeChar.getClan().updateClanInDB();
				activeChar.getClan().setAllyCrestId(requestor.getClan().getAllyCrestId());
				for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
					member.broadcastUserInfo(true);
			}
		}
		activeChar.getRequest().onRequestResponse();
	}

	@Override
	public String getType()
	{
		return _C__83_REQUESTANSWERJOINALLY;
	}
}