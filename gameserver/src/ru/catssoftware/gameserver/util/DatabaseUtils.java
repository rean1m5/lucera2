package ru.catssoftware.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/*
 * @author Ro0TT
 * @date 1.4.2012
 */
public class DatabaseUtils
{
	/**
	 * Закрывает соединения типа: Connection, Statement, PreparedStatement, ResultSet
	 * @param objects
	 */
	public static void close(Object...objects)
	{
		for(Object object : objects)
			if (object!=null)
				try
				{
					if (object instanceof Connection)
						((Connection) object).close();
					else if (object instanceof Statement)
						((Statement) object).close();
					else if (object instanceof PreparedStatement)
						((PreparedStatement) object).close();
					else if (object instanceof ResultSet)
						((ResultSet) object).close();
					else
					{
						Thread.dumpStack();
						throw new IllegalArgumentException("Illegal close connection type " + object.toString());
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
	}
}