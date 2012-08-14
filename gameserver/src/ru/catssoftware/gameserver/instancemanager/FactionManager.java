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
import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.entity.faction.Faction;

/**
 * @author evill33t
 *
 */
public class FactionManager
{
	private static final Logger		_log	= Logger.getLogger(FactionManager.class.getName());

	// =========================================================
	private static FactionManager	_instance;

	public static final FactionManager getInstance()
	{
		if (_instance == null)
			_instance = new FactionManager();
		return _instance;
	}

	private FactionManager()
	{
		_log.info("Initializing FactionManager");
		load();		
	}
	// =========================================================

	// =========================================================
	// Data Field
	private FastList<Faction>	_factions;
	private FastList<String>	_listTitles	= new FastList<String>();

	// =========================================================
	// Method - Public
	public void reload()
	{
		getFactions().clear();
		getFactionTitles().clear();
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

			statement = con.prepareStatement("Select id from factions order by id");
			rs = statement.executeQuery();

			while (rs.next())
			{
				Faction faction = new Faction(rs.getInt("id"));
				getFactions().add(faction);
				for (FastMap.Entry<Integer, String> e = faction.getTitle().head(), end = faction.getTitle().tail(); (e = e.getNext()) != end;)
					_listTitles.add(e.getValue().toLowerCase());
				faction = null;
			}

			statement.close();

			_log.info("Loaded: " + getFactions().size() + " faction(s)");
		}
		catch (Exception e)
		{
			_log.warn("Exception: FactionsManager.load(): " + e.getMessage(), e);
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
	public final Faction getFactions(int FactionId)
	{
		int index = getFactionIndex(FactionId);
		if (index >= 0)
			return getFactions().get(index);
		return null;
	}

	public final int getFactionIndex(int FactionId)
	{
		Faction faction;
		for (int i = 0; i < getFactions().size(); i++)
		{
			faction = getFactions().get(i);
			if (faction != null && faction.getId() == FactionId)
				return i;
		}
		return -1;
	}

	public final FastList<Faction> getFactions()
	{
		if (_factions == null)
			_factions = new FastList<Faction>();
		return _factions;
	}

	public final FastList<String> getFactionTitles()
	{
		if (_listTitles == null)
			_listTitles = new FastList<String>();
		return _listTitles;
	}
}