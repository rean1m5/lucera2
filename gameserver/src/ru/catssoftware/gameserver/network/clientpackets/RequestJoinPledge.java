package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AskJoinPledge;

public class RequestJoinPledge extends L2GameClientPacket
{
	private static final String	_C__24_REQUESTJOINPLEDGE	= "[C] 24 RequestJoinPledge";

	private int					_objectId;
	private int					_pledgeType;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_pledgeType = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Clan clan = activeChar.getClan();
		if (clan == null)
		{
			activeChar.sendPacket(SystemMessageId.NOT_JOINED_IN_ANY_CLAN);
			return;
		}

		L2Object obj = null;

		if (activeChar.getTargetId() == _objectId)
			obj = activeChar.getTarget();

		if (obj == null)
			obj = L2World.getInstance().getPlayer(_objectId);

		if (!(obj instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		L2PcInstance target = (L2PcInstance) obj;
		if (!clan.checkClanJoinCondition(activeChar, target, _pledgeType))
			return;

		if (!activeChar.getRequest().setRequest(target, this))
			return;

		AskJoinPledge ap = new AskJoinPledge(activeChar.getObjectId(), activeChar.getClan().getName());
		target.sendPacket(ap);
	}

	public int getSubPledgeType()
	{
		return _pledgeType;
	}

	@Override
	public String getType()
	{
		return _C__24_REQUESTJOINPLEDGE;
	}
}