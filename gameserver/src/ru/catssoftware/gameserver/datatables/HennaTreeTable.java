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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.item.L2Henna;

public class HennaTreeTable
{
	private static final Logger					_log		= Logger.getLogger(HennaTreeTable.class);
	private static HennaTreeTable				_instance;

	private final Map<Integer, List<L2Henna>>	_hennaTrees	= new FastMap<Integer, List<L2Henna>>();

	public static HennaTreeTable getInstance()
	{
		if (_instance == null)
			_instance = new HennaTreeTable();

		return _instance;
	}

	private HennaTreeTable()
	{
		int classId = 0;
		int count = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT id FROM class_list");
			ResultSet classlist = statement.executeQuery();

			while (classlist.next())
			{
				classId = classlist.getInt("id");
				FastList<L2Henna> list = new FastList<L2Henna>();

				PreparedStatement statement2 = con.prepareStatement("SELECT symbol_id FROM henna_trees where class_id=?");
				statement2.setInt(1, classId);
				ResultSet hennatree = statement2.executeQuery();

				while (hennatree.next())
				{
					int id = hennatree.getInt("symbol_id");

					L2Henna template = HennaTable.getInstance().getTemplate(id);
					if (template == null)
					{
						hennatree.close();
						statement2.close();
						classlist.close();
						statement.close();
						return;
					}

					list.add(template);
				}
				hennatree.close();
				statement2.close();

				count += list.size();
				_hennaTrees.put(classId, list);
			}

			classlist.close();
			statement.close();
			_log.info("HennaTreeTable: Loaded " + count + " Henna Tree Templates.");
		}
		catch (Exception e)
		{
			_log.warn("Error while creating henna tree for classId " + classId + " " + e, e);
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

	public List<L2Henna> getAvailableHenna(L2PcInstance player)
	{
		return _hennaTrees.get(player.getClassId().getId());
	}
}