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
package ru.catssoftware.gameserver.model.zone.form;

import java.lang.reflect.Constructor;

import javolution.util.FastList;



import org.apache.log4j.Logger;


import org.w3c.dom.Node;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.Location;

/**
 * @author  Crion
 */
public abstract class Shape
{
	protected static Logger		_log		= Logger.getLogger(Shape.class.getName());

	protected FastList<Tupel>	_points;
	protected int				_zMin, _zMax;
	private boolean				_z			= false;
	protected boolean			_exclude	= false;

	public boolean isExclude()
	{
		return _exclude;
	}

	public int getMaxZ()
	{
		return _zMax;
	}

	public int getMinZ()
	{
		return _zMin;
	}

	public boolean contains(L2Object obj)
	{
		return contains(obj.getX(), obj.getY(), obj.getZ());
	}

	public boolean contains(Location loc)
	{
		return contains(loc.getX(), loc.getY(), loc.getZ());
	}

	public boolean contains(int x, int y, int z)
	{
		return (!_z || (z >= _zMin && z <= _zMax)) && contains(x, y);
	}

	public abstract boolean contains(int x, int y);

	public abstract int getMiddleX();

	public abstract int getMiddleY();

	public abstract boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2);

	public abstract double getDistanceToZone(int x, int y);

	public abstract Location getRandomLocation();

	protected abstract Shape prepare(int zoneId);

	protected static boolean lineSegmentsIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2)
	{
		return java.awt.geom.Line2D.linesIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2);
	}

	public static Shape parseShape(Node sn, int zoneId)
	{
		String type = "";
		Shape shape = null;
		Class<?> clazz;
		Constructor<?> constructor;
		try
		{
			type = sn.getAttributes().getNamedItem("type").getNodeValue();
			clazz = Class.forName("ru.catssoftware.gameserver.model.zone.form.Shape" + type);
			constructor = clazz.getConstructor();
			shape = (Shape) constructor.newInstance();
		}
		catch (Exception e)
		{
			_log.error("Cannot create a Shape" + type + " in zone " + zoneId);
			return null;
		}

		shape._points = new FastList<Tupel>();
		for (Node n = sn.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("point".equalsIgnoreCase(n.getNodeName()))
			{
				Tupel t = Tupel.parseTupel(n, zoneId);
				if (t != null)
					shape._points.add(t);
				else
					return null;
			}
		}

		if ("Cylinder".equalsIgnoreCase(type))
		{
			try
			{
				int rad = Integer.parseInt(sn.getAttributes().getNamedItem("radius").getNodeValue());
				((ShapeCylinder) shape).setRadius(rad);
			}
			catch (Exception e)
			{
				_log.warn("missing or wrong radius for cylinder in zone " + zoneId);
				return null;
			}
		}
		else if ("ExCylinder".equalsIgnoreCase(type))
		{
			try
			{
				int innerRad = Integer.parseInt(sn.getAttributes().getNamedItem("innerRadius").getNodeValue());
				int outerRad = Integer.parseInt(sn.getAttributes().getNamedItem("outerRadius").getNodeValue());
				((ShapeExCylinder) shape).setRadius(innerRad, outerRad);
			}
			catch (Exception e)
			{
				_log.warn("missing or wrong radius for cylinder in zone " + zoneId);
				return null;
			}
		}

		Node z1 = sn.getAttributes().getNamedItem("zMin");
		Node z2 = sn.getAttributes().getNamedItem("zMax");
		if (z1 != null && z2 != null)
		{
			try
			{
				shape._zMin = Integer.parseInt(z1.getNodeValue());
				shape._zMax = Integer.parseInt(z2.getNodeValue());
				shape._z = true;
			}
			catch (NumberFormatException nfe)
			{
				_log.error("zMin or zMax value not a number in zone " + zoneId);
				return null;
			}
		}

		Node ex = sn.getAttributes().getNamedItem("exclude");
		if (ex != null)
		{
			try
			{
				shape._exclude = Boolean.parseBoolean(ex.getNodeValue());
			}
			catch (Exception e)
			{
				_log.error("Invalid value for exclude in zone " + zoneId);
			}
		}
		Shape result = shape.prepare(zoneId);
		if (result != null)
		{
			result._points.clear();
			result._points = null;
		}
		return result;
	}
}