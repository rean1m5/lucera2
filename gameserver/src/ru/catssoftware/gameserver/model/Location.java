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
	public int x;
	public int y;
	public int z;
	public int heading;

	public Location(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Location(String pos) {
		String []v =pos.split(" ");
		if(v.length>0)
			x = Integer.parseInt(v[0].trim());
		if(v.length>1)
			y = Integer.parseInt(v[1].trim());
		if(v.length>2)
			z = Integer.parseInt(v[2].trim());
		if(v.length>3)
			heading = Integer.parseInt(v[3].trim());
		
	}
	
	/**
	 * Копирующий конструктор<br>
	 * @param source as Location
	 */
	
	public Location(Node n) {
		NamedNodeMap attr = n.getAttributes();
		if(attr.getNamedItem("x")!=null)
			x = Integer.parseInt(attr.getNamedItem("x").getNodeValue());
		if(attr.getNamedItem("y")!=null)
			y = Integer.parseInt(attr.getNamedItem("y").getNodeValue());
		if(attr.getNamedItem("z")!=null)
			z = Integer.parseInt(attr.getNamedItem("z").getNodeValue());
		if(attr.getNamedItem("heading")!=null)
			heading = Integer.parseInt(attr.getNamedItem("heading").getNodeValue());
		
	}
	public Location(Location source) {
		x = source.x;
		y = source.y;
		z = source.z;
		heading = source.heading;
	}
	public Location(int x, int y, int z, int heading)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public int getHeading()
	{
		return heading;
	}
	@Override
	public String toString() {
		return String.format("%d %d %d", x, y, z);
	}

	public void setZ(int z) {
		this.z = z;
		
	}
	public boolean equals(int x, int y, int z) {
		return x== this.x && y== this.y && z== this.z;
	}

	public Location geo2world() {
		return new Location(x >>4 + L2World.MAP_MIN_X, y >> 4 + L2World.MAP_MIN_Y, z);
	}
	public Location rnd(int min, int max, boolean change)
	{
		Location loc = RndCoord.coordsRandomize(this, min, max);
		loc = GeoData.getInstance().moveCheck(x, y, z, loc.x, loc.y, 0,0);
		if(change)
		{
			x = loc.x;
			y = loc.y;
			z = loc.z;
			return this;
		}
		return loc;
	}
	
}