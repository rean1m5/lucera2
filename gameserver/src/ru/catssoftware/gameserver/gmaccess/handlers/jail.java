package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.banmanager.BanManager;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author m095
 * @version 2.0
 */

public class jail extends gmHandler
{
	private static final String[] commands =
	{
		"jail",
		"unjail"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];
		int				time	= -1;
		L2PcInstance 	player 	= null;

		if (params.length > 1)
		{
			player = L2World.getInstance().getPlayer(params[1]);
			if (params.length > 2)
			{
				time = Integer.parseInt(params[2]);
			}
		}
		else
		{
			admin.sendMessage("Параметры заданы неверно.");
			return;
		}

		if (command.startsWith("jail"))
		{
			if (player != null)
			{
				if (!BanManager.getInstance().jailPlayer(admin, player, time, false))
					admin.sendMessage("Произошла ошибка, попробуйте снова.");
			}
			else
			{
				if (!BanManager.getInstance().jailPlayer(admin, params[1], time))
					admin.sendMessage("Произошла ошибка, попробуйте снова.");
			}
			return;
		}
		else if (command.startsWith("unjail"))
		{
			if (player != null)
			{
				if (!BanManager.getInstance().unJailPlayer(admin, player))
					admin.sendMessage("Произошла ошибка, попробуйте снова.");
			}
			else
			{
				if (!BanManager.getInstance().unJailPlayer(admin, params[1]))
					admin.sendMessage("Произошла ошибка, попробуйте снова.");
			}
			return;
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}