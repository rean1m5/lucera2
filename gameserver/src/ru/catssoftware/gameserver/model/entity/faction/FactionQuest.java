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
package ru.catssoftware.gameserver.model.entity.faction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author evill33t
 *
 */
public class FactionQuest
{
	protected static Logger	_log	= Logger.getLogger(FactionQuest.class.getName());

	private final int		_questId;
	private static int		_factionId;
	private static String	_name;
	private static String	_descr;
	private static int		_reward;
	private static int		_mobId;
	private static int		_amount;
	private static int		_minLevel;

	public FactionQuest(int questId, int factionId, String name, String descr, int reward, int mobId, int amount, int minLevel)
	{
		_questId = questId;
		_factionId = factionId;
		_name = name;
		_descr = descr;
		_reward = reward;
		_mobId = mobId;
		_amount = amount;
		_minLevel = minLevel;
	}

	public int getId()
	{
		return _questId;
	}

	public static String getName()
	{
		return _name;
	}

	public static String getDescr()
	{
		return _descr;
	}

	public static int getReward()
	{
		return _reward;
	}

	public static int getAmount()
	{
		return _amount;
	}

	public static int getMobId()
	{
		return _mobId;
	}

	public static int getFactionId()
	{
		return _factionId;
	}

	public static int getMinLevel()
	{
		return _minLevel;
	}

	public static void createFactionQuest(L2PcInstance player, int factionQuestId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO character_faction_quests (char_id,faction_quest_id) VALUES (?,?)");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, factionQuestId);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("could not insert char faction quest:", e);
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

	public static void endFactionQuest(L2PcInstance player, int factionQuestId)
	{
		player.sendMessage(getName() + " completed.");
		player.getNPCFaction().addFactionPoints(getReward() * Config.FACTION_QUEST_RATE);
		deleteFactionQuest(player, factionQuestId);
	}

	public static void deleteFactionQuest(L2PcInstance player, int factionQuestId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_faction_quests WHERE char_id=? AND faction_quest_id=?");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, factionQuestId);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("could not delete char faction quest:", e);
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
