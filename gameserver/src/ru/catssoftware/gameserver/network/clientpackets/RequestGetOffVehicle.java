package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.BoatManager;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.GetOffVehicle;

public class RequestGetOffVehicle extends L2GameClientPacket
{
	private int	_id, _x, _y, _z;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		L2BoatInstance boat = BoatManager.getInstance().getBoat(_id);
		GetOffVehicle Gon = new GetOffVehicle(activeChar, boat, _x, _y, _z);
		activeChar.broadcastPacket(Gon);
	}

	@Override
	public String getType()
	{
		return "[S] 5d GetOffVehicle";
	}
}
