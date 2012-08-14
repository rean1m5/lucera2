package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class SnoopQuit extends L2GameClientPacket
{
	private static final String	_C__AB_SNOOPQUIT	= "[C] AB SnoopQuit";

	private int					_snoopId;

	@Override
	protected void readImpl()
	{
		_snoopId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		L2PcInstance target = L2World.getInstance().getPlayer(_snoopId);

		if (target == null)
			return;

		player.removeSnooped(target);
		target.removeSnooper(player);
		player.sendMessage("Прослушивание игрока "+target.getName()+" отменено.");
	}

	@Override
	public String getType()
	{
		return _C__AB_SNOOPQUIT;
	}
}