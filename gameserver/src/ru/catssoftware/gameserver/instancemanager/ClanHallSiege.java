package ru.catssoftware.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;

/*
*
* Author: MHard - L2CatsSoftware DevTeam
*
*/
public abstract class ClanHallSiege
{
	protected static Logger						_log	= Logger.getLogger(ClanHallSiege.class.getName());
	private Calendar							_siegeDate;
	public Calendar								_siegeEndDate;	
	private boolean								_isInProgress			= false;	

	public long restoreSiegeDate(int ClanHallId)
	{
		long res=0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT siege_data FROM clanhall_siege WHERE id=?");
			statement.setInt(1, ClanHallId);
			ResultSet rs = statement.executeQuery();

			if (rs.next())
				res = rs.getLong("siege_data");

			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: can't get clanhall siege date: " + e.getMessage(), e);
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
		return res;
	}
	public void setNewSiegeDate(long siegeDate, int ClanHallId ,int hour)
	{
		Calendar tmpDate=Calendar.getInstance();
		if (siegeDate<=System.currentTimeMillis())
		{
			tmpDate.setTimeInMillis(System.currentTimeMillis());
			tmpDate.add(Calendar.DAY_OF_MONTH, 3);
			tmpDate.set(Calendar.DAY_OF_WEEK, 6);
			tmpDate.set(Calendar.HOUR_OF_DAY, hour);
			tmpDate.set(Calendar.MINUTE, 0);
			tmpDate.set(Calendar.SECOND, 0);

			setSiegeDate(tmpDate);
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("UPDATE clanhall_siege SET siege_data=? WHERE id = ?");
				statement.setLong(1, getSiegeDate().getTimeInMillis());
				statement.setInt(2, ClanHallId);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.error("Exception: can't save clanhall siege date: " + e.getMessage(), e);
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
	public final Calendar getSiegeDate()
	{
		return _siegeDate;	
	}
	public final void setSiegeDate(Calendar par)
	{
		_siegeDate = par;
	}
	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}
	public final void setIsInProgress(boolean par)
	{
		_isInProgress = par;
	}
}