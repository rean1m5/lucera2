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
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


public class FortSiegeGuardManager
{
	protected static final Logger	_log				= Logger.getLogger(FortSiegeGuardManager.class.getName());

	private Fort				_fort;
	protected FastMap<Integer, FastList<L2Spawn>> _siegeGuards = new FastMap<Integer, FastList<L2Spawn>>();
	protected FastList<L2Spawn> _siegeGuardsSpawns;

	public FortSiegeGuardManager(Fort fort)
	{
		_fort = fort;
	}

	/**
	 * Spawn guards.<BR><BR>
	 */
	public void spawnSiegeGuard()
	{
		try
		{
			FastList<L2Spawn> monsterList = getSiegeGuardSpawn().get(getFort().getFortId());
			if (monsterList != null)
			{
				for (L2Spawn spawnDat : monsterList)
				{
					spawnDat.doSpawn();
					spawnDat.startRespawn();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Error spawning siege guards for fort " + getFort().getName() + ":" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Unspawn guards.<BR><BR>
	 */
	public void unspawnSiegeGuard()
	{
		try
		{
			FastList<L2Spawn> monsterList = getSiegeGuardSpawn().get(getFort().getFortId());
			if (monsterList != null)
			{
				for (L2Spawn spawnDat : monsterList)
				{
					spawnDat.stopRespawn();
					spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Error unspawning siege guards for fort " + getFort().getName() + ":" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Load guards.<BR><BR>
	 */
	void loadSiegeGuard()
	{
		_siegeGuards.clear();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT * FROM fort_siege_guards WHERE fortId = ? ");
			statement.setInt(1, getFort().getFortId());
			ResultSet rs = statement.executeQuery();

			L2Spawn spawn1;
			L2NpcTemplate template1;

			_siegeGuardsSpawns = new FastList<L2Spawn>();
			while (rs.next())
			{
				int fortId = rs.getInt("fortId");
				template1 = NpcTable.getInstance().getTemplate(rs.getInt("npcId"));
				if (template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setId(rs.getInt("id"));
					spawn1.setAmount(1);
					spawn1.setLocx(rs.getInt("x"));
					spawn1.setLocy(rs.getInt("y"));
					spawn1.setLocz(rs.getInt("z"));
					spawn1.setHeading(rs.getInt("heading"));
					spawn1.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn1.setLocation(0);

					_siegeGuardsSpawns.add(spawn1);
				}
				else
					_log.warn("Missing npc data in npc table for id: " + rs.getInt("npcId"));

				_siegeGuards.put(fortId, _siegeGuardsSpawns);
			}
			rs.close();
			statement.close();
		}
		catch (Exception e1)
		{
			_log.warn("Error loading siege guard for fort " + getFort().getName() + ":" + e1);
			e1.printStackTrace();
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

	public boolean isSiegeGuard(L2Spawn par)
	{
		return _siegeGuards.get(getFort().getFortId()).contains(par);
	}
	
	public final Fort getFort()
	{
		return _fort;
	}

	public final FastMap<Integer, FastList<L2Spawn>> getSiegeGuardSpawn()
	{
		return _siegeGuards;
	}
}