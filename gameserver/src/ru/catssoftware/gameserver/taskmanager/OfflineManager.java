package ru.catssoftware.gameserver.taskmanager;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;

public class OfflineManager
{
	private static final Logger				_log					= Logger.getLogger(OfflineManager.class);
	private static OfflineManager			_instance				= null;

	public static OfflineManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Offline Manager: initialize...");
			_instance = new OfflineManager();
		}
		return _instance;
	}

	private class Checker implements Runnable
	{
		@Override
		public void run()
		{
			int count = 0;
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (player == null)
					continue;
				if (!player.isOfflineTrade())
					continue;
				
				if (player.getEndOfflineTime() < System.currentTimeMillis())
				{
					player.setOfflineTrade(false);
					player.standUp();
					new Disconnection(player).defaultSequence(false);
					count++;
				}
			}
			if (count > 0)
				_log.info("Offline Manager: " + count + " player(s) deleted, offline time expired.");
		}
	}

	private OfflineManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Checker(), 1800000, 1800000);
	}
	
	public void removeTrader(L2PcInstance player)
	{
		if (player == null)
			return;

		player.setOfflineTrade(false);
		player.standUp();
		new Disconnection(player).defaultSequence(false);
	}
}
