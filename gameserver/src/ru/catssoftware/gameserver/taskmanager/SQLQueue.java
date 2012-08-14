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
package ru.catssoftware.gameserver.taskmanager;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;


import javolution.util.FastList;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.sql.SQLQuery;


/**
 * @author DiezelMax, NB4L1
 */
public final class SQLQueue extends ExclusiveTask
{
	private static SQLQueue _instance;
	private static Logger _log = Logger.getLogger(SQLQueue.class);
	private FastList<SQLQuery> _query = new FastList<SQLQuery>();
	public static final SQLQueue getInstance()
	{
		if (_instance == null)
			_instance = new SQLQueue();

		return _instance;
	}

	private SQLQueue()
	{
		schedule(60000);
		_log.info("SQLQueue: started");
	}

	
	protected Connection getConnection() throws SQLException
	{
		return L2DatabaseFactory.getInstance().getConnection();
	}

	
	public synchronized void run() {
		flush();
	}
	private SQLQuery getNextQuery() {
		synchronized(_query) {
			if(_query.isEmpty())
				return null;
			return _query.removeFirst();
		}
	}
	
	private boolean _running = false;
	private void flush() {
		Connection con = null;
		if(_running)
			return;
		try {
			_running = true;
			con = getConnection();
				for(SQLQuery q; (q=getNextQuery())!=null;) try {
					q.execute(con);
				} catch(Exception e) {
					_log.warn("SQLQueue: Error executing "+q.getClass().getSimpleName(),e);
				}
		}
		catch(SQLException e) {}
		finally {
				if(con!=null ) try { con.close(); } catch(SQLException e) {}
				_running = false;
		}
		
	}
	@Override
	protected void onElapsed() {
		flush();
		schedule(60000);
	}
	public void add(SQLQuery q ) {
		synchronized(_query) {
			_query.add(q);
		}
	}
}