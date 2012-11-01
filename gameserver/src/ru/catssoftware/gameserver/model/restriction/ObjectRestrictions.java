package ru.catssoftware.gameserver.model.restriction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.Shutdown.ShutdownModeType;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.tools.random.Rnd;

/**
 * @author  Noctarius
 */
public class ObjectRestrictions
{
	// Restrictions SQL String Definitions:
	private static final String							RESTORE_RESTRICTIONS	= "SELECT obj_Id, type, delay, message FROM obj_restrictions";
	private static final String							DELETE_RESTRICTIONS		= "DELETE FROM obj_restrictions";
	private static final String							INSERT_RESTRICTIONS		= "INSERT INTO obj_restrictions (`obj_Id`, `type`, `delay`, `message`) VALUES (?, ?, ?, ?)";

	private static final Logger							_log					= Logger.getLogger(ObjectRestrictions.class.getName());
	private static ObjectRestrictions				_instance;

	private Map<Integer, List<AvailableRestriction>>	_restrictionList		= new HashMap<Integer, List<AvailableRestriction>>();
	private Map<Integer, List<PausedTimedEvent>>		_pausedActions			= new HashMap<Integer, List<PausedTimedEvent>>();
	private Map<Integer, List<TimedRestrictionAction>>	_runningActions			= new HashMap<Integer, List<TimedRestrictionAction>>();

	public static ObjectRestrictions getInstance()
	{
		if(_instance==null) {
			_instance = new ObjectRestrictions();
		}
		return _instance;
	}

	private ObjectRestrictions() {
		load();
	}
	private void load()
	{
		int i = 0;

		_log.info("ObjectRestrictions: loading...");
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(RESTORE_RESTRICTIONS);
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int objId = rset.getInt("obj_Id");
				AvailableRestriction type = AvailableRestriction.forName(rset.getString("type"));
				int delay = rset.getInt("delay");
				String message = rset.getString("message");

				switch (delay)
				{
					case -1:
						addRestriction(objId, type);
						break;

					default:
						timedRemoveRestriction(objId, type, delay, message);
						pauseTasks(objId);
				}
				i++;
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return;
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
		_log.info("ObjectRestrictions: loaded " + i + " restrictions.");
	}

	public void shutdown()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(DELETE_RESTRICTIONS);
			statement.execute();
			statement.close();
			for (int id : _restrictionList.keySet())
			{
				for (AvailableRestriction restriction : _restrictionList.get(id))
				{
					statement = con.prepareStatement(INSERT_RESTRICTIONS);

					statement.setInt(1, id);
					statement.setString(2, restriction.name());
					statement.setLong(3, -1);
					statement.setString(4, "");
					statement.execute();
					statement.close();
				}
			}
			// Store paused restriction events
			for (int id : _pausedActions.keySet())
			{
				for (PausedTimedEvent paused : _pausedActions.get(id))
				{
					statement = con.prepareStatement(INSERT_RESTRICTIONS);

					statement.setInt(1, id);
					statement.setString(2, paused.getAction().getRestriction().name());
					statement.setLong(3, paused.getBalancedTime());
					statement.setString(4, paused.getAction().getMessage());

					statement.execute();
					statement.close();
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
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

	private void addRestriction(int objId, AvailableRestriction restriction)
	{
		if (_restrictionList.get(objId) == null)
			_restrictionList.put(objId, new ArrayList<AvailableRestriction>());

		if (!_restrictionList.get(objId).contains(restriction))
			_restrictionList.get(objId).add(restriction);
	}

	public void addRestriction(Object owner, AvailableRestriction restriction) throws RestrictionBindClassException
	{
		if (owner == null)
			return;

		if (!checkApplyable(owner, restriction))
			throw new RestrictionBindClassException("Restriction " + restriction.name() + " cannot bound to Class " + owner.getClass());

		int id = getObjectId(owner);
		addRestriction(id, restriction);
	}
	public void addGlobalRestriction(AvailableRestriction restriction)
	{
		addRestriction(0, restriction);
	}

	public void removeRestriction(Object owner, AvailableRestriction restriction)
	{
		if (owner == null)
			return;

		int id = -1;
		if (owner instanceof L2Object)
			id = ((L2Object) owner).getObjectId();
		else
			id = owner.hashCode();

		if (_restrictionList.get(id) != null && _restrictionList.get(id).contains(restriction))
		{
			_restrictionList.get(id).remove(restriction);
			if (_runningActions.get(id) != null)
			{
				for (TimedRestrictionAction action : _runningActions.get(id))
				{
					if (action.getRestriction() == restriction)
					{
						action.getTask().cancel(true);
						_runningActions.get(id).remove(action);
						break;
					}
				}
			}
			if (_pausedActions.get(id) != null)
			{
				for (PausedTimedEvent paused : _pausedActions.get(id))
				{
					if (paused.getAction().getRestriction() == restriction)
						_pausedActions.get(id).remove(paused);
				}
			}
		}
	}
	public void removeGlobalRestriction(AvailableRestriction restriction)
	{
		int id = 0;
		if (_restrictionList.get(id) != null && _restrictionList.get(id).contains(restriction))
		{
			_restrictionList.get(id).remove(restriction);
			if (_runningActions.get(id) != null)
			{
				for (TimedRestrictionAction action : _runningActions.get(id))
				{
					if (action.getRestriction() == restriction)
					{
						action.getTask().cancel(true);
						_runningActions.get(id).remove(action);
						break;
					}
				}
			}
			if (_pausedActions.get(id) != null)
			{
				for (PausedTimedEvent paused : _pausedActions.get(id))
				{
					if (paused.getAction().getRestriction() == restriction)
						_pausedActions.get(id).remove(paused);
				}
			}
		}
	}

	public void addRestrictionList(Object owner, List<AvailableRestriction> restrictions) throws RestrictionBindClassException
	{
		if (owner == null)
			return;

		int id = getObjectId(owner);

		if (_restrictionList.get(id) == null)
			_restrictionList.put(id, new ArrayList<AvailableRestriction>());

		for (AvailableRestriction restriction : restrictions)
		{
			if (!checkApplyable(owner, restriction))
				throw new RestrictionBindClassException("Restriction " + restriction.name() + " cannot bound to Class " + owner.getClass());
		}

		_restrictionList.get(id).addAll(restrictions);
	}

	public boolean checkRestriction(Object owner, AvailableRestriction restriction)
	{
		if (owner == null)
			return false;

//		if(_restrictionCleanup!=null)
//			_restrictionCleanup.start();
		int id = getObjectId(owner);

		if (_restrictionList.get(id) == null)
			return false;

		return _restrictionList.get(id).contains(restriction);
	}

	public boolean checkGlobalRestriction(AvailableRestriction restriction)
	{
		if (restriction!=AvailableRestriction.GlobalPlayerChat)
			return false;
		int id = 0; //this newer used ID, use for global restriction
		if (_restrictionList.get(id) == null)
			return false;
		return _restrictionList.get(id).contains(restriction);
	}

	public void timedRemoveRestriction(int objId, AvailableRestriction restriction, long delay)
	{
		TimedRestrictionEvent event = new TimedRestrictionEvent(objId, restriction, TimedRestrictionEventType.Remove, delay);

		ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(event, delay);

		event.getActionObject().setTask(task);
		addTask(objId, event.getActionObject());
	}

	public void timedRemoveRestriction(int objId, AvailableRestriction restriction, long delay, String message)
	{
		TimedRestrictionEvent event = new TimedRestrictionEvent(objId, restriction, TimedRestrictionEventType.Remove, delay, message);

		ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(event, delay);

		event.getActionObject().setTask(task);
		addTask(objId, event.getActionObject());
	}

	public void timedAddRestriction(L2Object owner, AvailableRestriction restriction, long delay) throws RestrictionBindClassException
	{
		if (!checkApplyable(owner.getObjectId(), restriction))
			throw new RestrictionBindClassException();

		timedAddRestriction(owner.getObjectId(), restriction, delay);
	}

	private void timedAddRestriction(int objId, AvailableRestriction restriction, long delay)
	{
		TimedRestrictionEvent event = new TimedRestrictionEvent(objId, restriction, TimedRestrictionEventType.Add, delay);

		ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(event, delay);

		event.getActionObject().setTask(task);
		addTask(objId, event.getActionObject());
	}

	public void timedAddRestriction(L2Object owner, AvailableRestriction restriction, long delay, String message) throws RestrictionBindClassException
	{
		if (!checkApplyable(owner, restriction))
			throw new RestrictionBindClassException();

		timedAddRestriction(owner.getObjectId(), restriction, delay, message);
	}

	private void timedAddRestriction(int objId, AvailableRestriction restriction, long delay, String message)
	{
		TimedRestrictionEvent event = new TimedRestrictionEvent(objId, restriction, TimedRestrictionEventType.Add, delay, message);

		ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneral(event, delay);

		event.getActionObject().setTask(task);
		addTask(objId, event.getActionObject());
	}

	private void addTask(int objId, TimedRestrictionAction action)
	{
		if (_runningActions.get(objId) == null)
			_runningActions.put(objId, new ArrayList<TimedRestrictionAction>());

		if (!_runningActions.get(objId).contains(action))
			_runningActions.get(objId).add(action);
	}

	private void addPausedTask(int objId, PausedTimedEvent action)
	{
		if (_pausedActions.get(objId) == null)
			_pausedActions.put(objId, new ArrayList<PausedTimedEvent>());

		if (!_pausedActions.get(objId).contains(action))
			_pausedActions.get(objId).add(action);
	}

	public boolean containsPausedTask(int objId)
	{
		return (_pausedActions.get(objId) != null && !_pausedActions.get(objId).isEmpty());
	}

	public boolean containsRunningTask(int objId)
	{
		return (_runningActions.get(objId) != null && !_runningActions.get(objId).isEmpty());
	}

	public void pauseTasks(int objId)
	{
		if (!containsRunningTask(objId))
			return;

		for (TimedRestrictionAction action : _runningActions.get(objId))
		{
			// Cancel active task
			if(action.getTask()!=null)
				action.getTask().cancel(true);

			// Save PausedEventObject
			PausedTimedEvent paused = new PausedTimedEvent(action, action.getBalancedTime());
			addPausedTask(objId, paused);
		}
		_runningActions.get(objId).clear();
	}

	public void resumeTasks(int objId)
	{
		if (!containsPausedTask(objId))
			return;

		for (PausedTimedEvent paused : _pausedActions.get(objId))
		{
			switch (paused.getAction().getEventType())
			{
			case Add:
				timedAddRestriction(objId, paused.getAction().getRestriction(), paused.getBalancedTime(), paused.getAction().getMessage());
				break;

			case Remove:
				timedRemoveRestriction(objId, paused.getAction().getRestriction(), paused.getBalancedTime(), paused.getAction().getMessage());
			}
		}

		// Clear up running tasks
		_pausedActions.get(objId).clear();
	}

	private class TimedRestrictionEvent implements Runnable
	{
		private final TimedRestrictionAction	_action;

		public TimedRestrictionEvent(int objId, AvailableRestriction restriction, TimedRestrictionEventType type, long delay)
		{
			_action = new TimedRestrictionAction(objId, restriction, type, null, delay);
		}

		public TimedRestrictionEvent(int objId, AvailableRestriction restriction, TimedRestrictionEventType type, long delay, String message)
		{
			_action = new TimedRestrictionAction(objId, restriction, type, message, delay);
		}

		public void run()
		{
			switch (_action.getEventType())
			{
			case Add:
				addRestriction(_action.getObjectId(), _action.getRestriction());
				break;

			case Remove:
				removeRestriction(_action.getObjectId(), _action.getRestriction());
			}

			if (_action.getMessage() != null)
			{
				L2Object owner = L2World.getInstance().findObject(_action.getObjectId());

				if (owner.isPlayer())
					((L2PcInstance) owner).sendMessage(_action.getMessage());
				else if (owner instanceof L2Summon)
				{
					L2Summon summon = (L2Summon) owner;
					summon.getOwner().sendMessage(_action.getMessage());
				}
			}
		}

		public TimedRestrictionAction getActionObject()
		{
			return _action;
		}
	}

	private enum TimedRestrictionEventType
	{
		Remove, Add
	}

	private class TimedRestrictionAction
	{
		private final int						_objId;
		private final AvailableRestriction		_restriction;
		private final TimedRestrictionEventType	_type;
		private final String					_message;
		private final Long						_delay;
		private Long							_starttime;
		private ScheduledFuture<?>				_task;

		public TimedRestrictionAction(int objId, AvailableRestriction restriction, TimedRestrictionEventType type, String message, long delay)
		{
			_objId = objId;
			_restriction = restriction;
			_type = type;
			_message = message;
			_delay = delay;

			_starttime = System.currentTimeMillis();
		}

		public int getObjectId()
		{
			return _objId;
		}

		public AvailableRestriction getRestriction()
		{
			return _restriction;
		}

		public TimedRestrictionEventType getEventType()
		{
			return _type;
		}

		public String getMessage()
		{
			return _message;
		}

		public Long getDelay()
		{
			return _delay;
		}

		public Long getBalancedTime()
		{
			return (_delay - (System.currentTimeMillis() - _starttime));
		}

		public ScheduledFuture<?> getTask()
		{
			return _task;
		}

		public void setTask(ScheduledFuture<?> task)
		{
			_task = task;
		}
	}

	private class PausedTimedEvent
	{
		private final Long						_balancedTime;
		private final TimedRestrictionAction	_action;

		public PausedTimedEvent(TimedRestrictionAction action, long balancedTime)
		{
			_action = action;
			_balancedTime = balancedTime;
		}

		public Long getBalancedTime()
		{
			return _balancedTime;
		}

		public TimedRestrictionAction getAction()
		{
			return _action;
		}
	}

	private boolean checkApplyable(Object owner, AvailableRestriction restriction)
	{
		return (restriction.getApplyableTo().isInstance(owner));
	}

	private int getObjectId(Object owner)
	{
		if (owner instanceof L2Object)
			return ((L2Object) owner).getObjectId();

		return owner.hashCode();
	}
}