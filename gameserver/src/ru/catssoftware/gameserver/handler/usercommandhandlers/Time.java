package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class Time implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 77 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (COMMAND_IDS[0] != id)
			return false;

		int time = GameTimeController.getInstance().getGameTime();
		String hour = "" + (time / 60);
		String minute;

		if (time % 60 < 10)
			minute = "0" + time % 60;
		else
			minute = "" + time % 60;

		SystemMessage sm;
		if (GameTimeController.getInstance().isNowNight())
		{
			sm = new SystemMessage(SystemMessageId.CURRENT_TIME_S1_S2_PM);
			sm.addString(hour);
			sm.addString(minute);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.CURRENT_TIME_S1_S2_AM);
			sm.addString(hour);
			sm.addString(minute);
		}
		activeChar.sendPacket(sm);
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}