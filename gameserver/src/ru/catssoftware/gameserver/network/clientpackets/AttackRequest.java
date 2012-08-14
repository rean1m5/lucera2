package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class AttackRequest extends L2GameClientPacket
{
	// cddddc
	private int					_objectId;
	@SuppressWarnings("unused")
	private int					_originX;
	@SuppressWarnings("unused")
	private int					_originY;
	@SuppressWarnings("unused")
	private int					_originZ;
	@SuppressWarnings("unused")
	private int					_attackId;

	private static final String	_C__0A_ATTACKREQUEST	= "[C] 0A AttackRequest";

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC(); // 0 for simple click   1 for shift-click
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Object target = null;

		// Get object from target
		if (activeChar.getTargetId() == _objectId)
			target = activeChar.getTarget();

		// Get object from world
		if (target == null)
			target = L2World.getInstance().findObject(_objectId);

		if (target == null)
			return;

		if (activeChar.getTarget() != target)
		{
			target.onAction(activeChar);
		}
		else
		{
			if ((target.getObjectId() != activeChar.getObjectId()) && activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null)
			{
				target.onForcedAttack(activeChar);
			}
			else
			{
				ActionFailed();
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__0A_ATTACKREQUEST;
	}
}