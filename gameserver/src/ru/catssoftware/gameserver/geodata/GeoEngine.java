
package ru.catssoftware.gameserver.geodata;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.geodata.pathfinding.CellPathFinding;
import ru.catssoftware.gameserver.geodata.pathfinding.Node;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FortSiegeGuardInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeGuardInstance;
import ru.catssoftware.tools.geometry.Point3D;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

/**
 * @author -Nemesiss-hhh
 */

public class GeoEngine extends GeoData
{
	private static final Logger 			_log			= Logger.getLogger(GeoEngine.class);

	private static GeoEngine _instance;
	private final static byte _e = 1;
	private final static byte _w = 2;
	private final static byte _s = 4;
	private final static byte _n = 8;
	private static Map<Short, MappedByteBuffer> _geodata = new FastMap<Short, MappedByteBuffer>();
	private static Map<Short, IntBuffer> _geodataIndex = new FastMap<Short, IntBuffer>();
	private static BufferedOutputStream _geoBugsOut;

	public static GeoEngine getInstance()
	{
		if(_instance == null)
			_instance = new GeoEngine();
		return _instance;
	}

	public GeoEngine()
	{
		_log.info("Geo Engine: Model: Lucera");
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
			return nGetHeight((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z);
		}

		@Override
		public short getSpawnHeight(int x, int y, int zmin, int zmax, int spawnid)
		{
			return nGetSpawnHeight((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,zmin,zmax,spawnid);
		}

		@Override
		public String geoPosition(int x, int y)
		{
			int gx = (x - L2World.MAP_MIN_X) >> 4;
			int gy = (y - L2World.MAP_MIN_Y) >> 4;
			return "bx: "+getBlock(gx)+" by: "+getBlock(gy)+" cx: "+getCell(gx)+" cy: "+getCell(gy)+"  region offset: "+getRegionOffset(gx,gy);
		}

		@Override
		public boolean canSeeTarget(L2Object cha, Point3D target, int instance)
		{
			if (DoorTable.getInstance().checkIfDoorsBetween(cha.getX(),cha.getY(),cha.getZ(),target.getX(),target.getY(),target.getZ(),instance))
				return false;
	
			if(cha.getZ() >= target.getZ())
				return canSeeTarget(cha.getX(),cha.getY(),cha.getZ(),target.getX(),target.getY(),target.getZ());
			else
				return canSeeTarget(target.getX(),target.getY(),target.getZ(), cha.getX(),cha.getY(),cha.getZ());
		}

		@Override
		public boolean canSeeTarget(L2Object cha, L2Object target, int instance)
		{
			int z = cha.getZ() + 45;
			int z2 = target.getZ() + 45;

			if (cha instanceof L2SiegeGuardInstance || cha instanceof L2FortSiegeGuardInstance)
				z += 30;

	
			if (target instanceof L2DoorInstance)
				return true;
		
			if(DoorTable.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), z, target.getX(), target.getY(), z2, instance))
				return false;
			if (target instanceof L2SiegeGuardInstance || target instanceof L2FortSiegeGuardInstance)
				z2 += 30; // well they don't move closer to balcony fence at the moment :(
			if (cha.getZ() >= target.getZ())
				return canSeeTarget(cha.getX(),cha.getY(),z,target.getX(),target.getY(),z2);
			else
				return canSeeTarget(target.getX(),target.getY(),z2, cha.getX(),cha.getY(),z);
		}

		@Override
		public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
		{
			int z = gm.getZ() + 45;
			int z2 = target.getZ() + 45;

			if (target instanceof L2DoorInstance)
			{
				gm.sendMessage("door always true");
				return true;
			}

			if (gm.getZ() >= target.getZ())
				return canSeeDebug(gm,(gm.getX() - L2World.MAP_MIN_X) >> 4,(gm.getY() - L2World.MAP_MIN_Y) >> 4,z,(target.getX() - L2World.MAP_MIN_X) >> 4,(target.getY() - L2World.MAP_MIN_Y) >> 4,z2);
			else
				return canSeeDebug(gm,(target.getX() - L2World.MAP_MIN_X) >> 4,(target.getY() - L2World.MAP_MIN_Y) >> 4,z2,(gm.getX() - L2World.MAP_MIN_X) >> 4,(gm.getY() - L2World.MAP_MIN_Y) >> 4,z);
		}

		@Override
		public short getNSWE(int x, int y, int z)
		{
			return nGetNSWE((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z);
		}

		@Override
		public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz,int instance)
		{
			Location destiny = moveCheck(x,y,z,tx,ty,tz,instance);
			return (destiny.getX() == tx && destiny.getY() == ty && tz == destiny.getZ());
		}

		@Override
		public Location moveCheck(int x, int y, int z, int tx, int ty, int tz, int instance)
		{
			Location startpoint = new Location(x,y,z);

			if (DoorTable.getInstance().checkIfDoorsBetween(x,y,z,tx,ty,tz,instance)) 
				return startpoint;

			Location destiny = new Location(tx,ty,tz);
			return moveCheck(startpoint, destiny,(x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z,(tx - L2World.MAP_MIN_X) >> 4,(ty - L2World.MAP_MIN_Y) >> 4,tz);
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
			int rx = (gx >> 11) + 15;
			int ry = (gy >> 11) + 10;
			String out = rx+";"+ry+";"+bx+";"+by+";"+cx+";"+cy+";"+gm.getZ()+";"+comment+"\n";
			try
			{
				_geoBugsOut.write(out.getBytes());
				_geoBugsOut.flush();
				gm.sendMessage("GeoData bug saved!");
			} catch (Exception e) {
				e.printStackTrace();
				gm.sendMessage("GeoData bug save Failed!");
			}
		}

		@Override
		public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
		{
			return canSee((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z,(tx - L2World.MAP_MIN_X) >> 4,(ty - L2World.MAP_MIN_Y) >> 4,tz);
		}

		@Override
		public boolean hasGeo(int x, int y)
		{
			int gx = (x - L2World.MAP_MIN_X) >> 4;
			int gy = (y - L2World.MAP_MIN_Y) >> 4;
			short region = getRegionOffset(gx,gy);
			return _geodata.get(region) != null;
		}

		private static boolean canSee(int x, int y, double z, int tx, int ty, int tz)
		{
			int dx = (tx - x);
			int dy = (ty - y);
			final double dz = (tz - z);
			final int distance2 = dx*dx+dy*dy;

			if (distance2 > 90000)
			{
				return false;
			}
			else if (distance2 < 82)
			{
				if(dz*dz > 22500)
				{
					short region = getRegionOffset(x,y);
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
			final double inc_z_directionx = dz*dx / (distance2);
			final double inc_z_directiony = dz*dy / (distance2);

			// next_* are used in NLOS check from x,y
			int next_x = x;
			int next_y = y;

			// creates path to the target
			// calculation stops when next_* == target
			if (dx >= dy)// dy/dx <= 1
			{
				int delta_A = 2*dy;
				int d = delta_A - dx;
				int delta_B = delta_A - 2*dx;

				for (int i = 0; i < dx; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_x += inc_x;
						z += inc_z_directionx;
						if (!nLOS(x,y,(int)z,inc_x,0,inc_z_directionx,tz,false))
							return false;
						next_y += inc_y;
						z += inc_z_directiony;
						//_log.info("1: next_x:"+next_x+" next_y"+next_y);
						if (!nLOS(next_x,y,(int)z,0,inc_y,inc_z_directiony,tz,false))
							return false;
					}
					else
					{
						d += delta_A;
						next_x += inc_x;
						//_log.info("2: next_x:"+next_x+" next_y"+next_y);
						z += inc_z_directionx;
						if (!nLOS(x,y,(int)z,inc_x,0,inc_z_directionx,tz,false))
							return false;
					}
				}
			}
			else
			{
				int delta_A = 2*dx;
				int d = delta_A - dy;
				int delta_B = delta_A - 2*dy;
				for (int i = 0; i < dy; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_y += inc_y;
						z += inc_z_directiony;
						if (!nLOS(x,y,(int)z,0,inc_y,inc_z_directiony,tz,false))
							return false;
						next_x += inc_x;
						z += inc_z_directionx;
						//_log.info("3: next_x:"+next_x+" next_y"+next_y);
						if (!nLOS(x,next_y,(int)z,inc_x,0,inc_z_directionx,tz,false))
							return false;
					}
					else
					{
						d += delta_A;
						next_y += inc_y;
						//_log.warning("4: next_x:"+next_x+" next_y"+next_y);
						z += inc_z_directiony;
						if (!nLOS(x,y,(int)z,0,inc_y,inc_z_directiony,tz,false))
							return false;
					}
				}
			}
			return true;
		}
		/*
		 * Debug function for checking if there's a line of sight between
		 * two coordinates.
		 *
		 * Creates points for line of sight check (x,y,z towards target) and
		 * in each point, layer and movement checks are made with NLOS function.
		 *
		 * Coordinates here are geodata x,y but z coordinate is world coordinate
		 */
		private static boolean canSeeDebug(L2PcInstance gm, int x, int y, double z, int tx, int ty, int tz)
		{
			int dx = (tx - x);
			int dy = (ty - y);
			final double dz = (tz - z);
			final int distance2 = dx*dx+dy*dy;

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
				if(dz*dz > 22500)
				{
					short region = getRegionOffset(x,y);
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
			final double inc_z_directionx = dz*dx / (distance2);
			final double inc_z_directiony = dz*dy / (distance2);

			gm.sendMessage("Los: from X: "+x+ "Y: "+y+ "--->> X: "+tx+" Y: "+ty);

			// next_* are used in NLOS check from x,y
			int next_x = x;
			int next_y = y;

			// creates path to the target
			// calculation stops when next_* == target
			if (dx >= dy)// dy/dx <= 1
			{
				int delta_A = 2*dy;
				int d = delta_A - dx;
				int delta_B = delta_A - 2*dx;

				for (int i = 0; i < dx; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_x += inc_x;
						z += inc_z_directionx;
						if (!nLOS(x,y,(int)z,inc_x,0,inc_z_directionx,tz,true))
							return false;
						next_y += inc_y;
						z += inc_z_directiony;
						//_log.warning("1: next_x:"+next_x+" next_y"+next_y);
						if (!nLOS(next_x,y,(int)z,0,inc_y,inc_z_directiony,tz,true))
							return false;
					}
					else
					{
						d += delta_A;
						next_x += inc_x;
						//_log.warning("2: next_x:"+next_x+" next_y"+next_y);
						z += inc_z_directionx;
						if (!nLOS(x,y,(int)z,inc_x,0,inc_z_directionx,tz,true))
							return false;
					}
				}
			}
			else
			{
				int delta_A = 2*dx;
				int d = delta_A - dy;
				int delta_B = delta_A - 2*dy;
				for (int i = 0; i < dy; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_y += inc_y;
						z += inc_z_directiony;
						if (!nLOS(x,y,(int)z,0,inc_y,inc_z_directiony,tz,true))
							return false;
						next_x += inc_x;
						z += inc_z_directionx;
						//_log.warning("3: next_x:"+next_x+" next_y"+next_y);
						if (!nLOS(x,next_y,(int)z,inc_x,0,inc_z_directionx,tz,true))
							return false;
					}
					else
					{
						d += delta_A;
						next_y += inc_y;
						//_log.warning("4: next_x:"+next_x+" next_y"+next_y);
						z += inc_z_directiony;
						if (!nLOS(x,y,(int)z,0,inc_y,inc_z_directiony,tz,true))
							return false;
					}
				}
			}
			return true;
		}
		/*
		 *  MoveCheck
		 */
		private static Location moveCheck(Location startpoint, Location destiny, int x, int y, double z, int tx, int ty, int tz)
		{
			int dx = (tx - x);
			int dy = (ty - y);
			final int distance2 = dx*dx+dy*dy;

			if (distance2 == 0)
				return destiny;
			if (distance2 > 36100) // 190*190*16 = 3040 world coord
			{
				// Avoid too long check
				// Currently we calculate a middle point
				// for wyvern users and otherwise for comfort
				double divider = Math.sqrt((double)30000/distance2);
				tx = x + (int)(divider * dx);
				ty = y + (int)(divider * dy);
				int dz = (tz - startpoint.getZ());
				tz = startpoint.getZ() + (int)(divider * dz);
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
			double tempz = z;

			// creates path to the target, using only x or y direction
			// calculation stops when next_* == target
			if (dx >= dy)// dy/dx <= 1
			{
				int delta_A = 2*dy;
				int d = delta_A - dx;
				int delta_B = delta_A - 2*dx;

				for (int i = 0; i < dx; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_x += inc_x;
						tempz = nCanMoveNext(x,y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
						next_y += inc_y;
						//_log.warning("2: next_x:"+next_x+" next_y"+next_y);
						tempz = nCanMoveNext(next_x,y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
					}
					else
					{
						d += delta_A;
						next_x += inc_x;
						//_log.warning("3: next_x:"+next_x+" next_y"+next_y);
						tempz = nCanMoveNext(x,y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
					}
				}
			}
			else
			{
				int delta_A = 2*dx;
				int d = delta_A - dy;
				int delta_B = delta_A - 2*dy;
				for (int i = 0; i < dy; i++)
				{
					x = next_x;
					y = next_y;
					if (d > 0)
					{
						d += delta_B;
						next_y += inc_y;
						tempz = nCanMoveNext(x,y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
						next_x += inc_x;
						//_log.warning("5: next_x:"+next_x+" next_y"+next_y);
						tempz = nCanMoveNext(x,next_y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
					}
					else
					{
						d += delta_A;
						next_y += inc_y;
						//_log.warning("6: next_x:"+next_x+" next_y"+next_y);
						tempz = nCanMoveNext(x,y,(int)z,next_x,next_y,tz);
						if (tempz == Double.MIN_VALUE)
							return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
						else z = tempz;
					}
				}
			}
			if (z == startpoint.getZ()) // geodata hasn't modified Z in any coordinate, i.e. doesn't exist
				return destiny;
			else
				return new Location(destiny.getX(),destiny.getY(),(int)z);
		}

		private static byte sign(int x)
		{
			if (x >= 0)
				return +1;
			else
				return -1;
		}

		//GeoEngine
		private static void nInitGeodata()
		{
			_log.info("Geo Engine: - Loading Geodata...");
			int fCnt=0;
			File geodir = new File(Config.GEODATA_ROOT,"/geodata");
			if(geodir.isDirectory()) {
				for(File f : geodir.listFiles()) {
					if(f.getName().endsWith(".l2j")) try {
						
						String []coords = f.getName().replace(".l2j", "").split("_");
						loadGeodataFile(Byte.parseByte(coords[0]), Byte.parseByte(coords[1]));
						fCnt++;
					} catch(Exception e) {
						_log.warn("Geo Engine: Error while loading "+f.getName(),e);
					}
				}
			}
			_log. info("Geo Engine: - Total Loaded: "+fCnt+" regions");			
			try
			{
				File geo_bugs = new File(Config.GEODATA_ROOT,"/geodata/geo_bugs.txt");
				_geoBugsOut = new BufferedOutputStream(new FileOutputStream(geo_bugs,true));
			} catch (Exception e) {
				_geoBugsOut = new BufferedOutputStream(new ByteArrayOutputStream());
				_log.warn("GeoEngine: Failed to Load geo_bugs.txt File.");
			}		
		}
		public static void unloadGeodata(byte rx, byte ry)
		{
			short regionoffset = (short)((rx << 5) + ry);
			_geodataIndex.remove(regionoffset);
			_geodata.remove(regionoffset);
		}
		public static boolean loadGeodataFile(byte rx, byte ry)
		{
			String fname = Config.GEODATA_ROOT + "/geodata/"+rx+"_"+ry+".l2j";
			short regionoffset = (short)((rx << 5) + ry);
			File Geo = new File(fname);
			int size, index = 0, block = 0, flor = 0;
			FileChannel roChannel = null;
			
			try {
				// Create a read-only memory-mapped file
				roChannel = new RandomAccessFile(Geo, "r").getChannel();
				size = (int)roChannel.size();
				MappedByteBuffer geo;
				if (Config.FORCE_GEODATA) //Force O/S to Loads this buffer's content into physical memory.
					//it is not guarantee, because the underlying operating system may have paged out some of the buffer's data
					geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
				else
					geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
				geo.order(ByteOrder.LITTLE_ENDIAN);
				int sign = geo.get();
				boolean encrypted = sign==0xF97971;
				if(!encrypted)
					geo.position(0);
				if (size > 196608)
				{
					// Indexing geo files, so we will know where each block starts
					IntBuffer indexs = IntBuffer.allocate(65536);
					while(block < 65536)
					{
						byte type = geo.get(index);
						indexs.put(block,index);
						block++;
						index++;
						if(type == 0)
							index += 2; // 1x short
						else if(type == 1)
							index += 128; // 64 x short
						else
						{
							int b;
							for(b=0;b<64;b++)
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
				_geodata.put(regionoffset,geo);

			} catch (Exception e)
			{
				e.printStackTrace();
				_log.warn("Failed to Load GeoFile at block: "+block+"\n");
				return false;
			}
			finally
			{
				try
				{
					roChannel.close();
				}
				catch (Exception e)
				{
				}
			}
			return true;
		}

		//Geodata Methods
		/**
		 * @param x
		 * @param y
		 * @return Region Offset
		 */
		private static short getRegionOffset(int x, int y)
		{
			int rx = x >> 11; // =/(256 * 8)
			int ry = y >> 11;
			return (short)(((rx+15) << 5) + (ry+10));
		}

		/**
		 * @param pos
		 * @return Block Index: 0-255
		 */
		private  static int getBlock(int geo_pos)
		{
			return (geo_pos >> 3) % 256;
		}

		/**
		 * @param pos
		 * @return Cell Index: 0-7
		 */
		private static int getCell(int geo_pos)
		{
			return geo_pos % 8;
		}

		//Geodata Functions

		/**
		 * @param x
		 * @param y
		 * @return Type of geo_block: 0-2
		 */
		private static short nGetType(int x, int y)
		{
			short region = getRegionOffset(x,y);
			int blockX = getBlock(x);
			int blockY = getBlock(y);
			int index = 0;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current geodata region
			else index = _geodataIndex.get(region).get((blockX << 8) + blockY);
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return 0;
			}
			return geo.get(index);
		}
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @return Nearest Z
		 */
		private static short nGetHeight(int geox, int geoy, int z)
		{
			short region = getRegionOffset(geox,geoy);
			int blockX = getBlock(geox);
			int blockY = getBlock(geoy);
			int cellX, cellY, index;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return (short)z;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
				return geo.getShort(index);
			else if(type == 1)//complex
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				return height;
			}
			else //multilevel
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				index++;
				short height=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case1), region: "+region+" - invalid layer count: "+layers+" at: "+geox+" "+geoy);
					return (short)z;
				}
				short temph = Short.MIN_VALUE;
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2
					if ((z-temph)*(z-temph) > (z-height)*(z-height))
						temph = height;
					layers--;
					index += 2;
				}
			 return temph;
			}
		}
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @return One layer higher Z than parameter Z
		 */
		private static short nGetUpperHeight(int geox, int geoy, int z)
		{
			short region = getRegionOffset(geox,geoy);
			int blockX = getBlock(geox);
			int blockY = getBlock(geoy);
			int cellX, cellY, index;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return (short)z;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
				return geo.getShort(index);
			else if(type == 1)//complex
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				return height;
			}
			else //multilevel
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				index++;
				short height=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case1), region: "+region+" - invalid layer count: "+layers+" at: "+geox+" "+geoy);
					return (short)z;
				}
				short temph = Short.MAX_VALUE;
				while(layers > 0) // from higher to lower
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2
					if (height < z) return temph;
					temph = height;
					layers--;
					index += 2;
				}
				return temph;
			}
		}
		
		/**
		 * @param x
		 * @param y
		 * @param zmin
		 * @param zmax
		 * @return Z betwen zmin and zmax
		 */
		private static short nGetSpawnHeight(int geox, int geoy, int zmin, int zmax, int spawnid)
		{
			short region = getRegionOffset(geox,geoy);
			int blockX = getBlock(geox);
			int blockY = getBlock(geoy);
			int cellX, cellY, index;
			short temph = Short.MIN_VALUE;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return (short)zmin;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
				temph = geo.getShort(index);
			else if(type == 1)//complex
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				temph = height;
			}
			else//multilevel
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				short height;
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				//Read current block type: 0-flat,1-complex,2-multilevel
				byte layers = geo.get(index);
				index++;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case2), region: "+region+" - invalid layer count: "+layers+" at: "+geox+" "+geoy);
					return (short)zmin;
				}
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2
					if ((zmin-temph)*(zmin-temph) > (zmin-height)*(zmin-height))
						temph = height;
					layers--;
					index += 2;
				}
				if (temph > zmax + 200 || temph < zmin - 200)
				{
					if(_log.isDebugEnabled())
						_log.warn("SpawnHeight Error - Couldnt find correct layer to spawn NPC - GeoData or Spawnlist Bug!: zmin: "+zmin+" zmax: "+zmax+" value: "+temph+" SpawnId: "+spawnid+" at: "+geox+" : "+geoy);
					return (short)zmin;
				}
			}
			if (temph > zmax + 1000 || temph < zmin - 1000)
			{
				if(_log.isDebugEnabled())
					_log.warn("SpawnHeight Error - Spawnlist z value is wrong or GeoData error: zmin: "+zmin+" zmax: "+zmax+" value: "+temph+" SpawnId: "+spawnid+" at: "+geox+" : "+geoy);
				return (short)zmin;
			}
			return temph;
		}
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @param tx
		 * @param ty
		 * @param tz
		 * @return True if char can move to (tx,ty,tz)
		 */
		private static double nCanMoveNext(int x, int y, int z, int tx, int ty, int tz)
		{
			short region = getRegionOffset(x,y);
			int blockX = getBlock(x);
			int blockY = getBlock(y);
			int cellX, cellY;
			short NSWE = 0;

			int index = 0;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return z;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0) //flat
				return geo.getShort(index);
			else if(type == 1) //complex
			{
				cellX = getCell(x);
				cellY = getCell(y);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				NSWE = (short)(height&0x0F);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				if(checkNSWE(NSWE,x,y,tx,ty)) return height;
				else return Double.MIN_VALUE;
			}
			else //multilevel, type == 2
			{
				cellX = getCell(x);
				cellY = getCell(y);
				int offset = (cellX << 3) + cellY;
				while(offset > 0) // iterates (too many times?) to get to layer count
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				//_log.warn("layers"+layers);
				index++;
				short height=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case3), region: "+region+" - invalid layer count: "+layers+" at: "+x+" "+y);
					return z;
				}
				short tempz = Short.MIN_VALUE;
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2

					// searches the closest layer to current z coordinate
					if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
					{
						//layercurr = layers;
						tempz = height;
						NSWE = geo.getShort(index);
						NSWE = (short)(NSWE&0x0F);
					}
					layers--;
					index += 2;
				}
				if(checkNSWE(NSWE,x,y,tx,ty)) return tempz;
				else return Double.MIN_VALUE;
			}
		}
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @param inc_x
		 * @param inc_y
		 * @param tz
		 * @return True if Char can see target
		 */
		private static boolean nLOS(int x, int y, int z, int inc_x, int inc_y, double inc_z, int tz, boolean debug)
		{
			short region = getRegionOffset(x,y);
			int blockX = getBlock(x);
			int blockY = getBlock(y);
			int cellX, cellY;
			short NSWE = 0;

			int index;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return true;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0) //flat, movement and sight always possible
			{
		    	short height = geo.getShort(index);
		    	if (debug)
		    		_log.warn("flatheight:" + height);
		    	if (z > height)
		    		return inc_z > height;
		    	else
		    		return inc_z < height;
			}
			else if(type == 1) //complex
			{
				cellX = getCell(x);
				cellY = getCell(y);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				NSWE = (short)(height&0x0F);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				if (!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
				{
					if (debug)
						_log.warn("height:" + height + " z" + z);	

					return z >= nGetUpperHeight(x + inc_x, y + inc_y, height);
				}
				else
					return true;
			}
			else //multilevel, type == 2
			{
				cellX = getCell(x);
				cellY = getCell(y);

				int offset = (cellX << 3) + cellY;
				while(offset > 0) // iterates (too many times?) to get to layer count
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);

				index++;
				short tempZ=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case4), region: "+region+" - invalid layer count: "+layers+" at: "+x+" "+y);
					return false;
				}
				short upperHeight = Short.MAX_VALUE; // big positive value
				short lowerHeight = Short.MIN_VALUE; // big negative value
				byte temp_layers = layers;
				boolean highestlayer = false;
				while(temp_layers > 0) // from higher to lower
				{
					// reads tempZ for current layer, result in world z coordinate
					tempZ = geo.getShort(index);
					tempZ = (short)(tempZ&0x0fff0);
					tempZ = (short)(tempZ >> 1); //tempZ / 2

					if (z > tempZ) {
						lowerHeight = tempZ;
						NSWE = geo.getShort(index);
						NSWE = (short)(NSWE&0x0F);
						highestlayer = true;
						break;
					}
					else {
						highestlayer = false;
						upperHeight = tempZ;
					}
					
					temp_layers--;
					index += 2;
				}
				if(debug) _log.warn("z:"+z+" x: "+cellX+" y:"+cellY+" la "+layers+" lo:"+lowerHeight+" up:"+upperHeight);
				// Check if LOS goes under a layer/floor
				// clearly under layer but not too much under 
				// lowerheight here only for geodata bug checking, layers very close? maybe could be removed
				if((z-upperHeight) < -10 && (z-upperHeight) > inc_z-10 && (z-lowerHeight) > 40) {
					if (debug) _log.warn("false, incz"+inc_z);
					return false; 
				}
				
				// or there's a fence/wall ahead when we're not on highest layer
				if(!highestlayer)
				{
					//a probable wall, there's movement block and layers above you
					if(!checkNSWE(NSWE,x,y,x+inc_x,y+inc_y)) // cannot move
					{
						if(debug) _log.warn("block and next in x"+inc_x+" y"+inc_y+" is:"+nGetUpperHeight(x+inc_x,y+inc_y,lowerHeight));
						// check one inc_x inc_y further, for the height there

						return z >= nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight);
					}
					else return true;
				}
				return checkNSWE(NSWE, x, y, x + inc_x, y + inc_y) || z >= nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight);
			}
		}
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @return NSWE: 0-15
		 */
		private short nGetNSWE(int x, int y, int z)
		{
			short region = getRegionOffset(x,y);
			int blockX = getBlock(x);
			int blockY = getBlock(y);
			int cellX, cellY;
			short NSWE = 0;

			int index = 0;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return 15;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
				return 15;
			else if(type == 1)//complex
			{
				cellX = getCell(x);
				cellY = getCell(y);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				NSWE = (short)(height&0x0F);
			}
			else//multilevel
			{
				cellX = getCell(x);
				cellY = getCell(y);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				index++;
				short height=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case5), region: "+region+" - invalid layer count: "+layers+" at: "+x+" "+y);
					return 15;
				}
				short tempz = Short.MIN_VALUE;
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2

					if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
					{
						tempz = height;
						NSWE = geo.get(index);
						NSWE = (short)(NSWE&0x0F);
					}
					layers--;
					index += 2;
				}
			}
			return NSWE;
		}
		
		/**
		 * @param x
		 * @param y
		 * @param z
		 * @return NSWE: 0-15
		 */
		@Override
		public Node[] getNeighbors(Node n)
		{
			List<Node> Neighbors = new FastList<Node>(4);
			Node newNode;
			int x = n.getLoc().getNodeX();
			int y = n.getLoc().getNodeY();
			int parentdirection = 0;
			if (n.getParent() != null) // check for not adding parent again
			{
				if (n.getParent().getLoc().getNodeX() > x) parentdirection = 1;
				if (n.getParent().getLoc().getNodeX() < x) parentdirection = -1;
				if (n.getParent().getLoc().getNodeY() > y) parentdirection = 2;
				if (n.getParent().getLoc().getNodeY() < y) parentdirection = -2;
			}
			short z = n.getLoc().getZ();
			short region = getRegionOffset(x,y);
			int blockX = getBlock(x);
			int blockY = getBlock(y);
			int cellX, cellY;
			short NSWE = 0;
			int index = 0;
			//Geodata without index - it is just empty so index can be calculated on the fly
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			//Get Index for current block of current region geodata
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			//Buffer that Contains current Region GeoData
			ByteBuffer geo = _geodata.get(region);
			if(geo == null)
			{
				if(_log.isDebugEnabled())
					_log.warn("Geo Region - Region Offset: "+region+" dosnt exist!!");
				return null;
			}
			//Read current block type: 0-flat,1-complex,2-multilevel
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
			{
				short height = geo.getShort(index);
				n.getLoc().setZ(height);
				if (parentdirection != 1) {
					newNode = CellPathFinding.getInstance().readNode(x+1,y,height); 
					//newNode.setCost(0);
					Neighbors.add(newNode);
				}
				if (parentdirection != 2) {
					newNode = CellPathFinding.getInstance().readNode(x,y+1,height); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -2) {
					newNode = CellPathFinding.getInstance().readNode(x,y-1,height); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -1) {
					newNode = CellPathFinding.getInstance().readNode(x-1,y,height); 
					Neighbors.add(newNode);
				}
			}
			else if(type == 1)//complex
			{
				cellX = getCell(x);
				cellY = getCell(y);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				NSWE = (short)(height&0x0F);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				n.getLoc().setZ(height);
				if (NSWE != 15 && parentdirection != 0) return null; // no node with a block will be used
				if (parentdirection != 1 && checkNSWE(NSWE,x,y,x+1,y)) 
				{
					newNode = CellPathFinding.getInstance().readNode(x+1,y,height); 
					//newNode.setCost(basecost+50);
					Neighbors.add(newNode);
				}
				if (parentdirection != 2 && checkNSWE(NSWE,x,y,x,y+1))
				{
					newNode = CellPathFinding.getInstance().readNode(x,y+1,height); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -2 && checkNSWE(NSWE,x,y,x,y-1))
				{
					newNode = CellPathFinding.getInstance().readNode(x,y-1,height); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -1 && checkNSWE(NSWE,x,y,x-1,y)) 
				{
					newNode = CellPathFinding.getInstance().readNode(x-1,y,height); 
					Neighbors.add(newNode);
				}
			}
			else//multilevel
			{
				cellX = getCell(x);
				cellY = getCell(y);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				index++;
				short height=-1;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case5), region: "+region+" - invalid layer count: "+layers+" at: "+x+" "+y);
					return null;
				}
				short tempz = Short.MIN_VALUE;
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2

					if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
					{
						tempz = height;
						NSWE = geo.get(index);
						NSWE = (short)(NSWE&0x0F);
					}
					layers--;
					index += 2;
				}
				n.getLoc().setZ(tempz);
				if (NSWE != 15 && parentdirection != 0) return null; // no node with a block will be used
				if (parentdirection != 1 && checkNSWE(NSWE,x,y,x+1,y)) 
				{
					newNode = CellPathFinding.getInstance().readNode(x+1,y,tempz); 
					//newNode.setCost(basecost+50);
					Neighbors.add(newNode);
				}
				if (parentdirection != 2 && checkNSWE(NSWE,x,y,x,y+1))
				{
					newNode = CellPathFinding.getInstance().readNode(x,y+1,tempz); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -2 && checkNSWE(NSWE,x,y,x,y-1))
				{
					newNode = CellPathFinding.getInstance().readNode(x,y-1,tempz); 
					Neighbors.add(newNode);
				}
				if (parentdirection != -1 && checkNSWE(NSWE,x,y,x-1,y)) 
				{
					newNode = CellPathFinding.getInstance().readNode(x-1,y,tempz); 
					Neighbors.add(newNode);
				}	    
			}
			Node[] result = new Node[Neighbors.size()];
			return Neighbors.toArray(result);
		}

		/**
		 * @param NSWE
		 * @param x
		 * @param y
		 * @param tx
		 * @param ty
		 * @return True if NSWE dont block given direction
		 */
		private static boolean checkNSWE(short NSWE, int x, int y, int tx, int ty)
		{
			//Check NSWE
			if(NSWE == 15)
			   return true;
			if(tx > x)//E
			{
				if ((NSWE & _e) == 0)
					return false;
			}
			else if (tx < x)//W
			{
				if ((NSWE & _w) == 0)
					return false;
			}
			if (ty > y)//S
			{
				if ((NSWE & _s) == 0)
					return false;
			}
			else if (ty < y)//N
			{
				if ((NSWE & _n) == 0)
					return false;
			}
			return true;
		}
		@Override
		public short findGround(int x, int y) {
			int geox = (x - L2World.MAP_MIN_X) >> 4;
            int geoy =	(y - L2World.MAP_MIN_Y) >> 4;
			short region = getRegionOffset(geox,geoy);
			int blockX = getBlock(geox);
			int blockY = getBlock(geoy);
			int cellX, cellY, index;
			short temph = Short.MAX_VALUE;
			if(_geodataIndex.get(region) == null) index = ((blockX << 8) + blockY)*3;
			else index = _geodataIndex.get(region).get(((blockX << 8))+(blockY));
			ByteBuffer geo = _geodata.get(region);
			if(geo == null) {
				return temph;
			}
			byte type = geo.get(index);
			index++;
			if(type == 0)//flat
				temph = geo.getShort(index);
			else if(type == 1)//complex
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				index += ((cellX << 3) + cellY) << 1;
				short height = geo.getShort(index);
				height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				temph = height;
			}
			else//multilevel
			{
				cellX = getCell(geox);
				cellY = getCell(geoy);
				short height;
				temph = 0;
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = geo.get(index);
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = geo.get(index);
				index++;
				if(layers <= 0 || layers > 125)
				{
					_log.warn("Broken geofile (case2), region: "+region+" - invalid layer count: "+layers+" at: "+geox+" "+geoy);
					return temph;
				}
				while(layers > 0)
				{
					height = geo.getShort(index);
					height = (short)(height&0x0fff0);
					height = (short)(height >> 1); //height / 2
					temph += height;
					layers--;
					index += 2;
				}
			}
			return temph;
		}

		
	}
