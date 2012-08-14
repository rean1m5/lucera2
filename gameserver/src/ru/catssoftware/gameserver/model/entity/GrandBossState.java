package ru.catssoftware.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.catssoftware.L2DatabaseFactory;


public class GrandBossState
{
	public static enum StateEnum
	{
		NOTSPAWN,
		ALIVE,
		DEAD,
		INTERVAL,
		SLEEP,
		UNKNOWN
	}

	private int					_bossId;
	private long				_respawnDate;
	private StateEnum			_state;

	public int getBossId()
	{
		return _bossId;
	}

	public void setBossId(int newId)
	{
		_bossId = newId;
	}

	public StateEnum getState()
	{
		if(_state==null)
			_state = StateEnum.UNKNOWN;
		return _state;
	}

	public void setState(StateEnum newState)
	{
		_state = newState;
		if(_bossId!=0)
			update();
	}

	public long getRespawnDate()
	{
		return _respawnDate;
	}

	public void setRespawnDate(long interval)
	{
		_respawnDate = interval + System.currentTimeMillis();
	}

	public GrandBossState()
	{
	}

	public GrandBossState(int bossId)
	{
		_bossId = bossId;
		load();
	}

	public GrandBossState(int bossId, boolean isDoLoad)
	{
		_bossId = bossId;
		if (isDoLoad)
			load();
	}

	public void load()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			PreparedStatement statement = con.prepareStatement("SELECT * FROM grandboss_intervallist WHERE bossId = ?");
			statement.setInt(1, _bossId);
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				_respawnDate = rset.getLong("respawnDate");
				_state = StateEnum.values()[rset.getInt("state")];
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
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

	public void save()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("INSERT INTO grandboss_intervallist (bossId,respawnDate,state) VALUES(?,?,?)");
			statement.setInt(1, _bossId);
			statement.setLong(2, _respawnDate);
			statement.setInt(3, _state.ordinal());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
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

	public void update()
	{
		Connection con = null;
		boolean needInsert = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE grandboss_intervallist SET respawnDate = ?,state = ? WHERE bossId = ?");
			statement.setLong(1, _respawnDate);
			statement.setInt(2, _state.ordinal());
			statement.setInt(3, _bossId);
			needInsert = statement.executeUpdate()==0;
			statement.close();
		}
		catch (Exception e)
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
		if(needInsert)
			save();
	}

	public void setNextRespawnDate(long newRespawnDate)
	{
		_respawnDate = newRespawnDate;
	}

	public long getInterval()
	{
		long interval = _respawnDate - System.currentTimeMillis();

		if (interval < 0)
			return 0;

		return interval;
	}
}