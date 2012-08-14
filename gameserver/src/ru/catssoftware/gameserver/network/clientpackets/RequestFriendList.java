package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2FriendList;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendList extends L2GameClientPacket
{
	private static final String	_C__60_REQUESTFRIENDLIST	= "[C] 60 RequestFriendList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		SystemMessage sm;

		sm = new SystemMessage(SystemMessageId.FRIEND_LIST_HEADER);
		activeChar.sendPacket(sm);

		for (String friendName : L2FriendList.getFriendListNames(activeChar))
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(friendName);

			if (friend == null)
			{
				sm = new SystemMessage(SystemMessageId.S1_OFFLINE);
				sm.addString(friendName);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_ONLINE);
				sm.addString(friendName);
			}

			activeChar.sendPacket(sm);
		}

		sm = new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER);
		activeChar.sendPacket(sm);
		sm = null;
	}

	@Override
	public String getType()
	{
		return _C__60_REQUESTFRIENDLIST;
	}
}
