package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class DisMount implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 62 };

	public synchronized boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		if (activeChar.isRentedPet())
			activeChar.stopRentPet();
		else if (activeChar.isMounted())
			activeChar.dismount();

		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}