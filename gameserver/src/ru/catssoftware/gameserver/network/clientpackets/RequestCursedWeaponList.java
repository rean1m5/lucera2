package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.network.serverpackets.ExCursedWeaponList;
import javolution.util.FastList;

public class RequestCursedWeaponList extends L2GameClientPacket
{
	private static final String	_C__D0_22_REQUESTCURSEDWEAPONLIST	= "[C] D0:22 RequestCursedWeaponList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		FastList<Integer> list = new FastList<Integer>();
		for (int id : CursedWeaponsManager.getInstance().getCursedWeaponsIds())
		{
			list.add(id);
		}
		activeChar.sendPacket(new ExCursedWeaponList(list));
	}

	@Override
	public String getType()
	{
		return _C__D0_22_REQUESTCURSEDWEAPONLIST;
	}
}
