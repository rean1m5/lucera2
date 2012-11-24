package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PartyMemberPosition;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;

public final class MoveBackwardToLocation extends L2GameClientPacket
{
	private static final String _C__01_MOVEBACKWARDTOLOC = "[C] 01 MoveBackwardToLoc";

	private int _targetX, _targetY, _targetZ;

	private Integer _moveMovement;

	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		readD();
		readD();
		readD();
		if (getByteBuffer().remaining() < 4)
			return;

		_moveMovement = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		activeChar._inWorld=true;

		if (_moveMovement == null)
		{
			Util.handleIllegalPlayerAction(activeChar, "Bot usage for movement by " + activeChar, IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		_targetZ += activeChar.getTemplate().getCollisionHeight();

		int curX = activeChar.getX();
		int curY = activeChar.getY();

		if (activeChar.isInBoat())
			activeChar.setInBoat(false);

		if (activeChar.getTeleMode() > 0)
		{
			ActionFailed();
			activeChar.teleToLocation(_targetX, _targetY, _targetZ, false);
			return;
		}

		if (activeChar.isFakeDeath())
			return;

		if (activeChar.isDead() || !getClient().checkKeyProtection())
		{
			ActionFailed();
			return;
		}

		if (_moveMovement == 0)
			ActionFailed();
		else
		{
			double dx = _targetX - curX;
			double dy = _targetY - curY;

			if (activeChar.isOutOfControl() || ((dx * dx + dy * dy) > 98010000))
			{
				ActionFailed();
				return;
			}
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));

			if (activeChar.getParty() != null)
				activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
		}
	}

	@Override
	public String getType()
	{
		return _C__01_MOVEBACKWARDTOLOC;
	}
}