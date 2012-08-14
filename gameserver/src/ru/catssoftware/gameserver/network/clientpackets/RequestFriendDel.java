package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2FriendList;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendDel extends L2GameClientPacket
{

	private static final String	_C__61_REQUESTFRIENDDEL	= "[C] 61 RequestFriendDel";
	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		L2PcInstance friend = L2World.getInstance().getPlayer(_name);

		if (friend == activeChar)
		{
			return;
		}
		else if (!L2FriendList.isInFriendList(activeChar, _name))
		{
			// Target is not in friend list.
			sm = new SystemMessage(SystemMessageId.S1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			sm = null;
		}
		else if (friend != null)
		{
			L2FriendList.removeFromFriendList(activeChar, friend);
			// Notify that target deleted from friends list.
			sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			// Notify target that requester deleted from friends list.
			sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(activeChar.getName());
			friend.sendPacket(sm);
		}
		else
		{
			L2FriendList.removeFromFriendList(activeChar, _name);
			sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
		}
	}

	@Override
	public String getType()
	{
		return _C__61_REQUESTFRIENDDEL;
	}
}
