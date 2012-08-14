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
package ru.catssoftware.gameserver.geodata.pathfinding.cellnodes;

import ru.catssoftware.gameserver.geodata.pathfinding.AbstractNodeLoc;
import ru.catssoftware.gameserver.model.L2World;

/**
 * @author -Nemesiss-
 */
public class NodeLoc extends AbstractNodeLoc
{
	private final int _x;
	private final int _y;
	private short _z;

	public NodeLoc(int x, int y, short z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getX()
	 */
	@Override
	public int getX()
	{
		return (_x << 4) + L2World.MAP_MIN_X;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getY()
	 */
	@Override
	public int getY()
	{
		return (_y << 4) + L2World.MAP_MIN_Y;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getZ()
	 */
	@Override
	public short getZ()
	{
		return _z;
	}

	@Override
	public void setZ(short z)
	{
		_z = z;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getNodeX()
	 */
	@Override
	public int getNodeX()
	{
		// TODO Auto-generated method stub
		return _x;
	}

	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getNodeY()
	 */
	@Override
	public int getNodeY()
	{
		// TODO Auto-generated method stub
		return _y;
	}
}