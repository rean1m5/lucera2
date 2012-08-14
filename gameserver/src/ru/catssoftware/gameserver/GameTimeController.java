package ru.catssoftware.gameserver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import javolution.util.FastList;
import javolution.util.FastSet;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.calendar.L2Calendar;
import ru.catssoftware.gameserver.datatables.ServerData;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.instancemanager.DayNightSpawnManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.ZakenManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance.ConditionListenerDependency;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ClientSetTime;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;
import org.apache.log4j.Logger;



public final class GameTimeController extends Thread
{
	public static final int					TICKS_PER_SECOND	= 10;
	public static final int					MILLIS_IN_TICK		= 1000 / TICKS_PER_SECOND;
	private static final Logger				_log				= Logger.getLogger(GameTimeController.class);
	private static L2Calendar				_calendar;
	private final Set<L2Character>			_movingChars		= new FastSet<L2Character>();
	private final FastList<L2Character>		_endedChars			= new FastList<L2Character>();
	private static final GameTimeController	_instance			= new GameTimeController();
	public long								_startMoveTime;
	public boolean							_shutdown			= false;

	public static GameTimeController getInstance()
	{
		return _instance;
	}

	private class MinuteCounter implements Runnable
	{
		public void run()
		{
			boolean isNight = isNowNight();
			int oldHour = _calendar.getDate().get(Calendar.HOUR_OF_DAY);
			_calendar.getDate().add(Calendar.MINUTE, 1);
			int newHour = _calendar.getDate().get(Calendar.HOUR_OF_DAY);

			if (newHour != oldHour)
			{
				// Обновление времени всем игрокам
				for (L2PcInstance player : L2World.getInstance().getAllPlayers())
					player.sendPacket(ClientSetTime.STATIC_PACKET);

				// Закрываем двери прохода к Закену
				if(ZakenManager.isLoaded)
					if (ZakenManager.OPEN_DOOR_TIME.contains(newHour))
					{
						DoorTable.getInstance().getDoor(21240006).openMe();
						ThreadPoolManager.getInstance().schedule(new ClosePiratesRoom(), ZakenManager.DOOR_OPENED * 60 * 1000);
					}

				// Проверка скила shadow sense
				if (newHour == Config.DATETIME_SUNSET)
				{
					addShadowSense();
				}
				if (newHour == Config.DATETIME_SUNRISE)
				{
					removeShadowSense();
				}

				// Проверка ночного времени
				if (isNight != isNowNight())
				{
					DayNightSpawnManager.getInstance().notifyChangeMode();
					for (L2PcInstance player : L2World.getInstance().getAllPlayers())
						player.refreshConditionListeners(ConditionListenerDependency.GAME_TIME);
				}
			}
			saveData();
		}
	}

	private class MovingObjectArrived implements Runnable
	{
		public void run()
		{
			for (L2Character cha; (cha = getNextEndedChar()) != null;)
			{
				try
				{
					cha.getKnownList().updateKnownObjects();
					if (cha instanceof L2BoatInstance)
						((L2BoatInstance) cha).evtArrived();
					if (cha.hasAI())
						cha.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				}
				catch (Exception e)
				{
					_log.warn("", e);
				}
			}
		}
	}

	public GregorianCalendar getDate()
	{
		return _calendar.getDate();
	}

	public String getFormatedDate()
	{
		SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
		if (Config.DATETIME_SAVECAL)
			format = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
		return format.format(getDate().getTime());
	}

	private GameTimeController()
	{
		super("GameTimeController");
		setDaemon(true);
		setPriority(MAX_PRIORITY);

		if (Config.DATETIME_SAVECAL)
			_calendar = (L2Calendar) loadData();

		if (_calendar == null)
		{
			_calendar = new L2Calendar();
			_calendar.getDate().set(Calendar.YEAR, 1281);
			_calendar.getDate().set(Calendar.MONTH, 5);
			_calendar.getDate().set(Calendar.DAY_OF_MONTH, 5);
			_calendar.getDate().set(Calendar.HOUR_OF_DAY, 23);
			_calendar.getDate().set(Calendar.MINUTE, 45);
			_calendar.setGameStarted(System.currentTimeMillis());
			saveData();
		}

		start();
		ThreadPoolManager.getInstance().scheduleMoveAtFixedRate(new MovingObjectArrived(), MILLIS_IN_TICK, MILLIS_IN_TICK);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new MinuteCounter(), 0, 1000 * Config.DATETIME_MULTI);
		_log.info("GameTimeController: Initialized.");
	}

	public static int getGameTicks()
	{
		return _calendar.gameTicks;
	}

	public int getGameTime()
	{
		return _calendar.getDate().get(Calendar.HOUR_OF_DAY) * 60 + _calendar.getDate().get(Calendar.MINUTE);
	}

	private L2Character[] getMovingChars()
	{
		synchronized (_movingChars)
		{
			return _movingChars.toArray(new L2Character[_movingChars.size()]);
		}
	}

	private L2Character getNextEndedChar()
	{
		synchronized (_endedChars)
		{
			return _endedChars.isEmpty() ? null : _endedChars.removeFirst();
		}
	}

	public boolean isNowNight()
	{
		int hour = _calendar.getDate().get(Calendar.HOUR_OF_DAY);
		if (Config.DATETIME_SUNRISE>Config.DATETIME_SUNSET)
		{
			if (hour < Config.DATETIME_SUNRISE && hour >= Config.DATETIME_SUNSET)
				return true;
		}
		else
		{
			if (hour < Config.DATETIME_SUNRISE || hour >= Config.DATETIME_SUNSET)
				return true;
		}
		return false;
	}

	private L2Calendar loadData()
	{

		L2Calendar cal = null;
		try {
			cal = new L2Calendar();
			cal.getDate().set(Calendar.YEAR, ServerData.getInstance().getData().getInteger("GameTime.year"));
			cal.getDate().set(Calendar.MONTH, ServerData.getInstance().getData().getInteger("GameTime.month"));
			cal.getDate().set(Calendar.DAY_OF_MONTH, ServerData.getInstance().getData().getInteger("GameTime.day"));
			cal.getDate().set(Calendar.HOUR_OF_DAY, ServerData.getInstance().getData().getInteger("GameTime.hour"));
			cal.getDate().set(Calendar.MINUTE, ServerData.getInstance().getData().getInteger("GameTime.minute"));
			cal.setGameStarted(ServerData.getInstance().getData().getLong("GameTime.started"));
			return cal;
		} catch(IllegalArgumentException e) {
			return null;
		}
	}

	private static final class ArrivedCharacterManager extends AbstractFIFOPeriodicTaskManager<L2Character>
	{
		private static final ArrivedCharacterManager _instance = new ArrivedCharacterManager();

		public static ArrivedCharacterManager getInstance()
		{
			return _instance;
		}

		private ArrivedCharacterManager()
		{
			super(GameTimeController.MILLIS_IN_TICK);
		}

		@Override
		protected void callTask(L2Character cha)
		{
			cha.getKnownList().updateKnownObjects();
			
			if (cha instanceof L2BoatInstance)
				((L2BoatInstance)cha).evtArrived();
			
			if (cha.hasAI())
				cha.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
		}

		@Override
		protected String getCalledMethodName()
		{
			return "getAI().notifyEvent(CtrlEvent.EVT_ARRIVED)";
		}
	}

	private void moveObjects()
	{
		for (L2Character cha : getMovingChars())
		{
			if (!cha.updatePosition(_calendar.gameTicks))
			{
				continue;
			}

			synchronized (_movingChars)
			{
				_movingChars.remove(cha);
			}

			ArrivedCharacterManager.getInstance().add(cha);
		}
	}

	public void registerMovingChar(L2Character cha)
	{
		synchronized (_movingChars)
		{
			_movingChars.add(cha);
		}
	}

	@Override
	public void run()
	{
		for (;;)
		{
			long currentTime = System.currentTimeMillis();
			_startMoveTime = currentTime;
			_calendar.gameTicks = (int) ((currentTime - _calendar.getGameStarted()) / MILLIS_IN_TICK);
			moveObjects();
			currentTime = System.currentTimeMillis();
			_calendar.gameTicks = (int) ((currentTime - _calendar.getGameStarted()) / MILLIS_IN_TICK);
			//move delay
			long sleepTime = Config.DATETIME_MOVE_DELAY - (currentTime - _startMoveTime);
			if (sleepTime > 0)
			{
				if (_shutdown==true)
				{
					break;
				}
				try
				{
					sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private class ClosePiratesRoom implements Runnable
	{
		public void run()
		{
			DoorTable.getInstance().getDoor(21240006).closeMe();
		}
	}

	public void saveData()
	{
		if (Config.DATETIME_SAVECAL)
		{
			ServerData.getInstance().getData().set("GameTime.year",_calendar.getDate().get(Calendar.YEAR));
			ServerData.getInstance().getData().set("GameTime.month",_calendar.getDate().get(Calendar.MONTH));
			ServerData.getInstance().getData().set("GameTime.day",_calendar.getDate().get(Calendar.DAY_OF_MONTH));
			ServerData.getInstance().getData().set("GameTime.hour",_calendar.getDate().get(Calendar.HOUR_OF_DAY));
			ServerData.getInstance().getData().set("GameTime.minute",_calendar.getDate().get(Calendar.MINUTE));
			ServerData.getInstance().getData().set("GameTime.started",_calendar.getGameStarted());
		}
	}

	private void addShadowSense()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player != null && skill != null && player.getRace().ordinal() == 2 && player.getSkillLevel(294) == 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_APPLIES);
				sm.addSkillName(294);
				player.sendPacket(sm);
			}
		}
	}

	private void removeShadowSense()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player != null && skill != null && player.getRace().ordinal() == 2 && player.getSkillLevel(294) == 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_DISAPPEARS);
				sm.addSkillName(294);
				player.sendPacket(sm);
			}
		}
	}



	public void stopTimer()
	{
		interrupt();
	}
}