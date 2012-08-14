package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.JoinParty;

public class RequestAnswerJoinParty extends L2GameClientPacket
{
	private static final String	_C__2A_REQUESTANSWERPARTY	= "[C] 2A RequestAnswerJoinParty";
	private int					_response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null || player.getParty()!=null)
			return;

		L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null)
			return;

		if (player != null)
		{
			JoinParty join = new JoinParty(_response);
			requestor.sendPacket(join);

			if (_response == 1)
			{
				if (requestor.getParty() != null)
				{
					if (requestor.getParty().getMemberCount() >= 9)
					{
						player.sendPacket(SystemMessageId.PARTY_FULL);
						requestor.sendPacket(SystemMessageId.PARTY_FULL);
						return;
					}
				}
				player.joinParty(requestor.getParty());
			}
			else
			{
				requestor.sendPacket(SystemMessageId.PLAYER_DECLINED);

				L2Party party = requestor.getParty();
				if (party != null && party.getMemberCount() == 1)
				{
					L2PartyRoom room = party.getPartyRoom();
					if (room != null)
						room.setParty(null);
					party.setPartyRoom(null);
					requestor.setParty(null);					
				}

			}
			if (requestor.getParty() != null)
				requestor.getParty().decreasePendingInvitationNumber();

			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}

	@Override
	public String getType()
	{
		return _C__2A_REQUESTANSWERPARTY;
	}
}
