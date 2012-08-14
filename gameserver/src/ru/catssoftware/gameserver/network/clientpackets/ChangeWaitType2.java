package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;

public class ChangeWaitType2 extends L2GameClientPacket
{
	private static final String	_C__1D_CHANGEWAITTYPE2	= "[C] 1D ChangeWaitType2";

	private boolean				_typeStand;

	/**
	 * packet type id 0x1d
	 *
	 * sample
	 *
	 * 1d
	 * 01 00 00 00 // type (0 = sit, 1 = stand)
	 *
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_typeStand = (readD() == 1);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		L2Object target = player.getTarget();
		if (player != null)
		{
			if (player.isOutOfControl())
			{
				ActionFailed();
				return;
			}

			if (player.getMountType() != 0) //prevent sit/stand if you riding
				return;

			if (target != null &&
					target instanceof L2StaticObjectInstance &&
					!player.isSitting()) {
				if (!((L2StaticObjectInstance) target).useThrone(player))
					player.sendMessage("Нельзя сесть на трон.");

				return;
			}
			if (_typeStand)
			{
				if (player.getObjectSittingOn() != null)
				{
					player.getObjectSittingOn().setBusyStatus(null);
					player.setObjectSittingOn(null);
				}
				player.standUp(false);
			}
			else
				player.sitDown(false);
		}
	}

	@Override
	public String getType()
	{
		return _C__1D_CHANGEWAITTYPE2;
	}
}
