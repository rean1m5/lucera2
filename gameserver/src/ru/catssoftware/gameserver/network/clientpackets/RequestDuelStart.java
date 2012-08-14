package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExDuelAskStart;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public final class RequestDuelStart extends L2GameClientPacket
{
	private static final String	_C__D0_27_REQUESTDUELSTART	= "[C] D0:27 RequestDuelStart";
	private String				_player;
	private int					_partyDuel;

	@Override
	protected void readImpl()
	{
		_player = readS();
		_partyDuel = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2PcInstance targetChar = L2World.getInstance().getPlayer(_player);
		if (targetChar == null)
		{
			activeChar.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}

		if (activeChar == targetChar)
		{
			activeChar.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}

		if (!activeChar.canDuel())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
			return;
		}
		else if (!targetChar.canDuel())
		{
			activeChar.sendPacket(targetChar.getNoDuelReason());
			return;
		}
		else if (!activeChar.isInsideRadius(targetChar, 250, false, false))
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_S1_IS_TOO_FAR_AWAY);
			msg.addString(targetChar.getName());
			activeChar.sendPacket(msg);
			return;
		}
		if (_partyDuel == 1)
		{
			if (!activeChar.isInParty() || !(activeChar.isInParty() && activeChar.getParty().isLeader(activeChar)))
			{
				activeChar.sendMessage("Вы должны быть лидером группы.");
				return;
			}
			else if (!targetChar.isInParty())
			{
				activeChar.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}
			else if (activeChar.getParty().getPartyMembers().contains(targetChar))
			{
				activeChar.sendMessage("Этот игрок уже в вашей группе.");
				return;
			}
			for (L2PcInstance temp : activeChar.getParty().getPartyMembers())
			{
				if (!temp.canDuel())
				{
					activeChar.sendMessage("Никто не готов к дуэли.");
					return;
				}
			}
			L2PcInstance partyLeader = null;

			for (L2PcInstance temp : targetChar.getParty().getPartyMembers())
			{
				if (partyLeader == null)
					partyLeader = temp;
				if (!temp.canDuel())
				{
					activeChar.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					return;
				}
			}
			if (partyLeader != null)
			{
				if (!partyLeader.isProcessingRequest())
				{
					activeChar.onTransactionRequest(partyLeader);
					partyLeader.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));

					SystemMessage msg = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
					msg.addString(partyLeader.getName());
					activeChar.sendPacket(msg);

					msg = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
					msg.addString(activeChar.getName());
					targetChar.sendPacket(msg);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
					msg.addString(partyLeader.getName());
					activeChar.sendPacket(msg);
				}
			}
		}
		else
		// 1vs1 duel
		{
			if (!targetChar.isProcessingRequest())
			{
				activeChar.onTransactionRequest(targetChar);
				targetChar.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));

				SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
				msg.addString(targetChar.getName());
				activeChar.sendPacket(msg);

				msg = new SystemMessage(SystemMessageId.S1_HAS_CHALLENGED_YOU_TO_A_DUEL);
				msg.addString(activeChar.getName());
				targetChar.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
				msg.addString(targetChar.getName());
				activeChar.sendPacket(msg);
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_27_REQUESTDUELSTART;
	}
}
