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

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;

public class SkillSpellbookTable
{
	private final static Logger					_log	= Logger.getLogger(SkillTreeTable.class.getName());
	private static SkillSpellbookTable			_instance;

	private static FastMap<Integer, Integer>	_skillSpellbooks;

	public static SkillSpellbookTable getInstance()
	{
		if (_instance == null)
			_instance = new SkillSpellbookTable();

		return _instance;
	}

	private SkillSpellbookTable()
	{
		_skillSpellbooks = new FastMap<Integer, Integer>();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT skill_id, item_id FROM skill_spellbooks");
			ResultSet spbooks = statement.executeQuery();

			while (spbooks.next())
				_skillSpellbooks.put(spbooks.getInt("skill_id"), spbooks.getInt("item_id"));

			spbooks.close();
			statement.close();

			_log.info("SkillSpellbookTable: Loaded " + _skillSpellbooks.size() + " Spellbooks.");
		}
		catch (Exception e)
		{
			_log.warn("Error while loading spellbook data: " + e);
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

	public int getBookForSkill(int skillId, int level)
	{
		if (skillId == 1405 && level != -1)
		{
			switch (level)
			{
			case 1:
				return 8618; // Ancient Book - Divine Inspiration (Modern Language Version)
			case 2:
				return 8619; // Ancient Book - Divine Inspiration (Original Language Version)
			case 3:
				return 8620; // Ancient Book - Divine Inspiration (Manuscript)
			case 4:
				return 8621; // Ancient Book - Divine Inspiration (Original Version)
			default:
				return -1;
			}
		}

		if (!_skillSpellbooks.containsKey(skillId))
			return -1;

		return _skillSpellbooks.get(skillId);
	}
}
