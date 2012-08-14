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
package ru.catssoftware.gameserver.model;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.util.RndCoord;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.1 $ $Date: 2005/03/27 15:29:33 $
 */
public final class Location
{
	private int	_x;
	private int	_y;
	private int	_z;
	private int	_heading;

	public Location(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	public Location(String pos) {
		String []v =pos.split(" ");
		if(v.length>0)
			_x = Integer.parseInt(v[0].trim());
		if(v.length>1)
			_y = Integer.parseInt(v[1].trim());
		if(v.length>2)
			_z = Integer.parseInt(v[2].trim());
		if(v.length>3)
			_heading = Integer.parseInt(v[3].trim());
		
	}
	
	/**
	 * Копирующий конструктор<br>
	 * @param source as Location
	 */
	
	public Location(Node n) {
		NamedNodeMap attr = n.getAttributes();
		if(attr.getNamedItem("x")!=null)
			_x = Integer.parseInt(attr.getNamedItem("x").getNodeValue());
		if(attr.getNamedItem("y")!=null)
			_y = Integer.parseInt(attr.getNamedItem("y").getNodeValue());
		if(attr.getNamedItem("z")!=null)
			_z = Integer.parseInt(attr.getNamedItem("z").getNodeValue());
		if(attr.getNamedItem("heading")!=null)
			_heading = Integer.parseInt(attr.getNamedItem("heading").getNodeValue());
		
	}
	public Location(Location source) {
		_x = source._x;
		_y = source._y;
		_z = source._z;
		_heading = source._heading;
	}
	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}

	public int getX()
	{
		return _x;
	}

	public int getY()
	{
		return _y;
	}

	public int getZ()
	{
		return _z;
	}

	public int getHeading()
	{
		return _heading;
	}
	@Override
	public String toString() {
		return String.format("%d %d %d", _x,_y,_z);
	}

	public void setZ(int z) {
		_z = z;
		
	}
	public boolean equals(int x, int y, int z) {
		return x==_x && y==_y && z==_z;
	}

	public Location geo2world() {
		return new Location(_x>>4 + L2World.MAP_MIN_X, _y >> 4 + L2World.MAP_MIN_Y,_z);
	}
	public Location rnd(int min, int max, boolean change)
	{
		Location loc = RndCoord.coordsRandomize(this, min, max);
		loc = GeoData.getInstance().moveCheck(_x, _y, _z, loc._x, loc._y, 0,0);
		if(change)
		{
			_x = loc._x;
			_y = loc._y;
			_z = loc._z;
			return this;
		}
		return loc;
	}

	public void set(int tx, int ty, int tz, int heading) {
		_x = tx;
		_y = ty;
		_z = tx;
		_heading = heading;
		
	}
	
}