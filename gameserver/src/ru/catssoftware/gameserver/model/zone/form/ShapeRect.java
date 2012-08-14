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

import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.tools.random.Rnd;


/**
 * @author  Crion
 */
public class ShapeRect extends Shape
{
	private int	_xMin, _xMax;
	private int	_yMin, _yMax;

	@Override
	public boolean contains(int x, int y)
	{
		return x >= _xMin && x <= _xMax && y >= _yMin && y <= _yMax;
	}

	@Override
	public int getMiddleX()
	{
		return (_xMin + _xMax) / 2;
	}

	@Override
	public int getMiddleY()
	{
		return (_yMin + _yMax) / 2;
	}

	@Override
	public boolean intersectsRectangle(int axMin, int axMax, int ayMin, int ayMax)
	{
		// Check if any point inside this rectangle
		if (contains(axMin, ayMin))
			return true;
		if (contains(axMin, ayMax))
			return true;
		if (contains(axMax, ayMin))
			return true;
		if (contains(axMax, ayMax))
			return true;

		// Check if any point from this rectangle is inside the other one
		if (_xMin > axMin && _xMin < axMax && _yMin > ayMin && _yMin < ayMax)
			return true;
		if (_xMin > axMin && _xMin < axMax && _yMax > ayMin && _yMax < ayMax)
			return true;
		if (_xMax > axMin && _xMax < axMax && _yMin > ayMin && _yMin < ayMax)
			return true;
		if (_xMax > axMin && _xMax < axMax && _yMax > ayMin && _yMax < ayMax)
			return true;

		// Horizontal lines may intersect vertical lines
		if (lineSegmentsIntersect(_xMin, _yMin, _xMax, _yMin, axMin, ayMin, axMin, ayMax))
			return true;
		if (lineSegmentsIntersect(_xMin, _yMin, _xMax, _yMin, axMax, ayMin, axMax, ayMax))
			return true;
		if (lineSegmentsIntersect(_xMin, _yMax, _xMax, _yMax, axMin, ayMin, axMin, ayMax))
			return true;
		if (lineSegmentsIntersect(_xMin, _yMax, _xMax, _yMax, axMax, ayMin, axMax, ayMax))
			return true;

		// Vertical lines may intersect horizontal lines
		if (lineSegmentsIntersect(_xMin, _yMin, _xMin, _yMax, axMin, ayMin, axMax, ayMin))
			return true;
		if (lineSegmentsIntersect(_xMin, _yMin, _xMin, _yMax, axMin, ayMax, axMax, ayMax))
			return true;
		if (lineSegmentsIntersect(_xMax, _yMin, _xMax, _yMax, axMin, ayMin, axMax, ayMin))
			return true;
		return lineSegmentsIntersect(_xMax, _yMin, _xMax, _yMax, axMin, ayMax, axMax, ayMax);
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		double test, shortestDist = Math.pow(_xMin - x, 2) + Math.pow(_yMin - y, 2);

		test = Math.pow(_xMin - x, 2) + Math.pow(_yMax - y, 2);
		if (test < shortestDist)
			shortestDist = test;

		test = Math.pow(_xMax - x, 2) + Math.pow(_yMin - y, 2);
		if (test < shortestDist)
			shortestDist = test;

		test = Math.pow(_xMax - x, 2) + Math.pow(_yMax - y, 2);
		if (test < shortestDist)
			shortestDist = test;

		return Math.sqrt(shortestDist);
	}

	@Override
	public Location getRandomLocation()
	{
		int x = Rnd.get(_xMin, _xMax);
		int y = Rnd.get(_yMin, _yMax);
		return new Location(x, y, _zMin);
	}

	@Override
	protected Shape prepare(int zoneId)
	{
		if (_points.size() != 2)
		{
			_log.error("Invalid point amount in zone" + zoneId + ", must be 2");
			return null;
		}
		if (_points.get(0).x < _points.get(1).x)
		{
			_xMin = _points.get(0).x;
			_xMax = _points.get(1).x;
		}
		else
		{
			_xMin = _points.get(1).x;
			_xMax = _points.get(0).x;
		}
		if (_points.get(0).y < _points.get(1).y)
		{
			_yMin = _points.get(0).y;
			_yMax = _points.get(1).y;
		}
		else
		{
			_yMin = _points.get(1).y;
			_yMax = _points.get(0).y;
		}
		return this;
	}
}