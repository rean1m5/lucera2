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
package ru.catssoftware.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.entity.faction.FactionQuest;

/**O
 * @author evill33t
 *
 */
public class FactionQuestManager
{
	protected static Logger				_log	= Logger.getLogger(FactionQuestManager.class.getName());

	// =========================================================
	private static FactionQuestManager	_instance;

	public static final FactionQuestManager getInstance()
	{
		if (_instance == null)
			_instance = new FactionQuestManager();
		return _instance;
	}

	// =========================================================

	// =========================================================
	// Data Field
	private FastList<FactionQuest>	_quests;

	// =========================================================
	// Constructor
	public FactionQuestManager()
	{
		load();
	}

	// =========================================================
	// Method - Public
	public final void reload()
	{
		getFactionQuests().clear();
		load();
	}

	// =========================================================
	// Method - Private
	private final void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("Select id, faction_id, name, description, reward, mobid, amount, min_level from faction_quests order by id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				getFactionQuests().add(new FactionQuest(rs.getInt("id"), rs.getInt("faction_id"), rs.getString("name"), rs.getString("description"), rs.getInt("reward"), rs.getInt("mobid"), rs.getInt("amount"), rs.getInt("min_level")));
			}

			statement.close();

			_log.info("Loaded: " + getFactionQuests().size() + " factionquests");
		}
		catch (Exception e)
		{
			_log.warn("Exception: FactionQuestManager.load(): " + e.getMessage(), e);
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

	// =========================================================
	// Property - Public
	public final FactionQuest getFactionQuest(int questId)
	{
		int index = getFactionQuestIndex(questId);
		if (index >= 0)
			return getFactionQuests().get(index);
		return null;
	}

	public final int getFactionQuestIndex(int questId)
	{
		FactionQuest quest;
		for (int i = 0; i < getFactionQuests().size(); i++)
		{
			quest = getFactionQuests().get(i);
			if (quest != null && quest.getId() == questId)
				return i;
		}
		return -1;
	}

	public final FastList<FactionQuest> getFactionQuests()
	{
		if (_quests == null)
			_quests = new FastList<FactionQuest>();
		return _quests;
	}
}
