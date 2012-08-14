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
import java.sql.ResultSet;
import java.sql.SQLException;

import javolution.util.FastList;
import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;

/**
 * @author evill33t
 *
 */
public class Faction
{
	private static final Logger			_log			= Logger.getLogger(Faction.class.getName());

	private int							_Id				= 0;
	private String						_name			= null;
	private float						_points			= 0;
	private int							_joinprice		= 0;
	private int							_side			= 0;											// 0 = Neutral 1 = Good 2 = Evil
	private FastList<Integer>			_list_classes	= new FastList<Integer>();
	private FastList<Integer>			_list_npcs		= new FastList<Integer>();
	private FastMap<Integer, String>	_list_title		= new FastMap<Integer, String>();

	public Faction(int factionId)
	{
		_Id = factionId;
		String _classlist = null;
		String _npclist = null;
		String _titlelist = null;
		int _tside = 0;

		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("Select * from factions where id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			while (rs.next())
			{
				_name = rs.getString("name");
				_joinprice = rs.getInt("price");
				_classlist = rs.getString("allowed_classes");
				_titlelist = rs.getString("titlelist");
				_npclist = rs.getString("npcs");
				_points = rs.getFloat("points");
				_tside = rs.getInt("side");
			}
			statement.close();

			if (_tside <= 2)
				_side = _tside;

			if (_classlist.length() > 0)
				for (String id : _classlist.split(","))
					_list_classes.add(Integer.parseInt(id));

			if (_npclist.length() > 0)
				for (String id : _npclist.split(","))
					_list_npcs.add(Integer.parseInt(id));

			if (_titlelist.length() > 0)
				for (String id : _titlelist.split(";"))
					_list_title.put(Integer.valueOf(id.split(",")[0]), id.split(",")[1]);
		}
		catch (Exception e)
		{
			_log.warn("Exception: Faction load: " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	private void updateDB()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("update factions set points = ? where id = ?");
			statement.setFloat(1, _points);
			statement.setInt(2, _Id);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception: Faction.load(): " + e.getMessage(), e);
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

	public void addPoints(int points)
	{
		_points += points;
		updateDB();
	}

	public void clearPoints()
	{
		_points = 0;
		updateDB();
	}

	public final int getId()
	{
		return _Id;
	}

	public final String getName()
	{
		return _name;
	}

	public final float getPoints()
	{
		return _points;
	}

	public final FastList<Integer> getClassList()
	{
		return _list_classes;
	}

	public final FastList<Integer> getNpcList()
	{
		return _list_npcs;
	}

	public final FastMap<Integer, String> getTitle()
	{
		return _list_title;
	}

	public final int getPrice()
	{
		return _joinprice;
	}

	public final int getSide()
	{
		return _side;
	}
}