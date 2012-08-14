/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;



public final class L2DatabaseFactory extends AbstractDatabaseFactory
{
	private static final Logger _log = Logger.getLogger(L2DatabaseFactory.class);



	public static L2DatabaseFactory getInstance() throws SQLException
	{
		if (_instance == null)
			_instance = new L2DatabaseFactory();

		return (L2DatabaseFactory)_instance;
	}

	public static void close(Connection con)
	{
		if (con == null)
			return;

		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			_log.warn("L2DatabaseFactory: Failed to close database connection!", e);
		}
	}

	private final BasicDataSource _source;

	private L2DatabaseFactory() throws SQLException
	{
		try
		{
			if (Config.DATABASE_MAX_CONNECTIONS < 2)
			{
				Config.DATABASE_MAX_CONNECTIONS = 2;
				_log.warn("at least " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
			}

			_source = new BasicDataSource();
			_source.setDriverClassName(Config.DATABASE_DRIVER);
			_source.setUrl(Config.DATABASE_URL);
			_source.setUsername(Config.DATABASE_LOGIN);
		    _source.setPassword(Config.DATABASE_PASSWORD);

		    _source.setInitialSize(1);
		    _source.setMaxActive(Config.DATABASE_MAX_CONNECTIONS);
		    _source.setMaxIdle(Config.DATABASE_MAX_CONNECTIONS/3==0?1:Config.DATABASE_MAX_CONNECTIONS/3);
		    _source.setMinIdle(1);
		    _source.setMaxWait(-1L);

		    _source.setDefaultAutoCommit(true);
		    _source.setValidationQuery("SELECT 1");
		    _source.setTestOnBorrow(false);
		    _source.setTestWhileIdle(true);

		    _source.setRemoveAbandoned(true);
		    _source.setRemoveAbandonedTimeout(60);

		    _source.setTimeBetweenEvictionRunsMillis(600 * 1000);
		    _source.setNumTestsPerEvictionRun(Config.DATABASE_MAX_CONNECTIONS);
		    _source.setMinEvictableIdleTimeMillis(60 * 1000L);
		   
		    _log.info ("L2DatabaseFactory: Connected to database server");
		}
		catch (Exception e)
		{
			throw new SQLException("L2DatabaseFactory: Failed to init database connections: " + e, e);
		}
	}

	public void shutdown() throws Throwable
	{
		
		_source.close();
	}

	public String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
	{
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		if (returnOnlyTopRecord)
		{
				mySqlTop1 = " Limit 1 ";
		}

		return "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause 
	    	+ mySqlTop1; 

	}

	public String safetyString(String... whatToCheck)
	{
		// NOTE: Use brace as a safty percaution just incase name is a reserved word
		String braceLeft = "`";
		String braceRight = "`";

		String result = "";
		for (String word : whatToCheck)
		{
			if (!result.isEmpty())
				result += ", ";

			result += braceLeft + word + braceRight;
		}

		return result;
	}

	public Connection getConnection()
	{
		return getConnection(null);
	}

	public Connection getConnection(Connection con)
	{
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
			}
			catch (SQLException e)
			{
				_log.fatal("L2DatabaseFactory: Failed to retrieve database connection!", e);
			}
		}

		return con;
	}

	public int getBusyConnectionCount() throws SQLException
	{
		return _source.getNumActive();
	}

	public int getIdleConnectionCount() throws SQLException
	{
		return _source.getNumIdle();
	}

}