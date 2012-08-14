package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;

public class level  extends gmHandler
{
	private static final String[] commands =
	{
		"remlevel",
		"addlevel",
		"setlevel"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		
		if (params.length < 2)
		{
			admin.sendMessage("Не задан уровень");
			return;
		}

		final String command = params[0];

		if (command.equals("addlevel") || command.equals("setlevel") || command.equals("remlevel"))
		{
			int reslevel = 0;
			int curlevel = 0;
			long xpcur = 0;
			long xpres = 0;
			int lvl = 0;

			try
			{
				lvl = Integer.parseInt(params[1]);
			}
			catch (Exception e)
			{
				admin.sendMessage("Не задан уровень");
				return;
			}

			L2Object target = admin.getTarget();
			if (target == null)
				target = admin;

			if (target instanceof L2PlayableInstance && lvl > 0)
			{
				L2PlayableInstance player = (L2PlayableInstance) target;

				curlevel = player.getLevel();
				reslevel = command.equals("addlevel") ? (curlevel + lvl) : command.equals("remlevel") ? (curlevel - lvl) : lvl;

				try
				{
					xpcur = player.getStat().getExp();
					xpres = player.getStat().getExpForLevel(reslevel);

					if (xpcur > xpres)
						player.getStat().removeExp(xpcur - xpres);
					else
						player.getStat().addExp(xpres - xpcur);

				}
				catch (Exception e)
				{
					admin.sendMessage("Неверно задан уровень");
					return;
				}
			}
			else
			{
				admin.sendMessage("Не задан уровень");
				return;
			}
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}