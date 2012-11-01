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

import java.io.File;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;



import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.BanditStrongholdSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.WildBeastFarmSiege;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.entity.Town;
import ru.catssoftware.gameserver.model.mapregion.L2MapArea;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegionRestart;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.geometry.Point3D;

/**
 * @author Noctarius
 */
public class MapRegionManager
{
	private static Logger							_log					= Logger.getLogger(MapRegionManager.class.getName());

	private static MapRegionManager				_instance				= null;

	private Map<Integer, L2MapRegion>			_mapRegions				= new FastMap<Integer, L2MapRegion>();
	private Map<Integer, L2MapRegionRestart>	_mapRegionRestart		= new FastMap<Integer, L2MapRegionRestart>();
	private Map<Integer, L2MapArea>				_mapRestartArea			= new FastMap<Integer, L2MapArea>();
	private Map<Integer, L2MapArea>				_mapAreas				= new FastMap<Integer, L2MapArea>();

	private Map<Integer, L2MapRegion>			_mapRegionsReload		= null;
	private Map<Integer, L2MapRegionRestart>	_mapRegionRestartReload	= null;
	private Map<Integer, L2MapArea>				_mapRestartAreaReload	= null;
	private Map<Integer, L2MapArea>				_mapAreasReload			= null;

	public final static MapRegionManager getInstance()
	{
		if (_instance == null)
			_instance = new MapRegionManager();

		return _instance;
	}

	public MapRegionManager()
	{
		load();
	}

	private void load()
	{
		_mapRegionsReload = new FastMap<Integer, L2MapRegion>();
		_mapRegionRestartReload = new FastMap<Integer, L2MapRegionRestart>();
		_mapRestartAreaReload = new FastMap<Integer, L2MapArea>();
		_mapAreasReload = new FastMap<Integer, L2MapArea>();

		for (File xml : Util.getDatapackFiles("mapregion", ".xml"))
		{
			Document doc = null;

			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

				factory.setValidating(false);
				factory.setIgnoringComments(true);

				doc = factory.newDocumentBuilder().parse(xml);
			}
			catch (Exception e)
			{
				_log.warn("MapRegionManager: Error while loading XML definition: " + xml.getName() + e, e);
				return;
			}

			try
			{
				parseDocument(doc);
			}
			catch (Exception e)
			{
				_log.warn("MapRegionManager: Error in XML definition: " + xml.getName() + e, e);
				return;
			}
		}

		// Replace old maps with reloaded ones
		_mapRegions = _mapRegionsReload;
		_mapRegionRestart = _mapRegionRestartReload;
		_mapRestartArea = _mapRestartAreaReload;
		_mapAreas = _mapAreasReload;

		// Reset unused maps
		_mapRegionsReload = null;
		_mapRegionRestartReload = null;
		_mapRestartAreaReload = null;
		_mapAreasReload = null;

		int redirectCount = 0;

		for (L2MapRegionRestart restart : _mapRegionRestart.values())
		{
			if (restart.getBannedRace() != null)
				redirectCount++;
		}

		_log.info("MapRegionManager: Loaded " + _mapRegionRestart.size() + " restartpoint(s).");
		_log.info("MapRegionManager: Loaded " + _mapRestartArea.size() + " restartareas with " + _mapAreas.size() + " arearegion(s).");
		_log.info("MapRegionManager: Loaded " + _mapRegions.size() + " zoneregion(s).");
		_log.info("MapRegionManager: Loaded " + redirectCount + " race depending redirects.");
	}

	public void reload()
	{
		load();
	}

	private void parseDocument(Document doc) throws Exception
	{
		Map<Integer, L2MapRegion> regions = new FastMap<Integer, L2MapRegion>();
		Map<Integer, L2MapRegionRestart> restarts = new FastMap<Integer, L2MapRegionRestart>();
		Map<Integer, L2MapArea> restartAreas = new FastMap<Integer, L2MapArea>();
		Map<Integer, L2MapArea> areas = new FastMap<Integer, L2MapArea>();

		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("mapregion".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("regions".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node f = d.getFirstChild(); f != null; f = f.getNextSibling())
						{
							if ("region".equalsIgnoreCase(f.getNodeName()))
							{
								L2MapRegion region = new L2MapRegion(f);

								if (!regions.containsKey(region.getId()))
									regions.put(region.getId(), region);
								else
									throw new Exception("Duplicate zoneRegionId: " + region.getId() + ".");
							}
						}
					}
					else if ("restartpoints".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node f = d.getFirstChild(); f != null; f = f.getNextSibling())
						{
							if ("restartpoint".equalsIgnoreCase(f.getNodeName()))
							{
								L2MapRegionRestart restart = new L2MapRegionRestart(f);

								if (!restarts.containsKey(restart.getId()))
									restarts.put(restart.getId(), restart);
								else
									throw new Exception("Duplicate restartpointId: " + restart.getId() + ".");
							}
						}
					}
					else if ("areas".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node f = d.getFirstChild(); f != null; f = f.getNextSibling())
						{
							if ("restartarea".equalsIgnoreCase(f.getNodeName()))
							{
								int id = -1;

								Node e = f.getAttributes().getNamedItem("id");
								if (e != null)
								{
									id = Integer.parseInt(e.getTextContent());

									for (Node r = f.getFirstChild(); r != null; r = r.getNextSibling())
									{
										if ("map".equalsIgnoreCase(r.getNodeName()))
										{
											int X = 0;
											int Y = 0;

											Node t = r.getAttributes().getNamedItem("X");
											if (t != null)
												X = Integer.parseInt(t.getTextContent());

											t = r.getAttributes().getNamedItem("Y");
											if (t != null)
												Y = Integer.parseInt(t.getTextContent());

											L2MapArea area = new L2MapArea(id, X, Y);

											if (!areas.containsKey(area.getId()))
											{
												restartAreas.put(id, area);
												areas.put(area.getId(), area);
												regions.put(area.getId(), area.getMapRegion());
											}
											else
												throw new Exception("Duplicate areaRegionId: " + area.getId() + ".");
										}
									}
								}
							}
						}
					}
				}
			}
		}

		_mapRegionsReload = regions;
		_mapRegionRestartReload = restarts;
		_mapRestartAreaReload = restartAreas;
		_mapAreasReload = areas;
	}

	public Map<Integer, L2MapRegion> getRegions()
	{
		return _mapRegions;
	}

	public L2MapRegionRestart getRestartLocation(L2PcInstance activeChar)
	{
		L2MapRegion region = getRegion(activeChar);

		// Temporary fix for new hunting grounds
		if (region == null)
			return _mapRegionRestart.get(Config.ALT_DEFAULT_RESTARTTOWN);

		int restartId = region.getRestartId(activeChar.getRace());

		return _mapRegionRestart.get(restartId);
	}

	public L2MapRegionRestart getRestartLocation(int restartId)
	{
		return _mapRegionRestart.get(restartId);
	}

	public Point3D getRestartPoint(L2PcInstance activeChar)
	{
		return getRestartPoint(getRegion(activeChar), activeChar);
	}

	public Point3D getRestartPoint(L2MapRegion region, L2PcInstance activeChar)
	{
		if (region != null)
		{
			int restartId = region.getRestartId(activeChar.getRace());

			L2MapRegionRestart restart = _mapRegionRestart.get(restartId);

			if (restart != null)
				return restart.getRandomRestartPoint(activeChar.getRace());
		}

		L2MapRegionRestart restart = _mapRegionRestart.get(Config.ALT_DEFAULT_RESTARTTOWN);
		return restart.getRandomRestartPoint(activeChar.getRace());
	}

	public Point3D getRestartPoint(int restartId)
	{
		L2MapRegionRestart restart = _mapRegionRestart.get(restartId);

		if (restart != null)
			return restart.getRandomRestartPoint();

		restart = _mapRegionRestart.get(Config.ALT_DEFAULT_RESTARTTOWN);
		return restart.getRandomRestartPoint();
	}

	public Point3D getChaosRestartPoint(L2PcInstance activeChar)
	{
		L2MapRegion region = getRegion(activeChar);

		if (region != null)
		{
			int restartId = region.getRestartId(activeChar.getRace());

			L2MapRegionRestart restart = _mapRegionRestart.get(restartId);

			if (restart != null)
				return restart.getRandomChaosRestartPoint(activeChar.getRace());
		}

		L2MapRegionRestart restart = _mapRegionRestart.get(Config.ALT_DEFAULT_RESTARTTOWN);
		return restart.getRandomChaosRestartPoint(activeChar.getRace());
	}

	public Point3D getChaosRestartPoint(int restartId)
	{
		L2MapRegionRestart restart = _mapRegionRestart.get(restartId);

		if (restart != null)
			return restart.getRandomChaosRestartPoint();

		restart = _mapRegionRestart.get(Config.ALT_DEFAULT_RESTARTTOWN);
		return restart.getRandomChaosRestartPoint();
	}

	public L2MapRegion getRegion(L2Character activeChar)
	{
		return getRegion(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}

	public L2MapRegion getRegionById(int regionId)
	{
		return _mapRegions.get(regionId);
	}

	public L2MapRegion getRegion(int x, int y, int z)
	{
		L2MapRegion areaRegion = null;

		for (L2MapRegion region : _mapRegions.values())
		{
			if (region.checkIfInRegion(x, y, z))
			{
				// prefer special regions
				if (region.isSpecialRegion())
					return region;

				areaRegion = region;
			}
		}

		return areaRegion;
	}

	public L2MapRegion getRegion(int x, int y)
	{
		return getRegion(x, y, -1);
	}

	public int getNextAccessibleRestartId(L2MapRegionRestart restart, L2PcInstance activeChar)
	{
		Town town = TownManager.getInstance().getTownByMaprestart(restart);
		if (town != null && town.hasCastleInSiege() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
		{
			int newTownId = TownManager.getInstance().getRedirectTownNumber(town.getTownId());
			town = TownManager.getInstance().getTown(newTownId);
			L2MapRegion region = town.getMapRegion();
			if (region != null)
				return region.getRestartId(activeChar.getRace());
		}
		return restart.getId();
	}

	//TODO: Needs to be clean rewritten
	public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere)
	{
		if (activeChar.isPlayer())
		{
			L2PcInstance player = (L2PcInstance) activeChar;
			L2Clan clan = player.getClan();
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;

			// Checking if in Dimensinal Gap
			if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), true)) // true -> ignore waiting room :)
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
					player.getParty().getDimensionalRift().usedTeleport(player);

				return DimensionalRiftManager.getInstance().getWaitingRoomTeleport();
			}

			// Checking if in an instance
			if (player.getInstanceId() > 0)
			{
				Instance playerInstance = InstanceManager.getInstance().getInstance(player.getInstanceId());
				if (playerInstance != null)
				{
					if (playerInstance.getSpawnLoc() != null)
					{
						int[] coord = playerInstance.getSpawnLoc();
						return new Location(coord[0], coord[1], coord[2]);
					}
				}
			}

			// Checking if in arena
			L2Zone arena = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Arena, player.getX(), player.getY());
			if (arena != null && arena.isInsideZone(player))
			{
				Location loc = arena.getRestartPoint(L2Zone.RestartType.OWNER);
				if (loc == null)
					loc = arena.getRandomLocation();
				return loc;
			}

			if (teleportWhere == TeleportWhereType.Town)
			{
				L2MapRegionRestart restart = getRestartLocation(player);
				int restartId = getNextAccessibleRestartId(restart, player);

				Location loc = null;
				// Karma player land out of city
				if (player.getKarma() > 1 || player.isCursedWeaponEquipped())
					loc = getLocationFromPoint3D(getChaosRestartPoint(restartId));
				if (loc == null)
					loc = getLocationFromPoint3D(getRestartPoint(restartId));
				return loc;
			}

			if (clan != null)
			{
				// If teleport to clan hall
				if (teleportWhere == TeleportWhereType.ClanHall)
				{
					clanhall = ClanHallManager.getInstance().getClanHallByOwner(clan);
					if (clanhall != null)
					{
						L2Zone zone = clanhall.getZone();

						if (zone != null)
						{
							Location loc = zone.getRestartPoint(L2Zone.RestartType.OWNER);
							if (loc == null)
								loc = zone.getRandomLocation();
							return loc;
						}
					}
				}

				// If teleport to castle
				if (teleportWhere == TeleportWhereType.Castle)
					castle = CastleManager.getInstance().getCastleByOwner(clan);

				else if (teleportWhere == TeleportWhereType.Fortress)
					fort = FortManager.getInstance().getFortByOwner(clan);

				// If Teleporting to castle or
				if (castle != null && teleportWhere == TeleportWhereType.Castle)
				{
					L2Zone zone = castle.getZone();

					if (zone != null)
					{
						if ((castle.getSiege() != null && castle.getSiege().getIsInProgress()) && (player.getKarma() > 1 || player.isCursedWeaponEquipped()))
							// Karma player respawns out of siege zone (only during sieges ? o.O )
							return zone.getRestartPoint(L2Zone.RestartType.CHAOTIC);

						return zone.getRestartPoint(L2Zone.RestartType.OWNER);
					}
				}
				else if (fort != null && teleportWhere == TeleportWhereType.Fortress)
				{
					L2Zone zone = fort.getZone();

					if (zone != null)
					{
						// If is on castle with siege and player's clan is defender
						if ((fort.getSiege() != null && fort.getSiege().getIsInProgress()) && (player.getKarma() > 1 || player.isCursedWeaponEquipped()))
							// Karma player respawns out of siege zone (only during sieges ? o.O )
							return zone.getRestartPoint(L2Zone.RestartType.CHAOTIC);

						return zone.getRestartPoint(L2Zone.RestartType.OWNER);
					}
				}
				else if (teleportWhere == TeleportWhereType.SiegeFlag)
				{
					Siege siege = SiegeManager.getInstance().getSiege(clan);
					FortSiege fsiege = FortSiegeManager.getInstance().getSiege(clan);

					// Check if player's clan is attacker
					if (siege != null && fsiege == null && siege.checkIsAttacker(clan) && siege.checkIfInZone(player))
					{
						// Karma player respawns out of siege zone
						if (player.getKarma() > 1 || player.isCursedWeaponEquipped())
						{
							L2Zone zone = siege.getCastle().getZone();
							if (zone != null)
								return zone.getRestartPoint(L2Zone.RestartType.CHAOTIC);
						}
						// get nearest flag
						L2NpcInstance flag = siege.getClosestFlag(player);
						// spawn to flag
						if (flag != null)
							return new Location(flag.getX(), flag.getY(), flag.getZ());
					}
					else if (siege == null && fsiege != null && fsiege.checkIsAttacker(clan) && fsiege.checkIfInZone(player))
					{
						// Karma player respawns out of siege zone
						if (player.getKarma() > 1 || player.isCursedWeaponEquipped())
						{
							L2Zone zone = fsiege.getFort().getZone();
							if (zone != null)
								return zone.getRestartPoint(L2Zone.RestartType.CHAOTIC);
						}
						// Get nearest flag
						L2NpcInstance flag = fsiege.getClosestFlag(player);
						// Spawn to flag
						if (flag != null)
							return new Location(flag.getX(), flag.getY(), flag.getZ()); 
					}
					else if (BanditStrongholdSiege.getInstance().isPlayerRegister(((L2PcInstance)activeChar).getClan(),activeChar.getName()))
					{
						L2NpcInstance flag = BanditStrongholdSiege.getInstance().getSiegeFlag(((L2PcInstance)activeChar).getClan());
						if (flag != null)
							return new Location(flag.getX(), flag.getY(), flag.getZ()); 
					}
					else if (WildBeastFarmSiege.getInstance().isPlayerRegister(((L2PcInstance)activeChar).getClan(),activeChar.getName()))
					{
						L2NpcInstance flag = WildBeastFarmSiege.getInstance().getSiegeFlag(((L2PcInstance)activeChar).getClan());
						if (flag != null)
							return new Location(flag.getX(), flag.getY(), flag.getZ()); 
					}
				}
			}
		}

		// teleport to default town if nothing else will work
		return getLocationFromPoint3D(getRestartPoint(Config.ALT_DEFAULT_RESTARTTOWN));
	}

	public Location getLocationFromPoint3D(Point3D point)
	{
		return new Location(point.getX(), point.getY(), point.getZ());
	}

	public int getAreaCastle(L2Character activeChar)
	{
		Town town = TownManager.getInstance().getClosestTown(activeChar);

		if (town == null)
			return 5;

		return town.getCastleId();
	}
	/**
	 * < 1, 8, > 15 - Empty<BR>1 - Talking island<BR>2 - Gludio<BR>
	 * 3 - Dark elven<BR>4 - Elven<BR>5 - Dion<BR>6 - Giran<BR>
	 * 7 - Neutral zone<BR>9 - Schuttgart<BR>10 - Oren<BR>
	 * 11 - Hunters Village<BR>12 - Innadril<BR>13 - Aden<BR>
	 * 14 - Rune<BR>15 - Goddard
	 * @param player a player
	 * @return L2 region used in partymatching
	 */
	public int getL2Region(L2PcInstance player)
	{
		L2MapRegion region = getRegion(player);
		int locName = -1;
		if (region != null)
		{
			int restartId = region.getRestartId();
			L2MapRegionRestart restart = getRestartLocation(restartId);
			locName = restart.getLocName();
		}
		return convertLocNameToL2Region(locName);
	}
	
	public int convertLocNameToL2Region(int locName)
	{
		switch(locName)
		{
		case 910: // TI
			return 1;
		case 911: // Gludin
		case 912: // Gludio
		case 2190: // Southern wastelands
		case 2710: // Keucereus
		case 2711: // inside SoI
		case 2712: // outside SoI
		case 2716: // inside Cleft
			return 2;
		case 913: // Neutral zone
			return 7;
		case 914: // Elven village
			return 4;
		case 915: // DE village
			return 3;
		case 916: // Dion
		case 917: // Floran
			return 5;
		case 918: // Giran
		case 919: // Harbor
			return 6;
		case 920: // Orc village
		case 921: // Dwarven village
		case 1714: // Schuttgart
			return 9;
		case 922: // Oren
			return 10;
		case 923: // Hunters village
			return 11;
		case 924: // Aden
		case 925: // Coliseum
		case 2189: // Kamael Village
			return 13;
		case 926: // Heine
			return 12;
		case 1537: // Rune
		case 1924: // Primeval Isle
		case 2259: // Fantasy Isle
			return 14;
		case 1538: // Goddard
			return 15;
		default: // TODO: Epilogue locations
			return 0; // no name
		}
	}	
}