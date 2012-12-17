package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PartyMemberPosition;

public class CannotMoveAnymore extends L2GameClientPacket
{
	private static final String	_C__36_STOPMOVE	= "[C] 36 CannotMoveAnymore";

	private int					_x;
	private int					_y;
	private int					_z;
	private int					_heading;

	/**
	 * packet type id 0x36
	 *
	 * sample
	 *
	 * 36
	 * a8 4f 02 00 // x
	 * 17 85 01 00 // y
	 * a7 00 00 00 // z
	 * 98 90 00 00 // heading?
	 *
	 * format:		cdddd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (player.getAI() != null)
		{
			player.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_BLOCKED, new Location(_x, _y, _z, _heading));
		}
		if (player.getParty() != null)
		{
			player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
		}
	}

	@Override
	public String getType()
	{
		return _C__36_STOPMOVE;
	}
}