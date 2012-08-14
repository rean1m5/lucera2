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
package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;

public class PetNameTable
{
	private final static Logger	_log	= Logger.getLogger(PetNameTable.class.getName());

	private static PetNameTable	_instance;

	public static PetNameTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new PetNameTable();
		}
		return _instance;
	}

	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id=?");
			statement.setString(1, name);
			statement.setString(2, Integer.toString(PetDataTable.getItemIdByPetId(petNpcId)));
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("could not check existing petname:" + e.getMessage());
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

		return result;
	}
}