package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.StopMoveInVehicle;
import ru.catssoftware.tools.geometry.Point3D;

/**
 * @author Maktakien
 */
public class CannotMoveAnymoreInVehicle extends L2GameClientPacket
{
	private int	_x;
	private int	_y;
	private int	_z;
	private int	_heading;
	private int	_boatId;

	@Override
	protected void readImpl()
	{
		_boatId = readD();
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
		{
			return;
		}
		if (player.isInBoat())
		{
			if (player.getBoat().getObjectId() == _boatId)
			{
				player.setInBoatPosition(new Point3D(_x, _y, _z));
				player.getPosition().setHeading(_heading);
				StopMoveInVehicle msg = new StopMoveInVehicle(player, _boatId);
				player.broadcastPacket(msg);
			}
		}
	}

	@Override
	public String getType()
	{
		return "[C] 5D CannotMoveAnymoreInVehicle";
	}

}
