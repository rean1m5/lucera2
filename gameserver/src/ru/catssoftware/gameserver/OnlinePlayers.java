package ru.catssoftware.gameserver;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2World;

public class OnlinePlayers
{
	private static OnlinePlayers	_instance;

	public static OnlinePlayers getInstance()
	{
		if (_instance == null)
			_instance = new OnlinePlayers();
		return _instance;
	}

	class AnnounceOnline implements Runnable
	{
		public void run()
		{
			int currentOnline = (int)(L2World.getInstance().getAllPlayers().size() * Config.ONLINE_PLAYERS_MULTIPLIER);

			if (currentOnline == 1)
			{
				Announcements.getInstance().announceToAll("Сейчас 1 игрок в игре.");
			}
			else
			{
				Announcements.getInstance().announceToAll("Сейчас " + currentOnline+ " игрока(ов) в игре.");
			}
			if (Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL>0)
				ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL);
		}
	}

	private OnlinePlayers()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL);
	}
}