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
package ru.catssoftware.gameserver.model.entity;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.zone.L2SiegeZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;

public class Siegeable extends Entity
{
	protected String	_name;
	protected int		_ownerId		= 0;
	protected L2Clan	_formerOwner	= null;

	private L2Zone		_zoneHQ;
	private L2SiegeZone	_zoneBF;
	private L2Zone		_zoneDS;
	private L2Zone		_zoneTP;

	public final String getName()
	{
		return _name;
	}

	public final int getOwnerId()
	{
		return _ownerId;
	}

	public void registerHeadquartersZone(L2Zone zone)
	{
		_zoneHQ = zone;
	}

	public void registerSiegeZone(L2SiegeZone zone)
	{
		_zoneBF = zone;
	}

	public void registerDefenderSpawn(L2Zone zone)
	{
		_zoneDS = zone;
	}

	public void registerTeleportZone(L2Zone zone)
	{
		_zoneTP = zone;
	}

	public void oustAllPlayers()
	{
		for (L2Character player : _zoneTP.getCharactersInside().values())
		{
			// To random spot in defender spawn zone
			if (player.isPlayer())
				player.teleToLocation(_zoneDS.getRandomLocation(), true);
		}
	}

	@Override
	public boolean checkBanish(L2PcInstance cha)
	{
		return cha.getClanId() != getOwnerId() && !cha.isGM();
	}

	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZoneBattlefield(L2Object obj)
	{
		return checkIfInZoneBattlefield(obj.getX(), obj.getY(), obj.getZ());
	}

	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZoneBattlefield(int x, int y, int z)
	{
		return getBattlefield().isInsideZone(x, y, z);
	}

	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZoneHeadQuarters(L2Object obj)
	{
		return checkIfInZoneHeadQuarters(obj.getX(), obj.getY(), obj.getZ());
	}

	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZoneHeadQuarters(int x, int y, int z)
	{
		return getHeadQuarters().isInsideZone(x, y, z);
	}

	public final L2Zone getHeadQuarters()
	{
		return _zoneHQ;
	}

	public final L2SiegeZone getBattlefield()
	{
		return _zoneBF;
	}

	public final L2Zone getDefenderSpawn()
	{
		return _zoneDS;
	}

	public final L2Zone getTeleZone()
	{
		return _zoneTP;
	}
}