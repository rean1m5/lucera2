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
package ru.catssoftware.gameserver.geodata;

import gnu.trove.TShortObjectHashMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.util.RndCoord;
import ru.catssoftware.tools.geometry.Point3D;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

/*
 * @author Ro0TT
 * @date 28.09.2012
 */

/**
 * @author -Nemesiss-
 */
public class GeoEngine extends GeoData
{
	private static final Logger _log = Logger.getLogger(GeoEngine.class);
	
	private static final byte EAST = 1;
	private static final byte WEST = 2;
	private static final byte SOUTH = 4;
	private static final byte NORTH = 8;
	private static final byte NSWE_ALL = 15;
	private static TShortObjectHashMap<MappedByteBuffer> _geodata = new TShortObjectHashMap<MappedByteBuffer>();
	private static TShortObjectHashMap<IntBuffer> _geodataIndex = new TShortObjectHashMap<IntBuffer>();
	private static BufferedOutputStream _geoBugsOut;
	
	/**
	 * Gets the single instance of GeoEngine.
	 * @return single instance of GeoEngine
	 */
	public static GeoEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Instantiates a new geo engine.
	 */
	protected GeoEngine()
	{
		nInitGeodata();
	}
	
	@Override
	public short getType(int x, int y)
	{
		return nGetType((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4);
	}
	
	@Override
	public short getHeight(int x, int y, int z)
	{
		return nGetHeight((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z);
	}
	
	@Override
	public short getSpawnHeight(int x, int y, int zmin, int zmax, L2Object object)
	{
		return nGetSpawnHeight((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, zmin, zmax, object);
	}
	
	@Override
	public String geoPosition(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		return "bx: " + getBlock(gx) + " by: " + getBlock(gy) + " cx: " + getCell(gx) + " cy: " + getCell(gy) + "  region offset: "
		+ getRegionOffset(gx, gy);
	}
	
	@Override
	public boolean canSeeTarget(L2Object cha, Point3D target)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ(), cha.getInstanceId()))
			return false;
		if (cha.getZ() >= target.getZ())
			return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ());
		return canSeeTarget(target.getX(), target.getY(), target.getZ(), cha.getX(), cha.getY(), cha.getZ());
	}
	
	@Override
	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		if (cha == null || target == null)
			return false;
		// To be able to see over fences and give the player the viewpoint
		// game client has, all coordinates are lifted 45 from ground.
		// Because of layer selection in LOS algorithm (it selects -45 there
		// and some layers can be very close...) do not change this without
		// changing the LOS code.
		// Basically the +45 is character height. Raid bosses are naturally higher,
		// dwarves shorter, but this should work relatively well.
		// If this is going to be improved, use e.g.
		// ((L2Character)cha).getTemplate().collisionHeight
		int z = cha.getZ() + 45;
		int z2 = target.getZ() + 45;
		if (!(target instanceof L2DoorInstance)
				&& DoorTable.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), z, target.getX(), target.getY(), z2, cha.getInstanceId()))
			return false;
		if (target instanceof L2DoorInstance)
			return true; // door coordinates are hinge coords..
		if (cha.getZ() >= target.getZ())
			return canSeeTarget(cha.getX(), cha.getY(), z, target.getX(), target.getY(), z2);
		return canSeeTarget(target.getX(), target.getY(), z2, cha.getX(), cha.getY(), z);
	}
	
	@Override
	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		// comments: see above
		int z = gm.getZ() + 45;
		int z2 = target.getZ() + 45;
		if (target instanceof L2DoorInstance)
		{
			gm.sendMessage("door always true");
			return true; // door coordinates are hinge coords..
		}
		
		if (gm.getZ() >= target.getZ())
			return canSeeDebug(gm, (gm.getX() - L2World.MAP_MIN_X) >> 4, (gm.getY() - L2World.MAP_MIN_Y) >> 4, z, (target.getX() - L2World.MAP_MIN_X) >> 4, (target.getY() - L2World.MAP_MIN_Y) >> 4, z2);
		return canSeeDebug(gm, (target.getX() - L2World.MAP_MIN_X) >> 4, (target.getY() - L2World.MAP_MIN_Y) >> 4, z2, (gm.getX() - L2World.MAP_MIN_X) >> 4, (gm.getY() - L2World.MAP_MIN_Y) >> 4, z);
	}
	
	@Override
	public short getNSWE(int x, int y, int z)
	{
		return nGetNSWE((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z);
	}
	
	@Override
	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		Location destiny = moveCheck(x, y, z, tx, ty, tz, instanceId);
		return (destiny.getX() == tx && destiny.getY() == ty && destiny.getZ() == tz);
	}
	
	@Override
	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		Location startpoint = new Location(x, y, z);
		if (DoorTable.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceId))
			return startpoint;
		
		Location destiny = new Location(tx, ty, tz);
		return moveCheck(startpoint, destiny, (x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, (tx - L2World.MAP_MIN_X) >> 4, (ty - L2World.MAP_MIN_Y) >> 4, tz);
	}
	
	@Override
	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
		int gx = (gm.getX() - L2World.MAP_MIN_X) >> 4;
		int gy = (gm.getY() - L2World.MAP_MIN_Y) >> 4;
		int bx = getBlock(gx);
		int by = getBlock(gy);
		int cx = getCell(gx);
		int cy = getCell(gy);
		int rx = (gx >> 11) + Config.WORLD_X_MIN;
		int ry = (gy >> 11) + Config.WORLD_X_MAX;
		String out = rx + ";" + ry + ";" + bx + ";" + by + ";" + cx + ";" + cy + ";" + gm.getZ() + ";" + comment + "\n";
		try
		{
			_geoBugsOut.write(out.getBytes());
			_geoBugsOut.flush();
			gm.sendMessage("GeoData bug saved!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			gm.sendMessage("GeoData bug save Failed!");
		}
	}

	@Override
	public boolean canSeeTarget(Location loc, Location locTarget)
	{
		return canSee((loc.getX() - L2World.MAP_MIN_X) >> 4, (loc.getY() - L2World.MAP_MIN_Y) >> 4, loc.getZ(), (locTarget.getX() - L2World.MAP_MIN_X) >> 4, (locTarget.getY() - L2World.MAP_MIN_Y) >> 4, locTarget.getZ());
	}

	@Override
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return canSee((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, (tx - L2World.MAP_MIN_X) >> 4, (ty - L2World.MAP_MIN_Y) >> 4, tz);
	}
	
	@Override
	public boolean hasGeo(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		short region = getRegionOffset(gx, gy);
		if (_geodata.contains(region))
			return true;
		return false;
	}

	private static void debug(String debug)
	{
		//_log.info("GeoEngine debug: " + debug);
	}

	public static boolean canSee(Location loc, Location locTarget)
	{
		return canSee(loc.getX(), loc.getY(),  loc.getZ(),  locTarget.getX(), locTarget.getY(), locTarget.getZ());
	}

	public Location findAroundPosition(Location loc, int radiusmin, int radiusmax, int geoIndex)
	{
		return findAroundPosition(loc.getX(), loc.getY(), loc.getZ(), radiusmin, radiusmax, geoIndex);
	}

	public Location findAroundPosition(int x, int y, int z, int radiusmin, int radiusmax, int geoIndex)
	{
		Location pos;
		int tempz;
		for(int i = 0; i < 100; i++)
		{
			pos = RndCoord.coordsRandomize(x, y, z, 0, radiusmin, radiusmax);
			tempz = getHeight(pos.getX(), pos.getY(), pos.getZ());
			pos.setZ(tempz);
			if(canMoveFromToTarget(x, y, z, pos.getX(), pos.getY(), pos.getZ(), geoIndex))
			{
				pos.setZ(tempz);
				return pos;
			}
		}
		return new Location(x, y, z);
	}

	/**
	 * Can see.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @return {@code true} if there is line of sight between the given coordinate sets, {@code false} otherwise
	 */
	private static boolean canSee(int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final double dz = (tz - z);
		final int distance2 = dx * dx + dy * dy;
		
		if (distance2 > 90000) // (300*300) 300*16 = 4800 in world coord
		{
			//Avoid too long check
			return false;
		}
		
		// Increment in Z coordinate when moving along X or Y axis
		// and not straight to the target. This is done because
		// calculation moves either in X or Y direction.
		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		final double inc_z_directionx = dz * dx / (distance2);
		final double inc_z_directiony = dz * dy / (distance2);
		
		// next_* are used in NLOS check from x,y
		int next_x = x;
		int next_y = y;
		
		// creates path to the target
		// calculation stops when next_* == target
		if (dx >= dy)// dy/dx <= 1
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;
			
			for (int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					z += inc_z_directionx;
					if (!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, false))
						return false;
					next_y += inc_y;
					z += inc_z_directiony;
					//_log.warn("1: next_x:"+next_x+" next_y"+next_y);
					if (!nLOS(next_x, y, (int) z, 0, inc_y, inc_z_directiony, false))
						return false;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					//_log.warn("2: next_x:"+next_x+" next_y"+next_y);
					z += inc_z_directionx;
					if (!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, false))
						return false;
				}
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for (int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					z += inc_z_directiony;
					if (!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, false))
						return false;
					next_x += inc_x;
					z += inc_z_directionx;
					debug("3: next_x:"+next_x+" next_y"+next_y);
					if (!nLOS(x, next_y, (int) z, inc_x, 0, inc_z_directionx, false))
						return false;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					debug("4: next_x:"+next_x+" next_y"+next_y);
					z += inc_z_directiony;
					if (!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, false))
						return false;
				}
			}
		}
		return true;
	}
	
	/*
	 * Debug function for checking if there's a line of sight between two coordinates.<br>
	 * Creates points for line of sight check (x,y,z towards target) and<br>
	 * in each point, layer and movement checks are made with NLOS function.<br>
	 * Coordinates here are geodata x,y but z coordinate is world coordinate<br>
	 */
	
	/**
	 * Can see debug.
	 * @param gm the Game Master
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @return {@code true} if the Game Master can see the target target (LOS), {@code false} otherwise
	 */
	private static boolean canSeeDebug(L2PcInstance gm, int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final double dz = (tz - z);
		final int distance2 = dx * dx + dy * dy;
		
		if (distance2 > 90000) // (300*300) 300*16 = 4800 in world coord
		{
			//Avoid too long check
			gm.sendMessage("dist > 300");
			return false;
		}
		// very short checks: 9 => 144 world distance
		// this ensures NLOS function has enough points to calculate,
		// it might not work when distance is small and path vertical
		else if (distance2 < 82)
		{
			// 150 should be too deep/high.
			if (dz * dz > 22500)
			{
				short region = getRegionOffset(x, y);
				// geodata is loaded for region and mobs should have correct Z coordinate...
				// so there would likely be a floor in between the two
				if (_geodata.get(region) != null)
					return false;
			}
			return true;
		}
		
		// Increment in Z coordinate when moving along X or Y axis
		// and not straight to the target. This is done because
		// calculation moves either in X or Y direction.
		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		final double inc_z_directionx = dz * dx / (distance2);
		final double inc_z_directiony = dz * dy / (distance2);
		
		gm.sendMessage("Los: from X: " + x + "Y: " + y + "--->> X: " + tx + " Y: " + ty);
		
		// next_* are used in NLOS check from x,y
		int next_x = x;
		int next_y = y;
		
		// creates path to the target
		// calculation stops when next_* == target
		if (dx >= dy)// dy/dx <= 1
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;
			
			for (int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					z += inc_z_directionx;
					if (!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, true))
						return false;
					next_y += inc_y;
					z += inc_z_directiony;
					//_log.warn("1: next_x:"+next_x+" next_y"+next_y);
					if (!nLOS(next_x, y, (int) z, 0, inc_y, inc_z_directiony, true))
						return false;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					//_log.warn("2: next_x:"+next_x+" next_y"+next_y);
					z += inc_z_directionx;
					if (!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, true))
						return false;
				}
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for (int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					z += inc_z_directiony;
					if (!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, true))
						return false;
					next_x += inc_x;
					z += inc_z_directionx;
					//_log.warn("3: next_x:"+next_x+" next_y"+next_y);
					if (!nLOS(x, next_y, (int) z, inc_x, 0, inc_z_directionx, true))
						return false;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					//_log.warn("4: next_x:"+next_x+" next_y"+next_y);
					z += inc_z_directiony;
					if (!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, true))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Провреяем, есть ли в радиусе от точки недоступные позиции.
	 * @param loc
	 * @param radius
	 * @param geoIndex
	 * @return
	 */
	public boolean cheсkRadius(Location loc, int radius, int geoIndex)
	{
		int dx, dy, dz;
		for (int i = 0; i < 360; i+=30)
		{
			dx = loc.getX() + (int) (radius * Math.cos(Math.toRadians(i)));
			dy = loc.getY() + (int) (radius * Math.sin(Math.toRadians(i)));
			dz = getHeight(dx, dy, loc.getZ());
			boolean canMove = canMoveFromToTarget(loc.getX(), loc.getY(), loc.getZ(), dx, dy, dz, geoIndex);
			if (!canMove)
				return false;
		}
		return true;
	}


	/**
	 * Получение точки, в радиусе которой нет стен.
	 * @param loc
	 * @param radius
	 * @param geoIndex
	 * @return
	 */
	public Location getPointInAvaliableRadius(Location loc, int radius, int geoIndex)
	{
		Location pos;
		int dx, dy, dz;
		for (int i = 360; i >= 0; i--)
		{
			dx = loc.getX() + (int) (radius * Math.cos(Math.toRadians(i)));
			dy = loc.getY() + (int) (radius * Math.sin(Math.toRadians(i)));
			dz = getHeight(dx, dy, loc.getZ());
			pos = new Location(dx, dy, dz);
			if (canMoveFromToTarget(loc.getX(), loc.getY(), loc.getZ(), dx, dy, dz, geoIndex))
				if (cheсkRadius(pos, radius, geoIndex))
					return pos;
		}

		return loc;
	}
	
	/**
	 * Move check.
	 * @param startpoint the start point location
	 * @param destiny the destination location
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @param tz the target's z coordinate
	 * @return the location modified if it didn't pass the geodata checks
	 */
	private static Location moveCheck(Location startpoint, Location destiny, int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final int distance2 = dx * dx + dy * dy;
		
		if (distance2 == 0)
			return destiny;
		if (distance2 > 36100) // 190*190*16 = 3040 world coord
		{
			// Avoid too long check
			// Currently we calculate a middle point
			// for wyvern users and otherwise for comfort
			double divider = Math.sqrt((double) 30000 / distance2);
			tx = x + (int) (divider * dx);
			ty = y + (int) (divider * dy);
			int dz = (tz - startpoint.getZ());
			tz = startpoint.getZ() + (int) (divider * dz);
			dx = (tx - x);
			dy = (ty - y);
			//return startpoint;
		}
		
		// Increment in Z coordinate when moving along X or Y axis
		// and not straight to the target. This is done because
		// calculation moves either in X or Y direction.
		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		
		//gm.sendMessage("MoveCheck: from X: "+x+ "Y: "+y+ "--->> X: "+tx+" Y: "+ty);
		
		// next_* are used in NcanMoveNext check from x,y
		int next_x = x;
		int next_y = y;
		double tempz;
		
		// creates path to the target, using only x or y direction
		// calculation stops when next_* == target
		if (dx >= dy)// dy/dx <= 1
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;
			
			for (int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y);
					if (tempz == java.lang.Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					
					z = tempz;
					next_y += inc_y;
					//_log.warn("2: next_x:"+next_x+" next_y"+next_y);
					tempz = nCanMoveNext(next_x, y, (int) z, next_x, next_y);
					if (tempz == Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					
					z = tempz;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					//_log.warn("3: next_x:"+next_x+" next_y"+next_y);
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y);
					if (tempz == Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					
					z = tempz;
				}
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for (int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if (d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y);
					if (tempz == Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					
					z = tempz;
					next_x += inc_x;
					//_log.warn("5: next_x:"+next_x+" next_y"+next_y);
					tempz = nCanMoveNext(x, next_y, (int) z, next_x, next_y);
					if (tempz == Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);

					z = tempz;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					//_log.warn("6: next_x:"+next_x+" next_y"+next_y);
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y);
					if (tempz == Double.MIN_VALUE)
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					
					z = tempz;
				}
			}
		}
		if (z == startpoint.getZ()) // geodata hasn't modified Z in any coordinate, i.e. doesn't exist
			return destiny;
		
		return new Location(destiny.getX(), destiny.getY(), (int) z);
	}
	
	/**
	 * Sign of parameter x.
	 * @param x the value to verify
	 * @return +1 if x is positive, -1 otherwise
	 */
	private static byte sign(int x)
	{
		if (x >= 0)
			return +1;
		
		return -1;
	}
	
	//GeoEngine
	/**
	 * Initialize the geodata.
	 */
	private static void nInitGeodata()
	{
		final File file = new File("./geodata/geo_index.txt");
		try
		{
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			LineNumberReader lnr = new LineNumberReader(br);
			_log.info("Geo Engine: - Loading Geodata...");
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0)
					continue;
				StringTokenizer st = new StringTokenizer(line, "_");
				byte rx = Byte.parseByte(st.nextToken());
				byte ry = Byte.parseByte(st.nextToken());
				loadGeodataFile(rx, ry);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Read geo_index File.");
		}
		
		try
		{
			File geo_bugs = new File("geodata/geo_bugs.txt");
			
			_geoBugsOut = new BufferedOutputStream(new FileOutputStream(geo_bugs, true));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load geo_bugs.txt File.");
		}
	}
	
	/**
	 * Unload geodata.
	 * @param rx the region x coordinate
	 * @param ry the region y coordinate
	 */
	public static void unloadGeodata(byte rx, byte ry)
	{
		short regionoffset = (short) ((rx << 5) + ry);
		_geodataIndex.remove(regionoffset);
		_geodata.remove(regionoffset);
	}
	
	/**
	 * Load geodata file.
	 * @param rx the region x coordinate
	 * @param ry the region y coordinate
	 * @return {@code true} if successful, {@code false} otherwise
	 */
	public static boolean loadGeodataFile(byte rx, byte ry)
	{
		if (rx < Config.WORLD_X_MIN || rx > Config.WORLD_X_MAX || ry < Config.WORLD_Y_MIN || ry > Config.WORLD_Y_MAX)
		{
			_log.warn("Failed to Load GeoFile: invalid region " + rx +","+ ry + "\n");
			return false;
		}
		String fname = "geodata/" + rx + "_" + ry + ".l2j";
		short regionoffset = (short) ((rx << 5) + ry);
		_log.info("Geo Engine: - Loading: " + fname);
		
		int size, index = 0, block = 0, flor = 0;
		// Create a read-only memory-mapped file
		final File Geo = new File(fname);
		try
		{
			RandomAccessFile raf = new RandomAccessFile(Geo, "r");
			FileChannel roChannel = raf.getChannel();
			size = (int) roChannel.size();
			MappedByteBuffer geo;
			if (Config.FORCE_GEODATA) //Force O/S to Loads this buffer's content into physical memory.
				//it is not guarantee, because the underlying operating system may have paged out some of the buffer's data
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
			else
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			geo.order(ByteOrder.LITTLE_ENDIAN);
			
			if (size > 196608)
			{
				// Indexing geo files, so we will know where each block starts
				IntBuffer indexs = IntBuffer.allocate(65536);
				while (block < 65536)
				{
					byte type = geo.get(index);
					indexs.put(block, index);
					block++;
					index++;
					if (type == 0)
						index += 2; // 1x short
					else if (type == 1)
						index += 128; // 64 x short
					else
					{
						int b;
						for (b = 0; b < 64; b++)
						{
							byte layers = geo.get(index);
							index += (layers << 1) + 1;
							if (layers > flor)
								flor = layers;
						}
					}
				}
				_geodataIndex.put(regionoffset, indexs);
			}
			_geodata.put(regionoffset, geo);

			if (Config.DEBUG)
				_log.info("Geo Engine: - Max Layers: " + flor + " Size: " + size + " Loaded: " + index);
		}
		catch (Exception e)
		{
			_log.warn("Failed to Load GeoFile at block: " + block + " file: " + fname);
			return false;
		}
		return true;
	}
	
	//Geodata Methods
	/**
	 * Gets the region offset.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the region offset
	 */
	private static short getRegionOffset(int x, int y)
	{
		int rx = x >> 11; // =/(256 * 8)
							int ry = y >> 11;
				return (short) (((rx + Config.WORLD_X_MIN) << 5) + (ry + Config.WORLD_Y_MIN));
	}
	
	/**
	 * Gets the block.
	 * @param geo_pos the geo-position
	 * @return Block Index: 0-255
	 */
	private static int getBlock(int geo_pos)
	{
		return (geo_pos >> 3) % 256;
	}
	
	/**
	 * Gets the cell.
	 * @param geo_pos the geo-position
	 * @return Cell Index: 0-7
	 */
	private static int getCell(int geo_pos)
	{
		return geo_pos % 8;
	}
	
	//Geodata Functions
	
	/**
	 * Gets the type.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return Type of geo_block: 0-2
	 */
	private static short nGetType(int x, int y)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current geodata region
		else
			index = idx.get((blockX << 8) + blockY);
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return 0;
		}
		return geo.get(index);
	}
	
	/**
	 * Gets the height.
	 * @param geox the geo-position x coordinate
	 * @param geoy the geo-position y coordinate
	 * @param z the z coordinate
	 * @return the nearest z
	 */
	private static short nGetHeight(int geox, int geoy, int z)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return (short) z;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0)//flat
			return geo.getShort(index);
		else if (type == 1)//complex
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1); //height / 2
			return height;
		}
		else
			//multilevel
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			int offset = (cellX << 3) + cellY;
			while (offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case1), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " "
						+ geoy);
				return (short) z;
			}
			short temph = Short.MIN_VALUE;
			while (layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				if ((z - temph) * (z - temph) > (z - height) * (z - height))
					temph = height;
				layers--;
				index += 2;
			}
			return temph;
		}
	}
	
	/**
	 * Get upper height.
	 * @param geox the geo-position x coordinate
	 * @param geoy the geo-position y coordinate
	 * @param z the z coordinate
	 * @return One layer higher z than parameter z
	 */
	private static short nGetUpperHeight(int geox, int geoy, int z)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		//Geodata without index - it is just empty so index can be calculated on the fly
		final IntBuffer idx = _geodataIndex.get(region);
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return (short) z;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0)//flat
			return geo.getShort(index);
		else if (type == 1)//complex
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1); //height / 2
			return height;
		}
		else
			//multilevel
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			int offset = (cellX << 3) + cellY;
			while (offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case1), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " "
						+ geoy);
				return (short) z;
			}
			short temph = Short.MAX_VALUE;
			while (layers > 0) // from higher to lower
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				if (height < z)
					return temph;
				temph = height;
				layers--;
				index += 2;
			}
			return temph;
		}
	}
	
	/**
	 * Get spawn height.
	 * @param geox the geo-position x coordinate
	 * @param geoy the geo-position y coordinate
	 * @param zmin the minimum z coordinate
	 * @param zmax the maximum z coordinate
	 * @param spawn the spawn
	 * @return a valid height between zmin and zmax
	 */
	private static short nGetSpawnHeight(int geox, int geoy, int zmin, int zmax, L2Object object)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		short temph = Short.MIN_VALUE;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return (short) zmin;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0)//flat
			temph = geo.getShort(index);
		else if (type == 1)//complex
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1); //height / 2
			temph = height;
		}
		else
			//multilevel
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			short height;
			int offset = (cellX << 3) + cellY;
			while (offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte layers = geo.get(index);
			index++;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case2), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " "
						+ geoy);
				return (short) zmin;
			}
			while (layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				if ((zmin - temph) * (zmin - temph) > (zmin - height) * (zmin - height))
					temph = height;
				layers--;
				index += 2;
			}
			if (temph > zmax + 200 || temph < zmin - 200)
			{
				if (Config.DEBUG)
					_log.warn("SpawnHeight Error - Couldnt find correct layer to spawn NPC - GeoData or Spawnlist Bug!: zmin: " + zmin
							+ " zmax: " + zmax + " value: " + temph + " Spawn: " + object.toString() + " at: " + geox + " : " + geoy);
				return (short) zmin;
			}
		}
		if (temph > zmax + 1000 || temph < zmin - 1000)
		{
			if (Config.DEBUG)
				_log.warn("SpawnHeight Error - Spawnlist z value is wrong or GeoData error: zmin: " + zmin + " zmax: " + zmax
						+ " value: " + temph + " Spawn: " + object.toString() + " at: " + geox + " : " + geoy);
			return (short) zmin;
		}
		return temph;
	}
	
	/**
	 * Can move next.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @return {@code true} if character can move to (tx,ty,tz), {@code false} otherwise
	 */
	private static double nCanMoveNext(int x, int y, int z, int tx, int ty)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;
		
		int index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return z;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0) //flat
			return geo.getShort(index);
		else if (type == 1) //complex
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1); //height / 2
			if (checkNSWE(NSWE, x, y, tx, ty))
				return height;
			return Double.MIN_VALUE;
		}
		else
			//multilevel, type == 2
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while (offset > 0) // iterates (too many times?) to get to layer count
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			//_log.warn("layers"+layers);
			index++;
			short height;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case3), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return z;
			}
			short tempz = Short.MIN_VALUE;
			while (layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				
				// searches the closest layer to current z coordinate
				if ((z - tempz) * (z - tempz) > (z - height) * (z - height))
				{
					//layercurr = layers;
					tempz = height;
					NSWE = geo.getShort(index);
					NSWE = (short) (NSWE & 0x0F);
				}
				layers--;
				index += 2;
			}
			if (checkNSWE(NSWE, x, y, tx, ty))
				return tempz;
			return Double.MIN_VALUE;
		}
	}
	
	/**
	 * Line of sight.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param inc_x the inc_x
	 * @param inc_y the inc_y
	 * @param inc_z the inc_z
	 * @param debug if {@code true} there will be debug logs
	 * @return {@code true} if the character can see the target
	 */
	private static boolean nLOS(int x, int y, int z, int inc_x, int inc_y, double inc_z, boolean debug)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;
		
		int index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return true;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0) //flat, movement and sight always possible
		{
			short height = geo.getShort(index);
			debug("flatheight:" + height);
			if (z > height)
				return z+inc_z > height;
			return z+inc_z < height;
		}
		else if (type == 1) //complex
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1); //height / 2
			if (!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
			{
				debug("height:" + height + " z" + z);
				if (z < nGetUpperHeight(x + inc_x, y + inc_y, height))
					return false; // an obstacle high enough
			}
			return true;
		}
		else
			//multilevel, type == 2
		{
			cellX = getCell(x);
			cellY = getCell(y);
			
			int offset = (cellX << 3) + cellY;
			while (offset > 0) // iterates (too many times?) to get to layer count
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			
			index++;
			short tempZ;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case4), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return false;
			}
			short upperHeight = Short.MAX_VALUE; // big positive value
			short lowerHeight = Short.MIN_VALUE; // big negative value
			byte temp_layers = layers;
			boolean highestlayer = true;
			while (temp_layers > 0) // from higher to lower
			{
				// reads tempZ for current layer, result in world z coordinate
				tempZ = geo.getShort(index);
				tempZ = (short) (tempZ & 0x0fff0);
				tempZ = (short) (tempZ >> 1); //tempZ / 2
				
				if (z > tempZ)
				{
					lowerHeight = tempZ;
					NSWE = geo.getShort(index);
					NSWE = (short) (NSWE & 0x0F);
					break;
				}
				
				highestlayer = false;
				upperHeight = tempZ;
				
				temp_layers--;
				index += 2;
			}
			debug("z:" + z + " x: " + cellX + " y:" + cellY + " la " + layers + " lo:" + lowerHeight + " up:" + upperHeight);
			// Check if LOS goes under a layer/floor
			// clearly under layer but not too much under
			// lower height here only for geodata bug checking, layers very close? maybe could be removed
			if ((z - upperHeight) < -10 && (z - upperHeight) > inc_z - 20 && (z - lowerHeight) > 40)
			{
				debug("false, incz" + inc_z);
				return false;
			}
			
			// or there's a fence/wall ahead when we're not on highest layer
			if (!highestlayer)
			{
				//a probable wall, there's movement block and layers above you
				if (!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y)) // cannot move
				{
					debug("block and next in x" + inc_x + " y" + inc_y + " is:"
								+ nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight));
					// check one inc_x inc_y further, for the height there
					if (z < nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight))
						return false; // a wall
				}
				return true;
			}
			if (!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
			{
				// check one inc_x inc_y further, for the height there
				if (z < nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight))
					return false; // we hit an obstacle high enough
			}
			return true;
		}
	}
	
	/**
	 * Get the NSWE for the given coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return NSWE: 0-15
	 */
	private static short nGetNSWE(int x, int y, int z)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;
		
		int index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return 15;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0)//flat
			return 15;
		else if (type == 1)//complex
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
		}
		else
			//multilevel
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while (offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case5), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return 15;
			}
			short tempz = Short.MIN_VALUE;
			while (layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				
				if ((z - tempz) * (z - tempz) > (z - height) * (z - height))
				{
					tempz = height;
					NSWE = geo.get(index);
					NSWE = (short) (NSWE & 0x0F);
				}
				layers--;
				index += 2;
			}
		}
		return NSWE;
	}
	
	/**
	 * Gets the height and NSWE.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return array [0] - height, [1] - NSWE
	 */
	@Override
	public short getHeightAndNSWE(int x, int y, int z)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;

		int index;
		final IntBuffer idx = _geodataIndex.get(region);
		//Geodata without index - it is just empty so index can be calculated on the fly
		if (idx == null)
			index = ((blockX << 8) + blockY) * 3;
		//Get Index for current block of current region geodata
		else
			index = idx.get(((blockX << 8)) + (blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = _geodata.get(region);
		if (geo == null)
		{
			if (Config.DEBUG)
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			return (short)((z << 1) | NSWE_ALL);
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
		if (type == 0)//flat
			return (short)((geo.getShort(index) << 1) | NSWE_ALL);
		else if (type == 1)//complex
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			return geo.getShort(index);
		}
		else
			//multilevel
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while (offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height;
			if (layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case1), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return (short)((z << 1) | NSWE_ALL);
			}
			short temph = Short.MIN_VALUE;
			short result = 0;
			while (layers > 0)
			{
				short block = geo.getShort(index);
				height = (short) (block & 0x0fff0);
				height = (short) (height >> 1); //height / 2
				if ((z - temph) * (z - temph) > (z - height) * (z - height))
				{
					temph = height;
					result = block;
				}
				layers--;
				index += 2;
			}
			return result;
		}
	}
	
	/**
	 * Check NSWE.
	 * @param NSWE the NSWE
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param tx the target's x coordinate
	 * @param ty the target's y coordinate
	 * @return {@code true} if NSWE don't block given direction, {@code false} otherwise
	 */
	private static boolean checkNSWE(short NSWE, int x, int y, int tx, int ty)
	{
		//Check NSWE
		if (NSWE == 15)
			return true;
		if (tx > x)//E
		{
			if ((NSWE & EAST) == 0)
				return false;
		}
		else if (tx < x)//W
		{
			if ((NSWE & WEST) == 0)
				return false;
		}
		if (ty > y)//S
		{
			if ((NSWE & SOUTH) == 0)
				return false;
		}
		else if (ty < y)//N
		{
			if ((NSWE & NORTH) == 0)
				return false;
		}
		return true;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine _instance = new GeoEngine();
	}
}
