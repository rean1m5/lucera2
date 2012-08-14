package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestExAcceptJoinMPCC extends L2GameClientPacket
{
	private static final String	_C__D0_0E_REQUESTEXASKJOINMPCC	= "[C] D0:0E RequestExAcceptJoinMPCC";
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
		SystemMessage sm;
		if (player != null)
		{
			L2PcInstance requestor = player.getActiveRequester();
			if (requestor == null)
				return;
			if (_response == 1)
			{
				boolean newCc = false;
				if (!requestor.getParty().isInCommandChannel())
				{
					new L2CommandChannel(requestor); // Create new CC
					newCc = true;
				}
				requestor.getParty().getCommandChannel().addParty(player.getParty());
				if (!newCc)
				{
					sm = new SystemMessage(SystemMessageId.JOINED_COMMAND_CHANNEL);
					player.getParty().broadcastToPartyMembers(sm);
				}
			}
			else
				requestor.sendMessage("Игрок отказался принять приглашение.");
			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_0E_REQUESTEXASKJOINMPCC;
	}
}