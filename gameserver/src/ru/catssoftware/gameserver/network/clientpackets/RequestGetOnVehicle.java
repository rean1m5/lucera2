package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.BoatManager;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.GetOnVehicle;
import ru.catssoftware.tools.geometry.Point3D;

public class RequestGetOnVehicle extends L2GameClientPacket
{
	private static final String	_C__5C_GETONVEHICLE	= "[C] 5C GetOnVehicle";

	private int					_id, _x, _y, _z;

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
		if (boat == null)
			return;

		GetOnVehicle Gon = new GetOnVehicle(activeChar, boat, _x, _y, _z);
		activeChar.setInBoatPosition(new Point3D(_x, _y, _z));
		activeChar.getPosition().setXYZ(boat.getPosition().getX(), boat.getPosition().getY(), boat.getPosition().getZ());
		activeChar.broadcastPacket(Gon);
		activeChar.revalidateZone(true);
	}

	@Override
	public String getType()
	{
		return _C__5C_GETONVEHICLE;
	}
}
