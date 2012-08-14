package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.CursedWeapon;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.network.serverpackets.ExCursedWeaponLocation;
import ru.catssoftware.gameserver.network.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;
import javolution.util.FastList;


public class RequestCursedWeaponLocation extends L2GameClientPacket
{
	private static final String	_C__D0_23_REQUESTCURSEDWEAPONLOCATION	= "[C] D0:23 RequestCursedWeaponLocation";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		FastList<CursedWeaponInfo> list = new FastList<CursedWeaponInfo>();
		for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if (!cw.isActive())
				continue;

			Location loc = cw.getCurrentLocation();

			if (loc != null)
				list.add(new CursedWeaponInfo(loc, cw.getItemId(), cw.isActivated() ? 1 : 0));
		}

		if (!list.isEmpty())
			activeChar.sendPacket(new ExCursedWeaponLocation(list));
	}

	@Override
	public String getType()
	{
		return _C__D0_23_REQUESTCURSEDWEAPONLOCATION;
	}
}
