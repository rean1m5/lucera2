package ru.catssoftware.gameserver.geodata;


import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.geodata.pathfinding.Node;
import ru.catssoftware.gameserver.geodata.pathfinding.PathFinding;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.tools.geometry.Point3D;

import java.io.File;

public class GeoData
{
	private static GeoData _instance;
	protected static Logger _log = Logger.getLogger(GeoData.class);
	public static enum PathFindingMode {
		PATHNODE,
		CELLFINDING
	}

	public static GeoData getInstance()
	{
		if(_instance == null)
		{
		
			File f;
			if (Config.GEODATA) try {
				f = new File(Config.GEODATA_ROOT,"/geodata");
				if(!f.exists()) {
					_log.error("Geo Engine: folder geodata not found in "+Config.GEODATA_ROOT);
					System.exit(1);
					return null;
				}
				if(Config.PATHFINDING && (Config.PATHFIND_MODE == PathFindingMode.PATHNODE)) {
					f = new File(Config.GEODATA_ROOT,"/pathnode");
					if(!f.exists()) {
						_log.error("Pathfind Engine: folder pathnode not found in "+Config.GEODATA_ROOT);
						System.exit(1);
						return null;
					}
				}
				_instance = GeoEngine.getInstance();
			} catch(Exception e) {
				_instance = new GeoData();
			} else
				_instance = new GeoData();
			PathFinding.getInstance();
		}
		return _instance;
	}

	public short getType  (int x, int y)
	{
		return 0;
	}

	public short getHeight(int x, int y, int z)
	{
		return (short)z;
	}

	public short getSpawnHeight(int x, int y, int zmin, int zmax, int spawnid)
	{
		return (short)zmin;
	}

	public String geoPosition(int x, int y)
	{
		return "";
	}

	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		return (Math.abs(target.getZ() - cha.getZ()) < 1000);
	}

	public boolean canSeeTarget(L2Object cha, L2Object target, int instance)
	{
		return (Math.abs(target.getZ() - cha.getZ()) < 1000);
	}

	public boolean canSeeTarget(L2Object cha, Point3D worldPosition, int instance)
	{
		return Math.abs(worldPosition.getZ() - cha.getZ()) < 1000;
	}

	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return (Math.abs(z - tz) < 1000);
	}

	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		return true;
	}

	public short getNSWE(int x, int y, int z)
	{
		return 15;
	}

	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz, int instance)
	{
		return new Location(tx,ty,tz);
	}

	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz, int instance)
	{
		return true;
	}

	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
	}

	public static void unloadGeodata(byte rx, byte ry)
	{
	}

	public static boolean loadGeodataFile(byte rx, byte ry)
	{
		return false;
	}

	public boolean hasGeo(int x, int y)
	{
		return false;
	}

	public Node[] getNeighbors(Node n)
	{
		return null;
	}

	public short getHeightAndNSWE(int x, int y, short z) {
		return (short)((z << 1) | 15);
	}
	
	public short findGround(int x, int y) {
		return 0;
	}
	
}