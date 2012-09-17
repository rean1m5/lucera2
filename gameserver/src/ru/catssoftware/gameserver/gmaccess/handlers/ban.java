package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.banmanager.BanManager;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author m095
 * @version 3.0
 */

public class ban extends gmHandler
{
	private static final String[] commands =
	{
		"ban",
		"unban",
		"banchar",
		"unbanchar",
		"gmacc",
		"banhwid"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		if (params.length < 2)
		{
			admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_SET_ALL_ARG));
			return;
		}

		String 			command 	= params[0];
		L2PcInstance 	player 		= L2World.getInstance().getPlayer(params[1]);

		if (command.equals("ban"))
		{
			if (player != null)
			{
				if (!BanManager.getInstance().banAccount(admin, player))
					admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			}
			else
			{
				if (!BanManager.getInstance().banAccount(admin, params[1]))
					admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			}
			return;
		}
		else if (command.equals("unban"))
		{
			if (!BanManager.getInstance().unBanAccount(admin, params[1]))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			return;
		}
		else if (command.equals("banchar"))
		{
			if (!BanManager.getInstance().banChar(admin, params[1]))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			return;
		}
		else if (command.equals("unbanchar"))
		{
			if (!BanManager.getInstance().unBanChar(admin, params[1]))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			return;
		}
		else if (command.equals("gmacc"))
		{
			if (!BanManager.getInstance().gmAccess(admin, params[1]))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
			return;
		}
		else if (command.equals("banhwid"))
		{
			if (!BanManager.getInstance().banHWID(admin, player, "ban hwid by " + admin.getName()))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}