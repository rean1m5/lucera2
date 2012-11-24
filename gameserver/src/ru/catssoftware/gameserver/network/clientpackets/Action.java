package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public final class Action extends L2GameClientPacket
{
	private static final String	ACTION__C__04	= "[C] 04 Action";

	// cddddc
	private int					_objectId;
	@SuppressWarnings("unused")
	private int					_originX;
	@SuppressWarnings("unused")
	private int					_originY;
	@SuppressWarnings("unused")
	private int					_originZ;
	private int					_actionId;

	@Override
	protected void readImpl()
	{
		_objectId = readD(); // Target object Identifier
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_actionId = readC(); // Action identifier : 0-Simple click, 1-Shift click
	}

	@Override
	protected void runImpl()
	{
		// Get the current L2PcInstance of the player
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		activeChar._bbsMultisell = 0;
		if (activeChar.isDead() || !getClient().checkKeyProtection()) {
			ActionFailed();
			return;
		}
		if (activeChar.inObserverMode())
		{
			activeChar.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			ActionFailed();
			return;
		}
		L2Object obj = null;

		// Get object from target
		if (activeChar.getTargetId() == _objectId)
			obj = activeChar.getTarget();
		else if(_objectId==activeChar.getObjectId())
			obj = activeChar;
		// Get object from world
		if (obj == null) {
			obj = activeChar.getKnownList().getKnownObject(_objectId);
			if(obj == null && activeChar.getParty()!=null)
				obj = activeChar.getParty().getMemberById(_objectId);
		}

		// If object requested does not exist, add warn msg into logs
		
		if (obj == null)
		{
			ActionFailed();
			return;
		}
		// Check if the target is valid, if the player haven't a shop or isn't the requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...)
		if (activeChar.getActiveRequester() == null)
		{
			switch (_actionId)
			{
			case 0:
				obj.onAction(activeChar);
				break;
			case 1:
				if (obj.isCharacter() && obj.getCharacter().isAlikeDead() && !activeChar.isGM())
					obj.onAction(activeChar);
				else
					obj.onActionShift(activeChar);
				break;
			default:
				ActionFailed();
				break;
			}
		}
		else
			ActionFailed(); 
	}

	@Override
	public String getType()
	{
		return ACTION__C__04;
	}
}