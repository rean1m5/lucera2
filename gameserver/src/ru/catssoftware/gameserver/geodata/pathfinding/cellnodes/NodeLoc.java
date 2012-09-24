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

import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.geodata.pathfinding.AbstractNodeLoc;
import ru.catssoftware.gameserver.model.L2World;
/**
 * @author -Nemesiss-
 */
public class NodeLoc extends AbstractNodeLoc
{
	private int _x;
	private int _y;
	private short _geoHeightAndNSWE;
	
	public NodeLoc(int x, int y, short z)
	{
		_x = x;
		_y = y;
		_geoHeightAndNSWE = GeoData.getInstance().getHeightAndNSWE(x, y, z);
	}
	
	public void set(int x, int y, short z)
	{
		_x = x;
		_y = y;
		_geoHeightAndNSWE = GeoData.getInstance().getHeightAndNSWE(x, y, z);
	}
	
	public short getNSWE()
	{
		return (short) (_geoHeightAndNSWE & 0x0f);
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
		short height = (short) (_geoHeightAndNSWE & 0x0fff0);
		return (short) (height >> 1);
	}
	
	@Override
	public void setZ(short z)
	{
		//
	}
	
	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getNodeX()
	 */
	@Override
	public int getNodeX()
	{
		return _x;
	}
	
	/**
	 * @see ru.catssoftware.gameserver.pathfinding.AbstractNodeLoc#getNodeY()
	 */
	@Override
	public int getNodeY()
	{
		return _y;
	}
	
	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + _x;
		result = (prime * result) + _y;
		result = (prime * result) + _geoHeightAndNSWE;
		return result;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof NodeLoc))
		{
			return false;
		}
		final NodeLoc other = (NodeLoc) obj;
		if (_x != other._x)
		{
			return false;
		}
		if (_y != other._y)
		{
			return false;
		}
		if (_geoHeightAndNSWE != other._geoHeightAndNSWE)
		{
			return false;
		}
		return true;
	}
}
