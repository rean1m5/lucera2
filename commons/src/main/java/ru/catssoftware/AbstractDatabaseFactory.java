package ru.catssoftware;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabaseFactory {
	protected static AbstractDatabaseFactory _instance;
	public static AbstractDatabaseFactory getInstance() throws SQLException {
		return _instance;
	}
	abstract public Connection getConnection();
	public static void close(Connection con) {
		
	}
}
