package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2FriendList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.FriendList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestAnswerFriendInvite extends L2GameClientPacket
{
	private static final String	_C__5F_REQUESTANSWERFRIENDINVITE	= "[C] 5F RequestAnswerFriendInvite";

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

		L2PcInstance requestor = activeChar.getActiveRequester();

		if (requestor == null)
		{
			activeChar.sendPacket(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
			return;
		}
		if (_response == 1)
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);

			L2FriendList.addToFriendList(requestor, activeChar);

			//Player added to requester friends list.
			SystemMessage rsm = new SystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS);
			rsm.addString(activeChar.getName());
			requestor.sendPacket(rsm);

			//Requester has joined as friend.
			SystemMessage asm = new SystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND);
			asm.addString(requestor.getName());
			activeChar.sendPacket(asm);
		}
		else
			requestor.sendPacket(SystemMessageId.FAILED_TO_INVITE_A_FRIEND);

		activeChar.setActiveRequester(null);
		requestor.onTransactionResponse();

		// Send notifications for both player in order to show them online
		activeChar.sendPacket(new FriendList(activeChar));
		requestor.sendPacket(new FriendList(requestor));
	}

	@Override
	public String getType()
	{
		return _C__5F_REQUESTANSWERFRIENDINVITE;
	}
}