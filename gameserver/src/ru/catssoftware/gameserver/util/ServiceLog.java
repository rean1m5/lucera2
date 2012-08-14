package ru.catssoftware.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/*
 * Author: M-095, L2CatsSoftware Dev Team
 */

public class ServiceLog
{
	private static Logger				_log				= Logger.getLogger(ServiceLog.class.getName());
	private static String			_charName			= null;
	private static int				_charId				= 0;

	public static void ServiceAudit(L2PcInstance player, String serviceName, String message)
	{
		if (player == null)
			return;

		_charName = player.getName();
		_charId = player.getObjectId();
		storeInDB(serviceName, _charName, _charId, message);
	}

	private static void storeInDB(String serviceName, String charName, int charId, String message)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			statement = con.prepareStatement("INSERT INTO service_log(serviceName, charName, charId, message, date) VALUES(?,?,?,?,now())");
			statement.setString(1, serviceName);
			statement.setString(2, charName);
			statement.setInt(3, charId);
			statement.setString(4, message.length() > 50 ? message.substring(0, 49) : message);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.fatal("Could not store Service Action:", e);
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