package ru.catssoftware.gameserver.taskmanager;

import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_FIXED_SHEDULED;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_GLOBAL_TASK;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_NONE;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_SHEDULED;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_SPECIAL;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_STARTUP;
import static ru.catssoftware.gameserver.taskmanager.TaskTypes.TYPE_TIME;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.taskmanager.tasks.*;




/**
 * @author Layane
 */
public final class TaskManager
{
	protected static final Logger				_log			= Logger.getLogger(TaskManager.class.getName());
	private static TaskManager				_instance;

	protected static final String[]			SQL_STATEMENTS	=
	{
			"SELECT id,task,type,last_activation,param1,param2,param3 FROM global_tasks",
			"UPDATE global_tasks SET last_activation=? WHERE id=?",
			"SELECT id FROM global_tasks WHERE task=?",
			"INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)"
	};

	private final FastMap<Integer, Task>	_tasks			= new FastMap<Integer, Task>();
	protected final FastList<ExecutedTask>	_currentTasks	= new FastList<ExecutedTask>();

	public class ExecutedTask implements Runnable
	{
		int					id;
		long				lastActivation;
		Task				task;
		TaskTypes			type;
		String[]			params;
		ScheduledFuture<?>	scheduled;

		public ExecutedTask(Task pTask, TaskTypes pType, ResultSet rset) throws SQLException
		{
			task = pTask;
			type = pType;
			id = rset.getInt("id");
			lastActivation = rset.getLong("last_activation");
			params = new String[]
			{ rset.getString("param1"), rset.getString("param2"), rset.getString("param3") };
		}

		public void run()
		{
			task.onTimeElapsed(this);
			lastActivation = System.currentTimeMillis();

			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[1]);
				statement.setLong(1, lastActivation);
				statement.setInt(2, id);
				statement.executeUpdate();
				statement.close();
			}
			catch (SQLException e)
			{
				_log.warn("cannot updated the Global Task " + id + ": " + e.getMessage());
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

			if (type == TYPE_SHEDULED || type == TYPE_TIME)
				stopTask();
		}

		@Override
		public boolean equals(Object object)
		{
			return id == ((ExecutedTask) object).id;
		}

		public Task getTask()
		{
			return task;
		}

		public TaskTypes getType()
		{
			return type;
		}

		public int getId()
		{
			return id;
		}

		public String[] getParams()
		{
			return params;
		}

		public long getLastActivation()
		{
			return lastActivation;
		}

		public void stopTask()
		{
			task.onDestroy();

			if (scheduled != null)
				scheduled.cancel(true);

			_currentTasks.remove(this);
		}

	}

	public static TaskManager getInstance()
	{
		if (_instance == null)
			_instance = new TaskManager();
		return _instance;
	}

	public TaskManager()
	{
		_log.info("TaskManager: initalized.");
		initializate();
	}

	private void initializate()
	{
		registerTask(new TaskCleanUp());
		registerTask(new TaskOlympiadSave());
		registerTask(new TaskRaidPointsReset());
		registerTask(new TaskRecom());
		registerTask(new TaskRestart());
		registerTask(new TaskShutdown());

		_log.info("TaskManager: Registered: " + _tasks.size() + " Tasks.");
	}

	public void registerTask(Task task)
	{
		int key = task.getName().trim().toLowerCase().hashCode();
		if (!_tasks.containsKey(key))
		{
			_tasks.put(key, task);
			task.initializate();
		}
	}

	public void startAllTasks()
	{
		Connection con = null;
		try
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[0]);
				ResultSet rset = statement.executeQuery();

				while (rset.next())
				{
					Task task = _tasks.get(rset.getString("task").trim().toLowerCase().hashCode());

					if (task == null)
						continue;
					TaskTypes type = TaskTypes.valueOf(rset.getString("type"));
					if (type != TYPE_NONE)
					{
						ExecutedTask current = new ExecutedTask(task, type, rset);
						if (launchTask(current))
							_currentTasks.add(current);
					}
				}
				_log.info("TaskManager: Loaded: " + _currentTasks.size() + " Tasks From Database.");

				rset.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.fatal("error while loading Global Task table " + e, e);
			}
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

	private boolean launchTask(ExecutedTask task)
	{
		final ThreadPoolManager scheduler = ThreadPoolManager.getInstance();
		final TaskTypes type = task.getType();

		if (type == TYPE_STARTUP)
		{
			task.run();
			return false;
		}
		else if (type == TYPE_SHEDULED)
		{
			long delay = Long.valueOf(task.getParams()[0]);
			task.scheduled = scheduler.scheduleGeneral(task, delay);
			return true;
		}
		else if (type == TYPE_FIXED_SHEDULED)
		{
			long delay = Long.valueOf(task.getParams()[0]);
			long interval = Long.valueOf(task.getParams()[1]);
			task.scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
			return true;
		}
		else if (type == TYPE_TIME)
		{
			try
			{
				Date desired = DateFormat.getInstance().parse(task.getParams()[0]);
				long diff = desired.getTime() - System.currentTimeMillis();
				if (diff >= 0)
				{
					task.scheduled = scheduler.scheduleGeneral(task, diff);
					return true;
				}
				_log.info("Task " + task.getId() + " is obsoleted.");
			}
			catch (Exception e)
			{
			}
		}
		else if (type == TYPE_SPECIAL)
		{
			ScheduledFuture<?> result = task.getTask().launchSpecial(task);
			if (result != null)
			{
				task.scheduled = result;
				return true;
			}
		}
		else if (type == TYPE_GLOBAL_TASK)
		{
			long interval = Long.valueOf(task.getParams()[0]) * 86400000L;
			String[] hour = task.getParams()[1].split(":");

			if (hour.length != 3)
			{
				_log.warn("Task " + task.getId() + " has incorrect parameters");
				return false;
			}

			Calendar check = Calendar.getInstance();
			check.setTimeInMillis(task.getLastActivation() + interval);			
			try
			{
				check.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour[0]));
				check.set(Calendar.MINUTE, Integer.valueOf(hour[1]));
				check.set(Calendar.SECOND, Integer.valueOf(hour[2]));
			}
			catch (Exception e)
			{
				_log.warn("Bad parameter on task " + task.getId() + ": " + e.getMessage());
				return false;
			}
			 while (check.before(Calendar.getInstance()))
				 check.add(Calendar.DAY_OF_YEAR, 1);
			long delay = check.getTimeInMillis() - System.currentTimeMillis();			 

			task.scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
			return true;
		}
		return false;
	}

	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3)
	{
		return addUniqueTask(task, type, param1, param2, param3, 0);
	}

	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[2]);
			statement.setString(1, task);
			ResultSet rset = statement.executeQuery();

			if (!rset.next())
			{
				statement = con.prepareStatement(SQL_STATEMENTS[3]);
				statement.setString(1, task);
				statement.setString(2, type.toString());
				statement.setLong(3, lastActivation);
				statement.setString(4, param1);
				statement.setString(5, param2);
				statement.setString(6, param3);
				statement.execute();
			}

			rset.close();
			statement.close();
			return true;
		}
		catch (SQLException e)
		{
			_log.warn("cannot add the unique task: " + e.getMessage());
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

		return false;
	}

	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3)
	{
		return addTask(task, type, param1, param2, param3, 0);
	}

	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[3]);
			statement.setString(1, task);
			statement.setString(2, type.toString());
			statement.setLong(3, lastActivation);
			statement.setString(4, param1);
			statement.setString(5, param2);
			statement.setString(6, param3);
			statement.execute();

			statement.close();
			return true;
		}
		catch (SQLException e)
		{
			_log.warn("cannot add the task:  " + e.getMessage());
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
		return false;
	}
}