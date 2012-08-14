package ru.catssoftware.sql;

import java.sql.Connection;

public interface SQLQuery {
	public void execute(Connection con);
}
