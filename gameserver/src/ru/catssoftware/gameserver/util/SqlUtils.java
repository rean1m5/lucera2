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
package ru.catssoftware.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;

public class SqlUtils
{
	private final static Logger	_log	= Logger.getLogger(SqlUtils.class.getName());

	// =========================================================
	// Data Field
	private static SqlUtils		_instance;

	// =========================================================
	// Property - Public
	public static SqlUtils getInstance()
	{
		if (_instance == null)
			_instance = new SqlUtils();
		return _instance;
	}

	public static Integer[][] get2DIntArray(String[] resultFields, String usedTables, String whereClause)
	{
		long start = System.currentTimeMillis();

		String query = "";

		Integer res[][] = null;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			query = L2DatabaseFactory.getInstance().prepQuerySelect(resultFields, usedTables, whereClause, false);
			PreparedStatement statement = con.prepareStatement(query);
			ResultSet rset = statement.executeQuery();

			int rows = 0;
			while (rset.next())
				rows++;

			res = new Integer[rows - 1][resultFields.length];

			rset.first();

			int row = 0;
			while (rset.next())
			{
				for (int i = 0; i < resultFields.length; i++)
					res[row][i] = rset.getInt(i + 1);
				row++;
			}
		}
		catch (Exception e)
		{
			_log.warn("Error in query '" + query + "':" + e, e);
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

		_log.debug("Get all rows in query '" + query + "' in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
}