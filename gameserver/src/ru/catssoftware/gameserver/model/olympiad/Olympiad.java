package ru.catssoftware.gameserver.model.olympiad;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.ServerData;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Hero;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.util.StatsSet;


 /**
  * Класс Олимпиады
  * Служит для работы соревнований между персонажами
  * CatsSoftware
  **/

public final class Olympiad
{
	/**
	 * Основные переменные класса
	 **/
	private static final Logger							_log							= Logger.getLogger(Olympiad.class);
	private static Olympiad								_instance						= null;
	private static Map<Integer, StatsSet>				_nobles = new FastMap<Integer, StatsSet>();
	protected static FastList<StatsSet>					_heroesToBe = new FastList<StatsSet>();
	private static FastList<L2PcInstance>				_nonClassBasedRegisters = new FastList<L2PcInstance>();
	private static Map<Integer, FastList<L2PcInstance>>	_classBasedRegisters = new FastMap<Integer, FastList<L2PcInstance>>();

	/**
	 * SQL запросы в базу данных
	 **/
	public static final String							OLYMPIAD_HTML_PATH				= "data/html/olympiad/";
	private static final String							OLYMPIAD_LOAD_NOBLES			= "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, "
																						+ "characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, "
																						+ "olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn "
																						+ "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
	private static final String							OLYMPIAD_SAVE_NOBLES			= "INSERT INTO olympiad_nobles "
																						+ "(`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,"
																						+ "`competitions_drawn`) VALUES (?,?,?,?,?,?,?)";
	private static final String							OLYMPIAD_UPDATE_NOBLES			= "UPDATE olympiad_nobles SET "
																						+ "olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?";
	private static final String							OLYMPIAD_GET_HEROS				= "SELECT olympiad_nobles.charId, characters.char_name "
																						+ "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId "
																						+ "AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 "
																						+ "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC";
	private static final String							GET_EACH_CLASS_LEADER			= "SELECT characters.char_name from olympiad_nobles_eom, characters "
																						+ "WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? "
																						+ "AND olympiad_nobles_eom.competitions_done >= 9 "
																						+ "ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC LIMIT 10";
	private static final String							GET_EACH_CLASS_LEADER_CURRENT	= "SELECT characters.char_name from olympiad_nobles, characters "
																						+ "WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? "
																						+ "AND olympiad_nobles.competitions_done >= 9 "
																						+ "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC LIMIT 10";
	private static final String							OLYMPIAD_DELETE_ALL				= "TRUNCATE olympiad_nobles";
	private static final String							OLYMPIAD_MONTH_CLEAR			= "TRUNCATE olympiad_nobles_eom";
	private static final String							OLYMPIAD_MONTH_CREATE			= "INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles";

	/**
	 * Переменные данных персонажа
	 **/
	public static final String							CHAR_ID							= "charId";
	public static final String							CLASS_ID						= "class_id";
	public static final String							CHAR_NAME						= "char_name";
	public static final String							POINTS							= "olympiad_points";
	public static final String							COMP_DONE						= "competitions_done";
	public static final String							COMP_WON						= "competitions_won";
	public static final String							COMP_LOST						= "competitions_lost";
	public static final String							COMP_DRAWN						= "competitions_drawn";

	/**
	 * Список классов олимпиады
	 **/
	private static final int[] HERO_IDS =
	{
		88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118
	};

	// Переменные хранения данных текущего состояния олимпиады
	protected long										_olympiadEnd;
	protected long										_validationEnd;
	protected int										_period;
	protected long										_nextWeeklyChange;
	protected int										_currentCycle;
	private long										_compEnd;
	private Calendar									_compStart;
	protected static boolean							_inCompPeriod;
	protected static boolean							_compStarted					= false;
	protected ScheduledFuture<?>						_scheduledCompStart;
	protected ScheduledFuture<?>						_scheduledCompEnd;
	protected ScheduledFuture<?>						_scheduledOlympiadEnd;
	protected ScheduledFuture<?>						_scheduledWeeklyTask;
	protected ScheduledFuture<?>						_scheduledValdationTask;

	public static Olympiad getInstance()
	{
		if (_instance == null)
			_instance = new Olympiad();
		return _instance;
	}

	/**
	 * Тип участия
	 * Классовый и ВнеКлассовый
	 * CatsSoftware
	 **/
	protected static enum COMP_TYPE
	{
		CLASSED, 
		NON_CLASSED
	}
	
	/**
	 * Главный метод загрузки
	 * Инициализирует старт олимпиады
	 **/
	public Olympiad()
	{
		if(!Config.ALT_OLY_ENABLED)
			return;
		load();
		if (_period == 0)
			init();
	}

	/**
	 * Возвращает кол-во стадионов
	 * CatsSoftware
	 **/
	public static Integer getStadiumCount()
	{
		return OlympiadManager.STADIUMS.length;
	}

	/**
	 * Загрузка олимпиады
	 * Данные олимпиады хранятся в файле конфигурации OLYMPIAD_DATA_FILE
	 * CatsSoftware
	 **/
	private void load()
	{
		_nobles = new FastMap<Integer, StatsSet>();
		try {
			_currentCycle = ServerData.getInstance().getData().getInteger("Olympiad.CurrentCycle");
		} catch(IllegalArgumentException e) {
			_currentCycle = 0;
		}
		try {
			_period = ServerData.getInstance().getData().getInteger("Olympiad.Period");
		} catch(IllegalArgumentException e) {
			_period = 0;
		}
		try {
			_olympiadEnd = ServerData.getInstance().getData().getLong("Olympiad.OlympiadEnd");
		} catch(IllegalArgumentException e) {
			_olympiadEnd = 0;
		}
		try {
			_validationEnd = ServerData.getInstance().getData().getLong("Olympiad.ValdationEnd");
		} catch(IllegalArgumentException e) {
			_validationEnd = 0;
		}
		try {
			_nextWeeklyChange = ServerData.getInstance().getData().getLong("Olympiad.NextWeeklyChange");
		} catch(IllegalArgumentException e) {
			_nextWeeklyChange = 0;
		}


		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				StatsSet statData = new StatsSet();
				int charId = rset.getInt(CHAR_ID);
				statData.set(CLASS_ID, rset.getInt(CLASS_ID));
				statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
				statData.set(POINTS, rset.getInt(POINTS));
				statData.set(COMP_DONE, rset.getInt(COMP_DONE));
				statData.set(COMP_WON, rset.getInt(COMP_WON));
				statData.set(COMP_LOST, rset.getInt(COMP_LOST));
				statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				statData.set("to_save", false);
				_nobles.put(charId, statData);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Olympiad System: Error loading noblesse data from database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

		synchronized (this)
		{
			_log.info("Olympiad System: Loading Olympiad System....");
			switch (_period)
			{
				case 0:
					if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
					   setNewOlympiadEnd();	
					else
						scheduleWeeklyChange();
					break;
				case 1:
					if (_validationEnd > Calendar.getInstance().getTimeInMillis())
						_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
					else
					{
						_currentCycle++;
						_period = 0;
						deleteNobles();
						setNewOlympiadEnd();
					}
					break;
				default:
					_log.warn("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
					return;
			}
			if (_period == 0)
				_log.info("Olympiad System: Currently in Olympiad Period");
			else
				_log.info("Olympiad System: Currently in Validation Period");

			long milliToEnd;
			if (_period == 0)
				milliToEnd = getMillisToOlympiadEnd();
			else
				milliToEnd = getMillisToValidationEnd();

			_log.info("Olympiad System: " + Math.round(milliToEnd / 60000) + " minutes until period ends");

			if (_period == 0)
			{
				milliToEnd = getMillisToWeekChange();

				_log.info("Olympiad System: Next weekly change is in " + Math.round(milliToEnd / 60000) + " minutes");
			}
		}
		_log.info("Olympiad System: Loaded " + _nobles.size() + " Nobles");
	}

	/**
	 * Инициализация олимпиады
	 * CatsSoftware
	 **/
	protected void init()
	{
		// Во время первого периода соревнования не проводятся
		if (_period == 1)
			return;
		
		// Переменные класса
		_nonClassBasedRegisters					= new FastList<L2PcInstance>();
		_classBasedRegisters					= new FastMap<Integer, FastList<L2PcInstance>>();
		int _cPeriodTime						= Config.ALT_OLY_CPERIOD * 60 * 60000;

		// Создаем календарные даты событий
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_TIME);
		_compStart.set(Calendar.MINUTE, Config.ALT_OLY_MIN);
		_compEnd = _compStart.getTimeInMillis() + _cPeriodTime;

		// Отменяем текущую задачу
		if (_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);

		// Стартуем задачу
		
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), getMillisToOlympiadEnd());
		// Обновляем данные
		updateCompStatus();
	}

	/**
	 * Завершающая задача олимпиады
	 * CatsSoftware
	 **/
	protected class OlympiadEndTask implements Runnable
	{
		public void run()
		{
			// Переменные
			int _vPeriodTime						= Config.ALT_OLY_VPERIOD * 60 * 60000;
			_log.info("Olympiad: Finishing "+_period+" period");
			// Анонсирование информации
			SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED);
			sm.addNumber(_currentCycle);
			Announcements.getInstance().announceToAll(sm);
			Announcements.getInstance().announceToAll(Message.getMessage(null, Message.MessageId.MSG_OLY_END));

			// Отмена задачи
			if (_scheduledWeeklyTask != null)
				_scheduledWeeklyTask.cancel(true);
			// Сохранение данных дворянин
			saveNobleData();
			// Обнуление данных периода
			_period = 1;
			// Определение героев
			sortHerosToBe();
			// Выдача бонуса героям
			giveHeroBonus();
			// Запись новых героев
			Hero.getInstance().computeNewHeroes(_heroesToBe);
			// Сохранение олимпиады
			saveOlympiadStatus();
			// Сохранение данных
			updateMonthlyData();

			// Создание события
			Calendar validationEnd = Calendar.getInstance();
			_validationEnd = validationEnd.getTimeInMillis() + _vPeriodTime;

			_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
		}
	}

	/**
	 * Проверочный период, завершение
	 * CatsSoftware
	 **/
	protected class ValidationEndTask implements Runnable
	{
		public void run()
		{
			// Анонсирование инфы
			Announcements.getInstance().announceToAll(Message.getMessage(null, Message.MessageId.MSG_OLY_NEW_PERIOD));
			// Сброс периодов
			_period = 0;
			// Начисляем периоды олимпиады
			_currentCycle++;
			// Удаление участников
			deleteNobles();
			// Установка дат
			setNewOlympiadEnd();
			// Инициализация олимпиады заново
			init();
		}
	}

	/**
	 * Регистрация участника
	 * Возвращает boolean, да/нет
	 * CatsSoftware
	 **/
	public boolean registerNoble(L2PcInstance noble, boolean classBased)
	{
		if (noble == null)
			return false;

		SystemMessage sm;
		if (!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		if (!Config.ALT_OLY_ENABLED)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		if (!_inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		if (noble.getGameEvent() != null)
		{
			noble.sendMessage(Message.getMessage(noble, Message.MessageId.MSG_NO_REGISTER_WHILE_EVENT));
			return false;
		}
		if(noble.getInventory().getSize() >= noble.getInventoryLimit() * 0.8) {
			sm = new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			noble.sendPacket(sm);
			return false;
		}
		if(noble.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE) { 
			sm = new SystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1);
			sm.addString("trade");
			noble.sendPacket(sm);
			return false;
		}
		
		if (noble.getBaseClass() != noble.getClassId().getId())
		{
			sm = new SystemMessage(SystemMessageId.C1_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_CLASS_CHARACTER);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		if (noble.isCursedWeaponEquipped())
		{
			sm = new SystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			noble.sendPacket(sm);
			return false;
		}
		if (noble.getInventoryLimit() * 0.8 <= noble.getInventory().getSize())
		{
			sm = new SystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
			sm.addPcName(noble);
			noble.sendPacket(sm);
			return false;
		}
		if (getMillisToCompEnd() < 600000)
		{
			noble.sendPacket(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			return false;
		}
		if(!Config.Allow_Same_HWID_On_Olympiad && noble.getHWid()!=null && noble.getHWid().length()!=0) {
			for(FastList<L2PcInstance> classed : _classBasedRegisters.values() ) {
				for(L2PcInstance pc : classed)
					if(pc.isOnline()>0 && L2GameClient.isSameHWID(noble.getHWid(),pc.getHWid())) {
						sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
						sm.addString(noble.getName());
						noble.sendPacket(sm);
						return false;
					}
			}
			for(L2PcInstance pc : _nonClassBasedRegisters) {
				if(pc.isOnline()>0 && L2GameClient.isSameHWID(noble.getHWid(),pc.getHWid())) {
					sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
					sm.addString(noble.getName());
					noble.sendPacket(sm);
					return false;
				}
			}
		}
		
		if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			for (L2PcInstance participant : classed)
			{
				if (participant.getObjectId() == noble.getObjectId())
				{
					sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
					sm.addString(noble.getName());
					noble.sendPacket(sm);
					return false;
				}
			}
		}
		if (isRegisteredInComp(noble))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		if (!_nobles.containsKey(noble.getObjectId()))
		{
			StatsSet statDat = new StatsSet();
			statDat.set(CLASS_ID, noble.getClassId().getId());
			statDat.set(CHAR_NAME, noble.getName());
			statDat.set(POINTS, Config.ALT_OLY_START_PCOUNT);
			statDat.set(COMP_DONE, 0);
			statDat.set(COMP_WON, 0);
			statDat.set(COMP_LOST, 0);
			statDat.set(COMP_DRAWN, 0);
			statDat.set("to_save", true);
			_nobles.put(noble.getObjectId(), statDat);
		}
		if (classBased && getNoblePoints(noble.getObjectId()) < 3)
		{
			noble.sendMessage(String.format(Message.getMessage(noble, Message.MessageId.MSG_POINTS_REQUIRE),3));
			return false;
		}
		if (!classBased && getNoblePoints(noble.getObjectId()) < 5)
		{
			noble.sendMessage(String.format(Message.getMessage(noble, Message.MessageId.MSG_POINTS_REQUIRE),5));
			return false;
		}

		if (classBased)
		{
			if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
			{
				FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
				classed.add(noble);

				_classBasedRegisters.remove(noble.getClassId().getId());
				_classBasedRegisters.put(noble.getClassId().getId(), classed);
			}
			else
			{
				FastList<L2PcInstance> classed = new FastList<L2PcInstance>();
				classed.add(noble);

				_classBasedRegisters.put(noble.getClassId().getId(), classed);
			}
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
		}
		else
		{
			_nonClassBasedRegisters.add(noble);
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
		}
		return true;
	}

	/**
	 * Возвращает кол-во дворян, которые участвовали в олимпиаде
	 * CatsSoftware
	 **/
	protected static int getNobleCount()
	{
		return _nobles.size();
	}

	protected static StatsSet getNobleStats(int playerId)
	{
		return _nobles.get(playerId);
	}

	protected static synchronized void updateNobleStats(int playerId, StatsSet stats)
	{
		_nobles.remove(playerId);
		_nobles.put(playerId, stats);
	}

	protected static FastList<L2PcInstance> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}

	protected static Map<Integer, FastList<L2PcInstance>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}

	protected static FastList<Integer> hasEnoughRegisteredClassed()
	{
		FastList<Integer> result = new FastList<Integer>();

		for (Integer classList : getRegisteredClassBased().keySet())
		{
			if (getRegisteredClassBased().get(classList).size() >= Config.ALT_OLY_CLASSED)
				result.add(classList);
		}

		if (!result.isEmpty())
			return result;

		return null;
	}

	protected static boolean hasEnoughRegisteredNonClassed()
	{
		return Olympiad.getRegisteredNonClassBased().size() >= Config.ALT_OLY_NONCLASSED;
	}

	protected static void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
	}

	public boolean isRegistered(L2PcInstance noble)
	{
		boolean result = false;

		if (_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(noble))
			result = true;

		else if (_classBasedRegisters != null && _classBasedRegisters.containsKey(noble.getClassId().getId()))
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			if (classed != null && classed.contains(noble))
				result = true;
		}

		return result;
	}

	public boolean unRegisterNoble(L2PcInstance noble)
	{
		if (noble == null)
			return false;

		if(noble.getOlympiadGameId()!=-1)
			return false;
		SystemMessage sm;
		if (!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		if (!Config.ALT_OLY_ENABLED)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		if (!_inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		if (!isRegistered(noble))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			return false;
		}
		for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
		{
			if (game == null)
				continue;

			if (game._playerOneID == noble.getObjectId() || game._playerTwoID == noble.getObjectId())
			{
				noble.sendMessage(Message.getMessage(noble, Message.MessageId.MSG_YOU_CANT_DOIT_AT_THIS_TIME));
				return false;
			}
		}
		if (_nonClassBasedRegisters.contains(noble))
			_nonClassBasedRegisters.remove(noble);
		else
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			classed.remove(noble);

			_classBasedRegisters.remove(noble.getClassId().getId());
			_classBasedRegisters.put(noble.getClassId().getId(), classed);
		}
		noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
		return true;
	}

	public void removeDisconnectedCompetitor(L2PcInstance player)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()) != null)
			OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()).handleDisconnect(player);

		FastList<L2PcInstance> classed = _classBasedRegisters.get(player.getClassId().getId());

		if (_nonClassBasedRegisters.contains(player))
			_nonClassBasedRegisters.remove(player);
		else if (classed != null && classed.contains(player))
		{
			classed.remove(player);

			_classBasedRegisters.remove(player.getClassId().getId());
			_classBasedRegisters.put(player.getClassId().getId(), classed);
		}
	}

	public void notifyCompetitorDamage(L2PcInstance player, int damage, int gameId)
	{
		if (OlympiadManager.getInstance().getOlympiadGames().get(gameId) != null)
			OlympiadManager.getInstance().getOlympiadGames().get(gameId).addDamage(player, damage);
	}

	private Runnable _olyStart = new Runnable()
	{
		public void run()
		{
			if (isOlympiadEnd())
				return;

			_inCompPeriod = true;
			OlympiadManager om = new OlympiadManager();

			Announcements.getInstance().announceToAll(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED);
			_log.info("Olympiad System: Olympiad Game Started");

			Thread olyCycle = new Thread(om);
			olyCycle.start();

			long regEnd = getMillisToCompEnd() - 600000;
			if (regEnd > 0)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					public void run()
					{
						Announcements.getInstance().announceToAll(SystemMessageId.OLYMPIAD_REGISTRATION_PERIOD_ENDED);
					}
				}, regEnd);
			}

			_scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					if (isOlympiadEnd())
						return;
					_inCompPeriod = false;
					Announcements.getInstance().announceToAll(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED);
					_log.info("Olympiad System: Olympiad Game Ended");

					ThreadPoolManager.getInstance().schedule(new Runnable() {
						@Override
						public void run()
						{
							if (OlympiadGame._battleStarted)
								ThreadPoolManager.getInstance().schedule(this, 60000);
							else
							{
								saveOlympiadStatus();
								init();
							}
						}
				
					}, 0);
				}
			}, getMillisToCompEnd());
		}
	};
	private void updateCompStatus()
	{
		synchronized (this)
		{
			long milliToStart = getMillisToCompBegin();

			double numSecs = (milliToStart / 1000) % 60;
			double countDown = ((milliToStart / 1000) - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);

			_log.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
			_log.info("Olympiad System: Event starts/started : " + _compStart.getTime());
		}

		_scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(_olyStart, getMillisToCompBegin());
	}

	private long getMillisToOlympiadEnd()
	{
		if(_olympiadEnd - Calendar.getInstance().getTimeInMillis() < 0)
			return 10000;
		return (_olympiadEnd - Calendar.getInstance().getTimeInMillis());
	}

	public void manualSelectHeroes()
	{
		if (_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);

		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), 0);
	}

	protected long getMillisToValidationEnd()
	{
		if (_validationEnd > Calendar.getInstance().getTimeInMillis())
			return (_validationEnd - Calendar.getInstance().getTimeInMillis());
		return 10L;
	}

	public boolean isOlympiadEnd()
	{
		return (_period != 0);
	}

	/**
	 * Создание периода
	 * CatsSoftware
	 **/
	protected void setNewOlympiadEnd()
	{
		// Переменные
		int _wPeriodTime						= Config.ALT_OLY_WPERIOD * 24 * 60 * 60000;

		// Анонсирование
		SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED);
		sm.addNumber(_currentCycle);
		Announcements.getInstance().announceToAll(sm);

		// Создание календарного события
		Calendar currentTime = Calendar.getInstance();
		currentTime.set(Calendar.HOUR_OF_DAY, 0);
		currentTime.set(Calendar.MINUTE, 1);
		currentTime.set(Calendar.SECOND, 00);
		if(Config.ALT_OLY_DURATION_TYPES.equalsIgnoreCase("month")) {
			currentTime.add(Calendar.MONTH, Config.ALT_OLY_DURATION);
			currentTime.set(Calendar.DAY_OF_MONTH, 1);
		} else if(Config.ALT_OLY_DURATION_TYPES.equalsIgnoreCase("week")) {
			currentTime.set(Calendar.DAY_OF_WEEK, 1);
			currentTime.add(Calendar.WEEK_OF_YEAR, Config.ALT_OLY_DURATION);
		}
		_olympiadEnd = currentTime.getTimeInMillis();

		Calendar nextChange = Calendar.getInstance();
		_nextWeeklyChange = nextChange.getTimeInMillis() + _wPeriodTime;
		scheduleWeeklyChange();
	}

	/**
	 * Возвращает данные
	 * Сейчас период соревнований?
	 * CatsSoftware
	 **/
	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}

	/**
	 * Возвращает время
	 * CatsSoftware
	 **/
	private long getMillisToCompBegin()
	{
		if (_compStart.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis())
			return 120000L;

		if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
			return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());

		return setNewCompBegin();
	}

	/**
	 * Возвращает дану заверешения нового периода дня
	 * CatsSoftware
	 **/
	private long setNewCompBegin()
	{
		// Переменные
		int _cPeriodTime						= Config.ALT_OLY_CPERIOD * 60 * 60000;

		// Установка календарных событий
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_TIME);
		_compStart.set(Calendar.MINUTE, Config.ALT_OLY_MIN);
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		_compEnd = _compStart.getTimeInMillis() + _cPeriodTime;

		_log.info("Olympiad System: New Schedule @ " + _compStart.getTime());

		return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}

	protected long getMillisToCompEnd()
	{
		return (_compEnd - Calendar.getInstance().getTimeInMillis());
	}

	private long getMillisToWeekChange()
	{
		if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
			return (_nextWeeklyChange - Calendar.getInstance().getTimeInMillis());
		return 10L;
	}

	/**
	 * Смена еженедельного периода
	 * CatsSoftware
	 **/
	private void scheduleWeeklyChange()
	{
		// Переменные
		final int _wPeriodTime						= Config.ALT_OLY_WPERIOD * 24 * 60 * 60000;

		_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			public void run()
			{
				addWeeklyPoints();
				_log.info("Olympiad System: Added weekly points to nobles");

				Calendar nextChange = Calendar.getInstance();
				_nextWeeklyChange = nextChange.getTimeInMillis() + _wPeriodTime;
			}
		}, getMillisToWeekChange(), _wPeriodTime);
	}

	/**
	 * Метод добавления очков участнику
	 * CatsSoftware
	 **/
	protected synchronized void addWeeklyPoints()
	{
		if (_period == 1)
			return;

		for (Integer nobleId : _nobles.keySet())
		{
			StatsSet nobleInfo = _nobles.get(nobleId);
			int currentPoints = nobleInfo.getInteger(POINTS);
			currentPoints += Config.ALT_OLY_WEEKLY_PCOUNT;
			nobleInfo.set(POINTS, currentPoints);
			updateNobleStats(nobleId, nobleInfo);
		}
	}

	/**
	 * Метод получения списка игровов игры
	 * CatsSoftware
	 **/
	public L2PcInstance[] getPlayers(int Id)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(Id) == null)
			return null;
		else
			return OlympiadManager.getInstance().getOlympiadGame(Id).getPlayers();
	}

	public int getCurrentCycle()
	{
		return _currentCycle;
	}

	public static void addSpectator(int id, L2PcInstance spectator, boolean storeCoords)
	{
		if (!Config.ALT_OLY_ENABLED)
		{
			spectator.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}
		if (!_inCompPeriod)
		{
			spectator.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}
		if (getInstance().isRegisteredInComp(spectator))
		{
			spectator.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
			return;
		}
		if (spectator.getGameEvent()!=null)
		{
			spectator.sendMessage(Message.getMessage(spectator, Message.MessageId.MSG_YOU_CANT_DOIT_AT_THIS_TIME));
			return;
		}
		if (spectator.getPet()!=null)
		{
			spectator.sendMessage(Message.getMessage(spectator, Message.MessageId.MSG_RELEASE_PET));
			return;
		}
		if(spectator.isCastingNow() || spectator.isInCombat() || spectator.isAlikeDead())
			return;
		if(spectator.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE) 
			return;
		OlympiadManager.STADIUMS[id].addSpectator(id, spectator, storeCoords);
	}

	public static int getSpectatorArena(L2PcInstance player)
	{
		for (int i = 0; i < OlympiadManager.STADIUMS.length; i++)
		{
			try {
				if (OlympiadManager.STADIUMS[i].getSpectators().contains(player))
					return i;
			} catch(NullPointerException npe) {
				continue;
			}
		}
		return -1;
	}

	public static void removeSpectator(int id, L2PcInstance spectator)
	{
		OlympiadManager.STADIUMS[id].removeSpectator(spectator);
	}

	public FastList<L2PcInstance> getSpectators(int id)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(id) == null)
			return null;
		return OlympiadManager.STADIUMS[id].getSpectators();
	}

	public Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return OlympiadManager.getInstance().getOlympiadGames();
	}

	public boolean playerInStadia(L2PcInstance player)
	{
		return player.isInsideZone(L2Zone.FLAG_STADIUM);
	}

	public int[] getWaitingList()
	{
		int[] array = new int[2];

		if (!inCompPeriod())
			return null;

		int classCount = 0;

		if (!_classBasedRegisters.isEmpty())
		{
			for (FastList<L2PcInstance> classed : _classBasedRegisters.values())
				classCount += classed.size();
		}

		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();

		return array;
	}

	/**
	 * Save noblesse data to database
	 */
	protected synchronized void saveNobleData()
	{
		if (_nobles == null || _nobles.isEmpty())
			return;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			for (Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);

				if (nobleInfo == null)
					continue;

				int charId = nobleId;
				int classId = nobleInfo.getInteger(CLASS_ID);
				int points = nobleInfo.getInteger(POINTS);
				int compDone = nobleInfo.getInteger(COMP_DONE);
				int compWon = nobleInfo.getInteger(COMP_WON);
				int compLost = nobleInfo.getInteger(COMP_LOST);
				int compDrawn = nobleInfo.getInteger(COMP_DRAWN);
				boolean toSave = nobleInfo.getBool("to_save");

				if (toSave)
				{
					statement = con.prepareStatement(OLYMPIAD_SAVE_NOBLES);
					statement.setInt(1, charId);
					statement.setInt(2, classId);
					statement.setInt(3, points);
					statement.setInt(4, compDone);
					statement.setInt(5, compWon);
					statement.setInt(6, compLost);
					statement.setInt(7, compDrawn);

					nobleInfo.set("to_save", false);

					updateNobleStats(nobleId, nobleInfo);
				}
				else
				{
					statement = con.prepareStatement(OLYMPIAD_UPDATE_NOBLES);
					statement.setInt(1, points);
					statement.setInt(2, compDone);
					statement.setInt(3, compWon);
					statement.setInt(4, compLost);
					statement.setInt(5, compDrawn);
					statement.setInt(6, charId);
				}
				statement.execute();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			_log.fatal("Olympiad System: Failed to save noblesse data to database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save olympiad.properties file with current olympiad status and update
	 * noblesse table in database
	 */
	public void saveOlympiadStatus()
	{
		saveNobleData();
		
		ServerData.getInstance().getData().set("Olympiad.CurrentCycle",_currentCycle);
		ServerData.getInstance().getData().set("Olympiad.Period",_period);
		ServerData.getInstance().getData().set("Olympiad.OlympiadEnd",_olympiadEnd);
		ServerData.getInstance().getData().set("Olympiad.ValdationEnd",_validationEnd);
		ServerData.getInstance().getData().set("Olympiad.NextWeeklyChange",_nextWeeklyChange);
	}

	protected void updateMonthlyData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
			statement.execute();
			statement.close();
			statement = con.prepareStatement(OLYMPIAD_MONTH_CREATE);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			 _log.fatal("Olympiad System: Failed to update monthly noblese data: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	protected void sortHerosToBe()
	{
		if (_period != 1)
			return;

		if (_nobles != null)
		{
			for (Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);

				if (nobleInfo == null)
					continue;

				int charId = nobleId;
				int classId = nobleInfo.getInteger(CLASS_ID);
				String charName = nobleInfo.getString(CHAR_NAME);
				int points = nobleInfo.getInteger(POINTS);
				int compDone = nobleInfo.getInteger(COMP_DONE);

				logResult(charName, "", Double.valueOf(charId), Double.valueOf(classId), compDone, points, "noble-charId-classId-compdone-points", 0, "");
			}
		}

		_heroesToBe = new FastList<StatsSet>();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			StatsSet hero;
			FastList<StatsSet> soulHounds = new FastList<StatsSet>();
			for (int i = 0; i < HERO_IDS.length; i++)
			{
				statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
				statement.setInt(1, HERO_IDS[i]);
				rset = statement.executeQuery();

				if (rset.next())
				{
					hero = new StatsSet();
					hero.set(CLASS_ID, HERO_IDS[i]);
					hero.set(CHAR_ID, rset.getInt(CHAR_ID));
					hero.set(CHAR_NAME, rset.getString(CHAR_NAME));

					if (HERO_IDS[i] == 132 || HERO_IDS[i] == 133) // Male & Female Soulhounds rank as one hero class
					{
						hero = _nobles.get(hero.getInteger(CHAR_ID));
						hero.set(CHAR_ID, rset.getInt(CHAR_ID));
						soulHounds.add(hero);
					}
					else
					{
						logResult(hero.getString(CHAR_NAME), "", hero.getDouble(CHAR_ID), hero.getDouble(CLASS_ID), 0, 0, "awarded hero", 0, "");
						_heroesToBe.add(hero);
					}
				}

				statement.close();
				rset.close();
			}
			switch (soulHounds.size())
			{
				case 0:
				{
					break;
				}
				case 1:
				{
					hero = new StatsSet();
					StatsSet winner = soulHounds.get(0);
					hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
					hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					logResult(hero.getString(CHAR_NAME), "", hero.getDouble(CHAR_ID), hero.getDouble(CLASS_ID), 0, 0, "awarded hero", 0, "");
					_heroesToBe.add(hero);
					break;
				}
				case 2:
				{
					hero = new StatsSet();
					StatsSet winner;
					StatsSet hero1 = soulHounds.get(0);
					StatsSet hero2 = soulHounds.get(1);
					int hero1Points = hero1.getInteger(POINTS);
					int hero2Points = hero2.getInteger(POINTS);
					int hero1Comps = hero1.getInteger(COMP_DONE);
					int hero2Comps = hero2.getInteger(COMP_DONE);

					if (hero1Points > hero2Points)
						winner = hero1;
					else if (hero2Points > hero1Points)
						winner = hero2;
					else
					{
						if (hero1Comps > hero2Comps)
							winner = hero1;
						else
							winner = hero2;
					}

					hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
					hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					logResult(hero.getString(CHAR_NAME), "", hero.getDouble(CHAR_ID), hero.getDouble(CLASS_ID), 0, 0, "awarded hero", 0, "");
					_heroesToBe.add(hero);
					break;
				}
			}
		}
		catch (SQLException e)
		{
			_log.warn("Olympiad System: Couldnt load heros from DB", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	public FastList<String> getClassLeaderBoard(int classId)
	{
		FastList<String> names = new FastList<String>();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			if (Config.ALT_OLY_SHOW_MONTHLY_WINNERS)
				statement = con.prepareStatement(GET_EACH_CLASS_LEADER);
			else
				statement = con.prepareStatement(GET_EACH_CLASS_LEADER_CURRENT);
			statement.setInt(1, classId);
			rset = statement.executeQuery();

			while (rset.next())
			{
				names.add(rset.getString(CHAR_NAME));
			}

			if (classId == 132) // Male & Female SoulHounds are ranked together
			{
				statement.setInt(1, 133);
				rset = statement.executeQuery();
				while (rset.next())
				{
					names.add(rset.getString(CHAR_NAME));
				}
			}

			statement.close();
			rset.close();

			return names;
		}
		catch (SQLException e)
		{
			  _log.warn("Olympiad System: Couldnt load olympiad leaders from DB", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return names;

	}

	protected void giveHeroBonus()
	{
		if (_heroesToBe.size() == 0)
			return;

		for (StatsSet hero : _heroesToBe)
		{
			int charId = hero.getInteger(CHAR_ID);

			StatsSet noble = _nobles.get(charId);
			int currentPoints = noble.getInteger(POINTS);
			currentPoints += Config.ALT_OLY_HERO_POINTS;
			noble.set(POINTS, currentPoints);

			updateNobleStats(charId, noble);
		}
	}

	public int getNoblessePasses(int objId)
	{
		if (_period != 1 || _nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(POINTS);
		if (points <= Config.ALT_OLY_MIN_POINT_FOR_EXCH)
			return 0;

		noble.set(POINTS, 0);
		updateNobleStats(objId, noble);

		points *= Config.ALT_OLY_GP_PER_POINT;

		return points;
	}

	public boolean isRegisteredInComp(L2PcInstance player)
	{
		boolean result = isRegistered(player);

		if (_inCompPeriod)
		{
			for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
			{
				if (game == null)
					continue;
				if ((game._playerOneID == player.getObjectId()) || (game._playerTwoID == player.getObjectId()))
				{
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public int getNoblePoints(int objId)
	{
		if (_nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(POINTS);

		return points;
	}

	public int getCompetitionDone(int objId)
	{
		if (_nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_DONE);

		return points;
	}

	public int getCompetitionWon(int objId)
	{
		if (_nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_WON);

		return points;
	}

	public int getCompetitionLost(int objId)
	{
		if (_nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_LOST);

		return points;
	}

	protected void deleteNobles()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			 _log.warn("Olympiad System: Couldnt delete nobles from DB", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		_nobles.clear();
	}

	/**
	 * Logs result of Olympiad to a csv file.
	 * 
	 * @param playerOne
	 * @param playerTwo
	 * @param p1hp
	 * @param p2hp
	 * @param p1dmg
	 * @param p2dmg
	 * @param result
	 * @param points
	 */
	public static synchronized void logResult(String playerOne, String playerTwo, Double p1hp, Double p2hp, int p1dmg, int p2dmg, String result, int points, String classed)
	{
		if (!Config.ALT_OLY_LOG_FIGHTS)
			return;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
		String date = formatter.format(new Date());
		FileWriter save = null;
		try
		{
			File file = new File("log/olympiad.csv");

			boolean writeHead = !file.exists();

			save = new FileWriter(file, true);

			if (writeHead)
			{
				String header = "Date,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Result,Points,Classed\r\n";
				save.write(header);
			}

			String out = date + "," + playerOne + "," + playerTwo + "," + p1hp + "," + p2hp + "," + p1dmg + "," + p2dmg + "," + result + "," + points + ","
					+ classed + "\r\n";
			save.write(out);
		}
		catch (IOException e)
		{
			 _log.warn("Olympiad System: Olympiad log could not be saved: ", e);
		}
		finally
		{
			try
			{
				save.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public static void sendMatchList(L2PcInstance player)
	{
		NpcHtmlMessage message = new NpcHtmlMessage(0);
		String replyMSG = HtmCache.getInstance().getHtm("data/html/olympiad/matchlist.htm", player);
		String rows = "";
		FastMap<Integer, String> matches = OlympiadManager.getInstance().getAllTitles();
		for (int i = 0; i < Olympiad.getStadiumCount(); i++)
		{
			int arenaID = i + 1;
			String players = "&nbsp;";
			String state = Message.getMessage(player, Message.MessageId.MSG_OLY_PREPARE);
			if (matches.containsKey(i))
				state = matches.get(i);
			rows+=("<tr><td fixwidth=30><a action=\"bypass -h OlympiadArenaChange " + i + "\">" + arenaID + "</a></td><td fixwidth=60>" + state + "</td><td>" + players + "</td></tr>");
		}
		
		message.setHtml(replyMSG);
		message.replace("%rows%", rows);
		player.sendPacket(message);
	}

	public static void bypassChangeArena(String command, L2PcInstance player)
	{
		String[] commands = command.split(" ");
		int id = Integer.parseInt(commands[1]);
		int arena = getSpectatorArena(player);
		if (arena >= 0)
			Olympiad.removeSpectator(arena, player);
		Olympiad.addSpectator(id, player, false);
	}
}