package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.L2GameServer;
import ru.catssoftware.gameserver.LoginServerThread;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.gameserverpackets.ServerStatus;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

public class server extends gmHandler
{
	private static final String[] commands =
	{
			"restart",
			"shutdown",
			"abort",
			"server",
			"cleanup",
			"kickall",
			"onlygm",
			"forall",
			"maxplayer"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];
		if (command.equals("server"))
		{
			showPage(admin);
			return;
		}
		else if (command.equals("maxplayer"))
		{
			if (params.length < 2)
			{
				admin.sendMessage("Задайте количество игроков");
				showPage(admin);
				return;
			}
			
			try
			{
				LoginServerThread.getInstance().setMaxPlayer(Integer.parseInt(params[1]));
				Config.MAXIMUM_ONLINE_USERS = Integer.parseInt(params[1]);
				admin.sendMessage("Максимальный онлайн, установлено " + Config.MAXIMUM_ONLINE_USERS);
			}
			catch (Exception e)
			{
				admin.sendMessage("Задайте количество игроков");
			}
			showPage(admin);
			return;
		}
		else if (command.equals("kickall"))
		{
			int count = 0;
			for (L2PcInstance pl : L2World.getInstance().getAllPlayers())
			{
				if (pl != null && !pl.isGM())
				{
					if (pl.isOfflineTrade())
					{
						pl.setOfflineTrade(false);
						pl.standUp();
					}
					new Disconnection(pl).defaultSequence(false);
					count++;
				}
			}
			admin.sendMessage("Удалено из игры " + count + " игроков");
			showPage(admin);
			return;
		}
		else if (command.equals("onlygm"))
		{
			LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_GM_ONLY);
			Config.SERVER_GMONLY = true;
			admin.sendMessage("Вход на сервер только для Gm. Включено");
			showPage(admin);
			return;
		}
		else if (command.equals("forall"))
		{
			LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
			admin.sendMessage("Вход на сервер только для Gm. Выключено");
			Config.SERVER_GMONLY = false;
			showPage(admin);
			return;
		}
		else if (command.equals("restart"))
		{
			if (params.length < 2)
			{
				admin.sendMessage("Задайте время до рестарта (в мс)");
				showPage(admin);
				return;
			}

			try
			{
				serverShutdown(Integer.parseInt(params[1]), true);
			}
			catch (Exception e)
			{
				admin.sendMessage("Задайте время до рестарта (в секундах)");
			}
			showPage(admin);
			return;
		}
		else if (command.equals("shutdown"))
		{
			if (params.length < 2)
			{
				admin.sendMessage("Задайте время до выключения (в секундах)");
				showPage(admin);
				return;
			}

			try
			{
				serverShutdown(Integer.parseInt(params[1]), false);
			}
			catch (Exception e)
			{
				admin.sendMessage("Задайте время до выключения (в секундах)");
			}
			showPage(admin);
			return;
		}
		else if (command.equals("abort"))
		{
			Shutdown.getInstance().abort();
			showPage(admin);
			return;
		}
		else if (command.equals("cleanup"))
		{
			System.gc();
			System.runFinalization();
			showPage(admin);
			return;
		}
	}
	
	private void serverShutdown(int seconds, boolean restart)
	{
		Shutdown.getInstance().startShutdown("GM", seconds, restart ? Shutdown.ShutdownModeType.RESTART : Shutdown.ShutdownModeType.SHUTDOWN);
	}

	private void showPage(L2PcInstance player)
	{
		/* mem */
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		String mem = ("Free: " + freeMem + " Mb. Total: " + totalMem + " Mb.");
		/* time */
		long sUptime, sHour, sMinutes, sSeconds = 0;
		String sTime = "";
		sUptime = ((System.currentTimeMillis() - L2GameServer._upTime) / 1000);
		sHour = sUptime / 3600;
		sMinutes = (sUptime - (sHour * 3600)) / 60;
		sSeconds = ((sUptime - (sHour * 3600)) - (sMinutes * 60));
		sTime = (sHour + " ч " + sMinutes + " мин " + sSeconds + " сек.");

		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile("data/html/admin/menus/server.htm");
		html.replace("%meminfo%", mem);
		html.replace("%os%", System.getProperty("os.name"));
		html.replace("%time%", sTime);
		html.replace("%online%", L2World.getInstance().getAllPlayersCount());
		html.replace("%max%", Config.MAXIMUM_ONLINE_USERS);
		html.replace("%geo%", (Config.GEODATA ? "Загружена" : "Выключена"));
		player.sendPacket(html);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}