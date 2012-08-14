package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.L2FriendList;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.FriendAddRequest;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class RequestFriendInvite extends L2GameClientPacket
{
	private static final String	_C__5E_REQUESTFRIENDINVITE	= "[C] 5E RequestFriendInvite";
	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		SystemMessage sm = null;
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;
		
		if (!FloodProtector.tryPerformAction(activeChar, Protected.CL_PACKET))
		{
			activeChar.sendMessage("Защита от флуда, попробуйте позже!");
			return;
		}

		L2PcInstance friend = L2World.getInstance().getPlayer(_name);

		if (friend == null)
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
		else if (friend == activeChar)
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
		else if (L2FriendList.isInFriendList(activeChar, friend))
		{
			sm = new SystemMessage(SystemMessageId.S1_ALREADY_ON_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
		}
		else if (friend.isInCombat())
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(_name);
			activeChar.sendPacket(sm);
		}
		else if (BlockList.isBlocked(friend, activeChar))
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(_name);
			activeChar.sendPacket(sm);
		}
		else if (!friend.isProcessingRequest())
		{
			activeChar.onTransactionRequest(friend);
			sm = new SystemMessage(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS);
			sm.addString(activeChar.getName());
			friend.sendPacket(sm);

			FriendAddRequest ajf = new FriendAddRequest(activeChar.getName());
			friend.sendPacket(ajf);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(_name);
			activeChar.sendPacket(sm);
		}
	}

	@Override
	public String getType()
	{
		return _C__5E_REQUESTFRIENDINVITE;
	}
}
