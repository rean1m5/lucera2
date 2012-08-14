package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class Mount implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 61 };

	public synchronized boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;
		return activeChar.mountPlayer(activeChar.getPet());
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}