package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.BoatManager;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.tools.geometry.Point3D;

public class RequestMoveToLocationInVehicle extends L2GameClientPacket
{
	private final Point3D	_pos		= new Point3D(0, 0, 0);
	private final Point3D	_origin_pos	= new Point3D(0, 0, 0);
	private int				_boatId;

	@Override
	protected void readImpl()
	{
		int _x, _y, _z;
		_boatId = readD(); //objectId of boat
		_x = readD();
		_y = readD();
		_z = readD();
		_pos.setXYZ(_x, _y, _z);
		_x = readD();
		_y = readD();
		_z = readD();
		_origin_pos.setXYZ(_x, _y, _z);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		else if (activeChar.isAttackingNow() && activeChar.getActiveWeaponItem() != null
				&& (activeChar.getActiveWeaponItem().getItemType() == L2WeaponType.BOW))
		{
			ActionFailed();
		}
		else
		{
			L2BoatInstance boat = BoatManager.getInstance().getBoat(_boatId);
			if (boat == null)
				return;
			activeChar.setBoat(boat);
			activeChar.setInBoat(true);
			activeChar.setInBoatPosition(_pos);
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO_IN_A_BOAT, new L2CharPosition(_pos.getX(), _pos.getY(), _pos.getZ(), 0),
					new L2CharPosition(_origin_pos.getX(), _origin_pos.getY(), _origin_pos.getZ(), 0));
		}

	}

	@Override
	public String getType()
	{
		return null;
	}
}
