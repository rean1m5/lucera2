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
package ru.catssoftware.gameserver.model.mapregion;

import java.util.Map;

import javolution.util.FastMap;

import org.w3c.dom.Node;

import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.model.world.L2Polygon;

/**
 * @author Noctarius
 */
public class L2MapRegion
{
	private int					_id				= -1;
	private L2Polygon			_polygon		= new L2Polygon();
	private Map<Race, Integer>	_restarts		= new FastMap<Race, Integer>();
	private int					_zMin			= -999999999;
	private int					_zMax			= 999999999;

	private boolean				_specialRegion	= true;
	private L2MapArea			_area			= null;

	public L2MapRegion(Node node)
	{
		Node e = node.getAttributes().getNamedItem("id");
		if (e != null)
			_id = Integer.parseInt(e.getNodeValue());

		parseRangeData(node);
	}

	/**
	 * for adding L2MapRegion by L2MapArea
	 * @param id
	 * @param polygon
	 */
	public L2MapRegion(int id, int restartId, L2Polygon polygon, L2MapArea area)
	{
		_id = id;
		_polygon = polygon;
		_area = area;

		// add restartpoints by id
		_restarts.put(Race.Human, restartId);
		_restarts.put(Race.Darkelf, restartId);
		_restarts.put(Race.Dwarf, restartId);
		_restarts.put(Race.Elf, restartId);
		_restarts.put(Race.Orc, restartId);

		// set to AreaMapRegion
		_specialRegion = false;
	}

	private void parseRangeData(Node node)
	{
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("zHeight".equalsIgnoreCase(n.getNodeName()))
			{
				Node d = n.getAttributes().getNamedItem("min");
				if (d != null)
					_zMin = Integer.parseInt(d.getTextContent());

				d = n.getAttributes().getNamedItem("max");
				if (d != null)
					_zMax = Integer.parseInt(d.getTextContent());
			}
			else if ("point".equalsIgnoreCase(n.getNodeName()))
			{
				int X = 0;
				int Y = 0;

				Node d = n.getAttributes().getNamedItem("X");
				if (d != null)
					X = Integer.parseInt(d.getTextContent());

				d = n.getAttributes().getNamedItem("Y");
				if (d != null)
					Y = Integer.parseInt(d.getTextContent());

				_polygon.addPoint(X, Y);
			}
			else if ("restart".equalsIgnoreCase(n.getNodeName()))
			{
				Race race = Race.Human;
				int restartId = 0;

				Node d = n.getAttributes().getNamedItem("race");
				if (d != null)
					race = Race.getRaceByName(d.getTextContent());

				d = n.getAttributes().getNamedItem("restartId");
				if (d != null)
					restartId = Integer.parseInt(d.getTextContent());

				if (!_restarts.containsKey(race))
					_restarts.put(race, restartId);
			}
		}

		// add first point at the end of the polygon again for crossing
		_polygon.addPoint(_polygon.getXPoints()[0], _polygon.getYPoints()[0]);
	}

	public int getRestartId(Race race)
	{
		return _restarts.get(race);
	}

	public int getRestartId()
	{
		return getRestartId(Race.Human);
	}

	public int[] getZ()
	{
		int[] z =
		{ _zMin, _zMax };

		return z;
	}

	public L2Polygon getRegionPolygon()
	{
		return _polygon;
	}

	public int getId()
	{
		return _id;
	}

	public final boolean checkIfInRegion(int x, int y, int z)
	{
		if (!_specialRegion)
			return (_area.checkIfInRegion(x, y));

		if (!quickIsInsideRegion(x, y, z))
			return false;

		return _polygon.contains(x, y);
	}

	private final boolean quickIsInsideRegion(int x, int y, int z)
	{
		int[] xPoints = _polygon.getXPoints();
		int[] yPoints = _polygon.getYPoints();

		int xMax = xPoints[0];
		int xMin = xPoints[0];
		int yMax = yPoints[0];
		int yMin = yPoints[0];

		for (int in : xPoints)
		{
			if (in > xMax)
				xMax = in;
			else if (in < xMin)
				xMin = in;
		}

		for (int in : yPoints)
		{
			if (in > yMax)
				yMax = in;
			else if (in < yMin)
				yMin = in;
		}

		if (!(x > xMin && x < xMax && y > yMin && y < yMax))
			return false;

		if (z == -1)
			return true;

		if (_zMin == -999999999 && _zMax == 999999999)
			return true;

		return z > _zMin && z < _zMax;
	}

	public boolean isSpecialRegion()
	{
		return _specialRegion;
	}

	public L2MapArea getArea()
	{
		return _area;
	}
}