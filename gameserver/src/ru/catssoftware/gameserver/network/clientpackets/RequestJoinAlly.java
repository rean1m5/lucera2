package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AskJoinAlly;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestJoinAlly extends L2GameClientPacket
{
	private static final String	_C__82_REQUESTJOINALLY	= "[C] 82 RequestJoinAlly";

	private int					_objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

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

		L2Object obj = null;

		// Get object from target
		if (activeChar.getTargetId() == _objectId)
			obj = activeChar.getTarget();

		// Get object from world
		if (obj == null)
			obj = L2World.getInstance().getPlayer(_objectId);

		if (!(obj instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		L2PcInstance target = (L2PcInstance) obj;
		L2Clan clan = activeChar.getClan();
		if (!clan.checkAllyJoinCondition(activeChar, target))
			return;

		if (!activeChar.getRequest().setRequest(target, this))
			return;

		SystemMessage sm = new SystemMessage(SystemMessageId.S2_ALLIANCE_LEADER_OF_S1_REQUESTED_ALLIANCE);
		sm.addString(activeChar.getClan().getAllyName());
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
		AskJoinAlly aja = new AskJoinAlly(activeChar.getObjectId(), activeChar.getClan().getAllyName());
		target.sendPacket(aja);

		activeChar.sendPacket(SystemMessageId.YOU_INVITED_FOR_ALLIANCE);
	}

	@Override
	public String getType()
	{
		return _C__82_REQUESTJOINALLY;
	}
}