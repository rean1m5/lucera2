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
import java.util.Map;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowInfoUpdate;

public class ClanHallManager
{
	protected static Logger			_log	= Logger.getLogger(ClanHallManager.class.getName());

	private static ClanHallManager	_instance;

	private Map<Integer, ClanHall>	_clanHall;
	private Map<Integer, ClanHall>	_freeClanHall;
	private Map<Integer, ClanHall>	_allClanHalls;
	private Map<Integer, ClanHall[]>  _townClanHalls;
	private final Town[]					_towns;
	private static boolean			_loaded	= false;

	public static ClanHallManager getInstance()
	{
		if (_instance == null)
			_instance = new ClanHallManager();

		return _instance;
	}

	public static boolean loaded()
	{
		return _loaded;
	}

	private ClanHallManager()
	{
		_clanHall = new FastMap<Integer, ClanHall>();
		_freeClanHall = new FastMap<Integer, ClanHall>();
		_allClanHalls = new FastMap<Integer, ClanHall>();
		_townClanHalls = new FastMap<Integer, ClanHall[]>();
		
		_towns = new Town[8];
		_towns[0] = new Town(5, "Gludio");
		_towns[1] = new Town(6, "Gludin");
		_towns[2] = new Town(7, "Dion");
		_towns[3] = new Town(8, "Giran");
		_towns[4] = new Town(10, "Aden");
		_towns[5] = new Town(15, "Goddard");
		_towns[6] = new Town(14, "Rune");
		_towns[7] = new Town(16, "Schuttgart");
		
		load();
	}

	/** Reload All Clan Hall */
	public final void reload()
	{
		_loaded = false;
		_clanHall.clear();
		_freeClanHall.clear();
		_allClanHalls.clear();
		load();
	}

	/** Load All Clan Hall */
	private final void load()
	{
		Connection con = null;
		try
		{
			int id, ownerId, lease, grade = 0;
			String Name, Desc, Location;
			long paidUntil = 0, paidDayTime = 0;
			boolean paid = false;

			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection(con);
			statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				id = rs.getInt("id");
				Name = rs.getString("name");
				ownerId = rs.getInt("ownerId");
				lease = rs.getInt("lease");
				Desc = rs.getString("desc");
				Location = rs.getString("location");
				paidUntil = rs.getLong("paidUntil");
				paidDayTime = rs.getLong("paidDayTime");
				grade = rs.getInt("Grade");
				paid = rs.getBoolean("paid");

				ClanHall ch = new ClanHall(id, Name, ownerId, lease, Desc, Location, paidUntil, paidDayTime, grade, paid);
				if (ownerId == 0)
					_freeClanHall.put(id, ch);
				else
				{
					L2Clan clan = ClanTable.getInstance().getClan(ownerId);
					if (clan != null)
					{
						_clanHall.put(id, ch);
						clan.setHasHideout(id);
					}
					else
					{
						_freeClanHall.put(id, ch);
						ch.free();
						AuctionManager.getInstance().initNPC(id);
					}
				}
				_allClanHalls.put(id, ch);
			}
			statement.close();
			_log.info("ClanHallManager: loaded " + getClanHalls().size() + " clan halls");
			_log.info("ClanHallManager: loaded " + getFreeClanHalls().size() + " free clan halls");
			ClanHall[] allHalls = _allClanHalls.values().toArray(new ClanHall[_allClanHalls.size()]);
			FastList<ClanHall> townHalls = new FastList<ClanHall>();
			for (Town t : _towns)
			{
				for (ClanHall ch : allHalls)
					if (ch.getLocation().equals(t.toString()))
						townHalls.add(ch);

				_townClanHalls.put(t.getId(), townHalls.toArray(new ClanHall[townHalls.size()]));
				_log.info("ClanHallManager: " + townHalls.size() + " halls in " + t.toString());
				townHalls.clear();
			}
			_loaded = true;
		}
		catch (SQLException e)
		{
			_log.fatal("Exception: ClanHallManager.load(): " + e.getMessage());
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

	/** Get Map with all FreeClanHalls */
	public final Map<Integer, ClanHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}

	/** Get Map with all ClanHalls that have owner*/
	public final Map<Integer, ClanHall> getClanHalls()
	{
		return _clanHall;
	}

	/** Get Map with all ClanHalls*/
	public final Map<Integer, ClanHall> getAllClanHalls()
	{
		return _allClanHalls;
	}

	/** Check is free ClanHall */
	public final boolean isFree(int chId)
	{
		return _freeClanHall.containsKey(chId);
	}

	/** Free a ClanHall */
	public final synchronized void setFree(int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		L2Clan oldClan = ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()); 
		oldClan.setHasHideout(0);
		oldClan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(oldClan));
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}

	/** Set ClanHallOwner */
	public final synchronized void setOwner(int chId, L2Clan clan)
	{
		if(clan.getHasHideout()!=0)
			setFree(clan.getHasHideout());
		if (!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
		{
			setFree(chId);
			_clanHall.put(chId, _freeClanHall.get(chId));
		}
		ClanTable.getInstance().getClan(clan.getClanId()).setHasHideout(chId);
		_clanHall.get(chId).setOwner(clan);
	}

	/** Return true if object is inside zone */
	public final boolean checkIfInZone(L2Object obj)
	{
		return (getClanHall(obj) != null);
	}

	/** Return true if object is inside zone */
	public final boolean checkIfInZone(int x, int y)
	{
		return (getClanHall(x, y) != null);
	}

	/** Get Clan Hall by Id */
	public final ClanHall getClanHallById(int clanHallId)
	{
		return _allClanHalls.get(clanHallId);
	}

	/** Get Clan Hall by Object */
	public final ClanHall getClanHall(L2Object activeObject)
	{
		return getClanHall(activeObject.getPosition().getX(), activeObject.getPosition().getY());
	}

	/** Get Clan Hall by region x,y,offset */
	public final ClanHall getClanHall(int x, int y)
	{
		for (Map.Entry<Integer, ClanHall> ch : _allClanHalls.entrySet())
		{
			if (ch.getValue().checkIfInZone(x, y))
				return ch.getValue();
		}
		return null;
	}

	/** Get Clan Hall by name */
	public final ClanHall getClanHall(String name)
	{
		for (Map.Entry<Integer, ClanHall> ch : _allClanHalls.entrySet())
		{
			if (ch.getValue().getName().equals(name))
				return ch.getValue();
		}
		return null;
	}

	/** Get Clan Hall by Owner */
	public final ClanHall getClanHallByOwner(L2Clan clan)
	{
		for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
		{
			if (clan.getClanId() == ch.getValue().getOwnerId())
				return ch.getValue();
		}
		return null;
	}

	public final ClanHall getNearbyClanHall(int x, int y, int maxDist)
	{
		ClanHall clanH = null;
		double distance;

		for (Map.Entry<Integer, ClanHall> ch : _allClanHalls.entrySet())
		{
			distance = ch.getValue().getDistanceToZone(x, y);

			if (distance < maxDist)
			{
				if (clanH == null)
					clanH = ch.getValue();
				else if (distance < clanH.getDistanceToZone(x, y))
					clanH = ch.getValue();
			}
		}

		return clanH;
	}
	public final ClanHall[] getTownClanHalls(int townId)
	{
		return _townClanHalls.get(townId);
	}
	private class Town {
		private final int _id;
		private final String _loc;
		private Town(int id, String loc)
		{
			_id = id;
			_loc = loc;
		}
		private final int getId(){return _id;}
		private final String getLocation(){return _loc;}
		public final String toString(){return getLocation();}
	}	
}