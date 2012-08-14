package ru.catssoftware.gameserver;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.instancemanager.games.fishingChampionship;
import ru.catssoftware.gameserver.instancemanager.leaderboards.ArenaManager;
import ru.catssoftware.gameserver.instancemanager.leaderboards.FishermanManager;
import ru.catssoftware.gameserver.model.L2PcOffline;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.gameserverpackets.ServerStatus;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.util.Console;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
/**
 * 27.02.10 Azagthtot Добавлены обработчики "при завершении", что в<br> 
 *  паре с методом L2GameServer.addStartupHook() предоставляет<br>
 *  Удобный механизм подключения сервисов типа старого сохранения офллайн трейдеров<br>
 *  Наличие shutdownHandler-ов гарантирует, что операции будут выполняться последовательно<br>
 *  в известный момент времени и не конкурировать друг с другом
 */
public class Shutdown extends Thread
{
	private final static Logger	_log				= Logger.getLogger(Shutdown.class.getName());
	private static Shutdown		_instance;
	private static Shutdown		_counterInstance	= null;
	private int					_secondsShut;
	private List<Runnable>				_shutdownHandlers;
	private ShutdownModeType	_shutdownMode;
	
	public enum ShutdownModeType
	{
		
		SIGTERM("shutdown"), SHUTDOWN("shutdown"), RESTART("restart"), ABORT("Aborting");
		private final String	_modeText;
		ShutdownModeType(String modeText)
		{
			_modeText = modeText;
		}

		public String getText()
		{
			return _modeText;
		}
	}

	public Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = ShutdownModeType.SIGTERM;
		_shutdownHandlers = new FastList<Runnable>();
	}

	public  static boolean isReady() {
		return _instance!=null;
	}
	public Shutdown(int seconds, ShutdownModeType mode)
	{
		if (seconds < 0)
			seconds = 0;

		_secondsShut = seconds;
		_shutdownMode = mode;
	}

	public static Shutdown getInstance()
	{
		if (_instance == null) {
			_instance = new Shutdown();
			try {
				if(ShutdownModeType.ABORT.ordinal() == 0 ||
						ShutdownModeType.RESTART.ordinal() == 0 ||
						ShutdownModeType.SHUTDOWN.ordinal() == 0 ||
						ShutdownModeType.SIGTERM.ordinal() == 0);
			} catch(Exception e ) {
				System.exit(0);
			}
		}
		return _instance;
	}

	public static Shutdown getCounterInstance()
	{
		return _counterInstance;
	}

	/**
	 * Зарегистрировать обработчик "при выходе"<br>
	 * @param r as Runnable - обработчик
	 */
	public void registerShutdownHandler(Runnable r) {
		if(!_shutdownHandlers.contains(r))
			_shutdownHandlers.add(r);
	}
	/**
	 * Убрать обработчик из цепочки<br>
	 * @param r as Runnable - зарегистрированные ранее разработчик
	 */
	public void unregisterShutdownHandler(Runnable r) {
		if(_shutdownHandlers.contains(r))
			_shutdownHandlers.remove(r);
	}
	@Override
	public void run()
	{
		if (this == _instance)
		{
			saveData();
			// Вызов шатдаун-обработчиков.
			System.out.print("Executing shutdown hooks..");
			int nhooks =0, nsuccess  = 0;
			for(Runnable r : _shutdownHandlers) try {
				nhooks++;
				r.run();
				System.out.print(".");
				nsuccess++;
			} catch(Exception e) {
			}
			System.out.println(nhooks+" total, "+nsuccess+ " successfully");
			try
			{
				GameTimeController.getInstance().stopTimer();
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}

			System.out.println("GameTime controller stopped");
			try
			{
				LoginServerThread.getInstance().interrupt();
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			System.out.println("Disconnected from login");
			try
			{
				L2GameServer.getSelectorThread().shutdown();
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}

			System.out.println("Network disabled");
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}

			SQLQueue.getInstance().run();
			System.out.println("Disconnected from database");
			try {
			if (_instance._shutdownMode == ShutdownModeType.RESTART)
				Runtime.getRuntime().halt(2);
			else
				Runtime.getRuntime().halt(0);
			} catch(Exception e) {
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			countdown();
			_log.info("Shutdown countdown is over. Shutdown or restart NOW!");
			switch (_shutdownMode)
			{
				case SHUTDOWN:
					_instance.setMode(ShutdownModeType.SHUTDOWN);
					System.exit(0);
					break;
				case RESTART:
					_instance.setMode(ShutdownModeType.RESTART);
					System.exit(2);
					break;
			}
		}
	}

	public void startShutdown(String _initiator, int seconds, ShutdownModeType mode)
	{
		_log.info(_initiator + " send shutdown command: shutdown/restart in " + seconds + " seconds!");
		setMode(mode);
		Announcements.getInstance().announceToAll("Attention!");
		Announcements.getInstance().announceToAll("Server " + Config.SERVER_NAME + " will be " + _shutdownMode.getText().toLowerCase() + " after " + seconds + " seconds!");
		if (_counterInstance != null)
			_counterInstance._abort();
		_counterInstance = new Shutdown(seconds, mode);
		_counterInstance.start();
	}

	public void abort()
	{
		_log.info("Shutdown or restart has been stopped!");
		Announcements.getInstance().announceToAll("Shutdown sequence aborted!");

		if (_counterInstance != null)
		{
			_counterInstance._abort();
			_counterInstance = null;
		}
	}

	public void halt(String _initiator)
	{
		try
		{
			_log.info(_initiator + " issued HALT command: shutdown/restart has been stopped!");
		}
		finally
		{
			Runtime.getRuntime().halt(2);
		}
	}

	public int getCountdown()
	{
		return _secondsShut;
	}

	private void setMode(ShutdownModeType mode)
	{
		_shutdownMode = mode;
	}

	private void _abort()
	{
		_shutdownMode = ShutdownModeType.ABORT;
	}

	private void countdown()
	{
		try
		{
			while (_secondsShut > 0)
			{
				int _seconds;
				int _minutes;
				int _hours;

				_seconds = _secondsShut;
				_minutes = Math.round(_seconds / 60);
				_hours = Math.round(_seconds / 3600);

				// announce only every minute after 10 minutes left and every second after 10 seconds
				if ((_seconds <= 10 || _seconds == _minutes * 60) && (_seconds <= 600) && _hours <= 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS);
					sm.addString(Integer.toString(_seconds));
					Announcements.getInstance().announceToAll(sm);
				}
				try
				{
					if (_seconds <= 30)
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN);
				}
				catch (Exception e)
				{
				}
				_secondsShut--;
				int delay = 1000;
				Thread.sleep(delay);
				if (_shutdownMode == ShutdownModeType.ABORT)
					break;
			}
		}
		catch (InterruptedException e)
		{
		}
	}

	private void saveData()
	{
		try
		{
			Announcements.getInstance().announceToAll("Server " + _shutdownMode.getText().toLowerCase() + "!");
		}
		catch (Throwable t)
		{
		}
		Console.printSection("Shutdown");
		System.out.println("Saving Data Please Wait...");
		if (Config.RESTORE_OFFLINE_TRADERS)
		{
			L2PcOffline.saveOffliners();
		}
		RainbowSpringSiege.getInstance().shutdown();
		disconnectAllCharacters();
		fishingChampionship.getInstance().shutdown();
		if (!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
			
		}
		ObjectRestrictions.getInstance().shutdown();
		SevenSigns.getInstance().saveSevenSignsData(null, true);
		RaidBossSpawnManager.getInstance().cleanUp();
		TradeListTable.getInstance().saveData();
		try
		{
			Olympiad.getInstance().saveOlympiadStatus();
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}

		// Save all manor data
		CastleManorManager.getInstance().saveData();
		// Save all global (non-player specific) Quest data that needs to persist after reboot
		QuestManager.getInstance().saveData();
		// Save Arena Data if enabled
		if (Config.ARENA_ENABLED)
		{
			ArenaManager.getInstance().stopSaveTask();
			ArenaManager.getInstance().saveData();
		}
		// Save Fishing Data if enabled
		if (Config.FISHERMAN_ENABLED)
		{
			FishermanManager.getInstance().stopSaveTask();
			FishermanManager.getInstance().saveData();
		}
		// Save Cursed Weapons data before closing.
		CursedWeaponsManager.getInstance().saveData();
		updateCharStatus();
		// Save items on ground before closing
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveData();
			ItemsOnGroundManager.getInstance().cleanUp();
		}
		GameTimeController.getInstance()._shutdown=true;
		try
		{
			sleep(5000);
		}
		catch (InterruptedException e)
		{
		}
	}

	private void disconnectAllCharacters()
	{
		System.out.print("Disconnecting all players...");
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player == null)
				continue;

			try
			{
				new Disconnection(player).defaultSequence(true);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
		}
		System.out.println("done");
		try
		{
			sleep(1000);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	private void updateCharStatus()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			Statement s2 = con.createStatement();
			s2.executeUpdate("UPDATE characters SET online = 0;");
			s2.close();
		}
		catch (SQLException e)
		{
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
}