package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.AutoSpawnManager;
import ru.catssoftware.gameserver.instancemanager.AutoSpawnManager.AutoSpawnInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class sevensigns extends gmHandler
{
	private AutoSpawnManager		autospawn			= AutoSpawnManager.getInstance();
	private AutoSpawnInstance 		blackSpawnInst		= null;
	private AutoSpawnInstance 		merchSpawnInst		= null;
	private int						teleportIndex		= -1;
	private static final String[] 	commands 			=
	{
		"ss_period_change",
		"mammon_find"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];

		if (command.equals("ss_period_change"))
		{
			SevenSigns.getInstance().changePeriodManualy();
			return;
		}
		else if (command.equals("mammon_find"))
		{
			
			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				admin.sendMessage("В текущий период мамоны не активны");
				return;
			}

			blackSpawnInst = autospawn.getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
			merchSpawnInst = autospawn.getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);
			
			try
			{
				teleportIndex = Integer.parseInt(params[1]);
			}
			catch (Exception NumberFormatException)
			{
			}

			if (blackSpawnInst != null)
			{
				L2NpcInstance[] blackInst = blackSpawnInst.getNPCInstanceList();
				if (blackInst.length > 0)
				{
					int x1 = blackInst[0].getX(), y1 = blackInst[0].getY(), z1 = blackInst[0].getZ();
					admin.sendMessage("Blacksmith of Mammon: " + x1 + " " + y1 + " " + z1);
					if (teleportIndex == 1)
						admin.teleToLocation(x1, y1, z1, true);
				}
				else
					admin.sendMessage("Blacksmith of Mammon: не найден");
			}
			else
				admin.sendMessage("Blacksmith of Mammon: не найден");

			if (merchSpawnInst != null)
			{
				L2NpcInstance[] merchInst = merchSpawnInst.getNPCInstanceList();
				if (merchInst.length > 0)
				{
					int x2 = merchInst[0].getX(), y2 = merchInst[0].getY(), z2 = merchInst[0].getZ();
					admin.sendMessage("Merchant of Mammon: " + x2 + " " + y2 + " " + z2);
					if (teleportIndex == 2)
						admin.teleToLocation(x2, y2, z2, true);
				}
				else
					admin.sendMessage("Merchant of Mammon: не найден");
			}
			else
				admin.sendMessage("Merchant of Mammon: не найден");
		}
		else
			admin.sendMessage("Cmd: '"+command+"'");
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}