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
package ru.catssoftware.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;


/**
 *
 * @author  Visor
 */

public class L2SubClassSkillsInfo
{
	private static final String SELECT_SUBCLASS_SKILLS_INFO = 
		"select * from character_subclass_certification where characterId=? and subIndex=?"; 
	private static final String UPDATE_SUBCLASS_SKILLS_INFO = 
		"update character_subclass_certification set cert_65=?, cert_70=?, cert_75=?, cert_80=? where characterId=? and subIndex=?";
	private static final String ADD_SUBCLASS_SKILLS_INFO = 
		"insert into character_subclass_certification (cert_65, cert_70, cert_75, cert_80, characterId, subIndex) values (?,?,?,?,?,?)";
	private static final String CLEAR_SUBCLASS_SKILLS_INFO = 
		"update character_subclass_certification set cert_65=false, cert_70=false, cert_75=false, cert_80=false where characterId=?";
	
	public boolean cert_65 = false;
	public boolean cert_70 = false;
	public boolean cert_75 = false;
	public boolean cert_80 = false;

	private static final Logger _log = Logger.getLogger(L2SubClassSkillsInfo.class.getName());
	
	public static L2SubClassSkillsInfo LoadFromDatabase(L2PcInstance player)
	{
		L2SubClassSkillsInfo result = new L2SubClassSkillsInfo();
		Connection connection = null;
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			if (connection != null)
			{
				PreparedStatement statement = connection.prepareStatement(SELECT_SUBCLASS_SKILLS_INFO);
				statement.setInt(1, player.getObjectId());
				statement.setInt(2, player.getClassIndex());
				ResultSet resultSet = statement.executeQuery();
				if (resultSet.next())
				{
					result.cert_65 = resultSet.getBoolean("cert_65");
					result.cert_70 = resultSet.getBoolean("cert_70");
					result.cert_75 = resultSet.getBoolean("cert_75");
					result.cert_80 = resultSet.getBoolean("cert_80");
				}
				else
					InsertToDatabase(player, result);
				statement.close();
			}
		}
		catch (SQLException e)
		{
			if(_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while getting connection to the database", e);
		}
		finally
		{
			try
			{
				if (connection != null)
					connection.close();
			}
			catch (SQLException e)
			{
				if(_log.isLoggable(Level.SEVERE))
					_log.log(Level.SEVERE, "Internal server error during connection closing", e);
			}
		}
		return result;
	}
	
	public static void ClearSubClassSkillsInfo(L2PcInstance player)
	{
		Connection connection = null;
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			if (connection != null)
			{
				PreparedStatement statement = connection.prepareStatement(CLEAR_SUBCLASS_SKILLS_INFO);
				statement.setInt(1, player.getObjectId());
				statement.execute();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			if(_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while getting connection to the database", e);
		}
		finally
		{
			try
			{
				if (connection != null)
					connection.close();
			}
			catch (SQLException e)
			{
				if(_log.isLoggable(Level.SEVERE))
					_log.log(Level.SEVERE, "Internal server error during connection closing", e);
			}
		}
	}
	
	public static void InsertToDatabase(L2PcInstance player, L2SubClassSkillsInfo info)
	{
		AddUpdateSkillsInfo(player, info, ADD_SUBCLASS_SKILLS_INFO);
	}
	
	public static void UpdateSkillsInfoForPlayer(L2PcInstance player, L2SubClassSkillsInfo info)
	{
		AddUpdateSkillsInfo(player, info, UPDATE_SUBCLASS_SKILLS_INFO);
	}
	
	private static void AddUpdateSkillsInfo(L2PcInstance player, L2SubClassSkillsInfo info, String sqlQuery)
	{
		Connection connection = null;
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			if (connection != null)
			{
				PreparedStatement statement = connection.prepareStatement(sqlQuery);
				statement.setBoolean(1, info.cert_65);
				statement.setBoolean(2, info.cert_70);
				statement.setBoolean(3, info.cert_75);
				statement.setBoolean(4, info.cert_80);
				statement.setInt(5, player.getObjectId());
				statement.setInt(6, player.getClassIndex());
				statement.execute();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			if(_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while getting connection to the database", e);
		}
		finally
		{
			try
			{
				if (connection != null)
					connection.close();
			}
			catch (SQLException e)
			{
				if(_log.isLoggable(Level.SEVERE))
					_log.log(Level.SEVERE, "Internal server error during connection closing", e);
			}
		}
	}
}