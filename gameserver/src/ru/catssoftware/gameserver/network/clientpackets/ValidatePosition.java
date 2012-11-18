package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PartyMemberPosition;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocationInVehicle;
import ru.catssoftware.gameserver.util.Broadcast;

public class ValidatePosition extends L2GameClientPacket
{
	private static final String _C__48_VALIDATEPOSITION = "[C] 48 ValidatePosition";

	private int _x, _y, _z, _heading;
	@SuppressWarnings("unused")
	private int _data;


	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_data = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.isTeleporting())
			return;
		activeChar.checkSummon();
		int realX = activeChar.getX();
		int realY = activeChar.getY();
		int realZ = activeChar.getZ();

		if (_x == 0 && _y == 0)
		{
			if (realX != 0)
				return;
		}

		activeChar.setHeading(_heading);
		double dx = _x - realX;
		double dy = _y - realY;
		double dz = _z - realZ;
		double diffSq = (dx * dx + dy * dy);
		double speedsq = activeChar.getStat().getMoveSpeed() * activeChar.getStat().getMoveSpeed();
		if( diffSq <= speedsq * 1.5 && dz < 1500)
		{
			activeChar.setLastServerPosition(realX, realY, realZ);
			activeChar.getPosition().setXYZ(_x, _y, _z);
			if(activeChar.getParty()!=null)
			{
				activeChar.setLastPartyPosition(_x, _y, _z);
				activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
			}
			if (activeChar.isInBoat())
				Broadcast.toKnownPlayers(activeChar, new ValidateLocationInVehicle(activeChar));
			else
				Broadcast.toKnownPlayers(activeChar, new ValidateLocation(activeChar));
		}
	}

	@Override
	public String getType()
	{
		return _C__48_VALIDATEPOSITION;
	}
}
