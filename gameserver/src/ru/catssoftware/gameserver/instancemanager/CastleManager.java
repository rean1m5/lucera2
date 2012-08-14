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

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;

public class CastleManager
{
	protected static Logger				_log	= Logger.getLogger(CastleManager.class.getName());

	private static CastleManager		_instance;
	private FastMap<Integer, Castle>	_castles;

	public static final CastleManager getInstance()
	{
		if (_instance == null)
			_instance = new CastleManager();
		return _instance;
	}

	private static final int	_castleCirclets[]	= { 0, 6838, 6835, 6839, 6837, 6840, 6834, 6836, 8182, 8183 };

	public CastleManager()
	{
		load();
	}

	public final Castle getClosestCastle(L2Object activeObject)
	{
		Castle castle = getCastle(activeObject);
		if (castle == null)
		{
			double closestDistance = Double.MAX_VALUE;
			double distance;

			for (Castle castleToCheck : getCastles().values())
			{
				if (castleToCheck == null)
					continue;
				distance = castleToCheck.getDistanceToZone(activeObject.getX(), activeObject.getY());
				if (closestDistance > distance)
				{
					closestDistance = distance;
					castle = castleToCheck;
				}
			}
		}
		return castle;
	}

	public void reload()
	{
		getCastles().clear();
		load();
	}

	private final void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("SELECT id FROM castle ORDER BY id");
			rs = statement.executeQuery();

			while (rs.next())
			{
				int id = rs.getInt("id");
				getCastles().put(id, new Castle(id));
			}

			statement.close();

			_log.info("Loaded: " + getCastles().size() + " castles");
		}
		catch (SQLException e)
		{
			_log.warn("Exception: loadCastleData(): " + e.getMessage());
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

	public final Castle getCastleById(int castleId)
	{
		return getCastles().get(castleId);
	}

	public final Castle getCastle(L2Object activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final Castle getCastle(int x, int y, int z)
	{
		Castle castle;
		for (int i = 1; i <= getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if (castle != null && castle.checkIfInZone(x, y, z))
				return castle;
		}
		return null;
	}

	public final Castle getCastleByName(String name)
	{
		Castle castle;
		for (int i = 1; i <= getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if (castle != null && castle.getName().equalsIgnoreCase(name.trim()))
				return castle;
		}
		return null;
	}

	public final Castle getCastleByOwner(L2Clan clan)
	{
		if (clan == null)
			return null;

		Castle castle;
		for (int i = 1; i <= getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if (castle != null && castle.getOwnerId() == clan.getClanId())
				return castle;
		}
		return null;
	}

	public final FastMap<Integer, Castle> getCastles()
	{
		if (_castles == null)
			_castles = new FastMap<Integer, Castle>();
		return _castles;
	}

	public final void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		switch (sealStrifeOwner)
		{
		case SevenSigns.CABAL_DUSK:
			maxTax = 5;
			break;
		case SevenSigns.CABAL_DAWN:
			maxTax = 25;
			break;
		default: // no owner
			maxTax = 15;
			break;
		}

		for (Castle castle : _castles.values())
			if (castle.getTaxPercent() > maxTax)
				castle.setTaxPercent(maxTax);
	}

	int	_castleId	= 1;	// from this castle

	public int getCirclet()
	{
		return getCircletByCastleId(_castleId);
	}

	public int getCircletByCastleId(int castleId)
	{
		if (castleId > 0 && castleId < 10)
			return _castleCirclets[castleId];

		return 0;
	}

	// remove this castle's circlets from the clan
	public void removeCirclet(L2Clan clan, int castleId)
	{
		for (L2ClanMember member : clan.getMembers())
			removeCirclet(member, castleId);
	}

	public void removeCirclet(L2ClanMember member, int castleId)
	{
		if (member == null)
			return;
		L2PcInstance player = member.getPlayerInstance();
		int circletId = getCircletByCastleId(castleId);

		if (circletId != 0)
		{
			// online-player circlet removal
			if (player != null && player.getInventory() != null)
			{
				L2ItemInstance circlet = player.getInventory().getItemByItemId(circletId);
				if (circlet != null)
				{
					if (circlet.isEquipped())
						player.getInventory().unEquipItemInSlotAndRecord(circlet.getLocationSlot());
					player.destroyItemByItemId("CastleCircletRemoval", circletId, 1, player, true);
				}
				return;
			}
			// else offline-player circlet removal
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?");
				statement.setInt(1, member.getObjectId());
				statement.setInt(2, circletId);
				statement.execute();
				statement.close();
			}
			catch (SQLException e)
			{
				_log.error("Failed to remove castle circlets offline for player " + member.getName(), e);
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
}
