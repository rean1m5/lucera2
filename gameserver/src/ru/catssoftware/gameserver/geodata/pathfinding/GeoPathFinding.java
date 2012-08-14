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
package ru.catssoftware.gameserver.geodata.pathfinding;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;




import ru.catssoftware.Config;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.geodata.pathfinding.AbstractNodeLoc;
import ru.catssoftware.gameserver.geodata.pathfinding.Node;
import ru.catssoftware.gameserver.geodata.pathfinding.PathFinding;
import ru.catssoftware.gameserver.geodata.pathfinding.geonodes.GeoNodeLoc;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.Location;

import javolution.util.FastList;
import javolution.util.FastMap;


/**
 * @author -Nemesiss-
 */
public class GeoPathFinding extends PathFinding
{
	
	private static GeoPathFinding _instance;
	private static Map<Short, ByteBuffer> _pathNodes = new FastMap<Short, ByteBuffer>();
	private static Map<Short, IntBuffer> _pathNodesIndex = new FastMap<Short, IntBuffer>();

	public static GeoPathFinding getInstance()
	{
		if (_instance == null)
			_instance = new GeoPathFinding();
		return _instance;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.PathFinding#PathNodesExist(short)
	 */
	@Override
	public boolean pathNodesExist(short regionoffset)
	{
		return _pathNodesIndex.containsKey(regionoffset);
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.PathFinding#FindPath(int, int, short, int, int, short)
	 */
	@Override
	public List<AbstractNodeLoc> findPath(int x, int y, int z, int tx, int ty, int tz, int instance, boolean playable)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		short gz = (short)z;
		int gtx = (tx - L2World.MAP_MIN_X) >> 4;
		int gty = (ty - L2World.MAP_MIN_Y) >> 4;
		short gtz = (short)tz;

		Node start = readNode(gx,gy,gz);
		Node end = readNode(gtx,gty,gtz);
		if (start == null || end == null)
			return null;
		if (Math.abs(start.getLoc().getZ() - z) > 55)
			return null; // not correct layer
		if (Math.abs(end.getLoc().getZ() - tz) > 55)
			return null; // not correct layer
		if (start == end)
			return null;

    	// TODO: Find closest path node we CAN access. Now only checks if we can not reach the closest
		Location temp = GeoData.getInstance().moveCheck(x, y, z, start.getLoc().getX(), start.getLoc().getY(), start.getLoc().getZ(),instance);
		if ((temp.getX() != start.getLoc().getX()) || (temp.getY() != start.getLoc().getY()))
			return null; //   cannot reach closest...

		// TODO: Find closest path node around target, now only checks if final location can be reached
		temp = GeoData.getInstance().moveCheck(tx, ty, tz, end.getLoc().getX(), end.getLoc().getY(), end.getLoc().getZ(),instance);
		if ((temp.getX() != end.getLoc().getX()) || (temp.getY() != end.getLoc().getY()))
			return null; //   cannot reach closest...

		//return searchAStar(start, end);
		return searchByClosest2(start, end);
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.PathFinding#ReadNeighbors(short, short)
	 */
	@Override
	public Node[] readNeighbors(Node n, int idx)
	{
		int node_x = n.getLoc().getNodeX();
		int node_y = n.getLoc().getNodeY();

		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		ByteBuffer pn = _pathNodes.get(regoffset);

		List<Node> Neighbors = new FastList<Node>(8);
		Node newNode;
		short new_node_x, new_node_y;

		//Region for sure will change, we must read from correct file
		byte neighbor = pn.get(idx++); //N
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)node_x;
			new_node_y = (short)(node_y-1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null) Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //NE
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = (short)(node_y-1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null) Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //E
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = (short)node_y;
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //SE
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = (short)(node_y+1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //S
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)node_x;
			new_node_y = (short)(node_y+1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //SW
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = (short)(node_y+1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //W
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = (short)node_y;
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		neighbor = pn.get(idx++); //NW
		if (neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = (short)(node_y-1);
			newNode = readNode(new_node_x,new_node_y,neighbor);
			if (newNode != null)
				Neighbors.add(newNode);
		}
		Node[] result = new Node[Neighbors.size()];
		return Neighbors.toArray(result);
	}

	private Node readNode(short node_x, short node_y, byte layer)
	{
		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		if (!this.pathNodesExist(regoffset)) return null;
		short nbx = getNodeBlock(node_x);
		short nby = getNodeBlock(node_y);
		int idx = _pathNodesIndex.get(regoffset).get((nby << 8)+nbx);
		ByteBuffer pn = _pathNodes.get(regoffset);
		//reading
		byte nodes = pn.get(idx);
		idx += layer*10+1;//byte + layer*10byte
		if (nodes < layer)
			_log.warn("SmthWrong!");

		short node_z = pn.getShort(idx);
		idx += 2;
		return new Node(new GeoNodeLoc(node_x,node_y,node_z), idx);
	}

	private Node readNode(int gx, int gy, short z)
	{
		short node_x = getNodePos(gx);
		short node_y = getNodePos(gy);
		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		if (!this.pathNodesExist(regoffset)) return null;
		short nbx = getNodeBlock(node_x);
		short nby = getNodeBlock(node_y);
		int idx = _pathNodesIndex.get(regoffset).get((nby << 8)+nbx);
		ByteBuffer pn = _pathNodes.get(regoffset);
		//reading
		byte nodes = pn.get(idx++);
		int idx2 = 0; //create index to nearlest node by z
		short last_z = Short.MIN_VALUE;
		while (nodes > 0)
		{
			short node_z = pn.getShort(idx);
			if (Math.abs(last_z - z) >  Math.abs(node_z -z))
			{
				last_z = node_z;
				idx2 = idx+2;
			}
			idx += 10; //short + 8 byte
			nodes--;
		}
		return new Node(new GeoNodeLoc(node_x,node_y,last_z), idx2);
	}

	private GeoPathFinding()
	{
			_log.info("PathFinding Engine: - Loading Path Nodes...");
			int fCnt = 0; 
			File geodir = new File(Config.GEODATA_ROOT,"/pathnode");
			if(geodir.isDirectory()) {
				for(File f : geodir.listFiles()) {
					if(f.getName().endsWith(".pn")) try {
						String []coords = f.getName().replace(".pn", "").split("_");
						LoadPathNodeFile(Byte.parseByte(coords[0]), Byte.parseByte(coords[1]));
						fCnt++;
					} catch(Exception e) {
						_log.warn("PathFinding Engine: Error while loading "+f.getName(),e);
					}
				}
			}
			_log.info("PathFinding Engine: - Total loaded "+fCnt+" regions");
	}

	private void LoadPathNodeFile(byte rx,byte ry)
	{
		String fname = Config.GEODATA_ROOT + "/pathnode/" + rx + "_" + ry + ".pn";
		short regionoffset = getRegionOffset(rx,ry);
		File Pn = new File(fname);
		int node = 0,size, index = 0;
		FileChannel roChannel = null;
		try
		{
	        // Create a read-only memory-mapped file
	        roChannel = new RandomAccessFile(Pn, "r").getChannel();
			size = (int)roChannel.size();
			MappedByteBuffer nodes;
			if (Config.FORCE_GEODATA) //Force O/S to Loads this buffer's content into physical memory.
				//it is not guarantee, because the underlying operating system may have paged out some of the buffer's data
				nodes = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
			else
				nodes = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);

			// Indexing pathnode files, so we will know where each block starts
			IntBuffer indexs = IntBuffer.allocate(65536);

			while(node < 65536)
			{
				byte layer = nodes.get(index);
		        indexs.put(node++, index);
				index += layer*10+1;
			}
			_pathNodesIndex.put(regionoffset, indexs);
			_pathNodes.put(regionoffset, nodes);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			_log.warn("Failed to Load PathNode File: " + fname + "\n");
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
	}
}