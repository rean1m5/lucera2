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
public class ShapePoly extends Shape
{
	private int[]	_x;
	private int[]	_y;

	private int		_xMin	= Integer.MAX_VALUE, _xMax = Integer.MIN_VALUE;
	private int		_yMin	= _xMin, _yMax = _xMax;

	@Override
	public boolean contains(int x, int y)
	{
		boolean inside = false;
		for (int i = 0, j = _x.length - 1; i < _x.length; j = i++)
		{
			if ((((_y[i] <= y) && (y < _y[j])) || ((_y[j] <= y) && (y < _y[i]))) && (x < (_x[j] - _x[i]) * (y - _y[i]) / (_y[j] - _y[i]) + _x[i]))
				inside = !inside;
		}
		return inside;
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
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		int tX, tY, uX, uY;

		// First check if a point of the polygon lies inside the rectangle
		if (_x[0] > ax1 && _x[0] < ax2 && _y[0] > ay1 && _y[0] < ay2)
			return true;

		// Or a point of the rectangle inside the polygon
		if (contains(ax1, ay1))
			return true;

		// If the first point wasn't inside the rectangle it might still have any line crossing any side
		// of the rectangle

		// Check every possible line of the polygon for a collision with any of the rectangles side
		for (int i = 0; i < _y.length; i++)
		{
			tX = _x[i];
			tY = _y[i];
			uX = _x[(i + 1) % _x.length];
			uY = _y[(i + 1) % _x.length];

			// Check if this line intersects any of the four sites of the rectangle
			if (lineSegmentsIntersect(tX, tY, uX, uY, ax1, ay1, ax1, ay2))
				return true;
			if (lineSegmentsIntersect(tX, tY, uX, uY, ax1, ay1, ax2, ay1))
				return true;
			if (lineSegmentsIntersect(tX, tY, uX, uY, ax2, ay2, ax1, ay2))
				return true;
			if (lineSegmentsIntersect(tX, tY, uX, uY, ax2, ay2, ax2, ay1))
				return true;
		}

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		double test, shortestDist = Math.pow(_x[0] - x, 2) + Math.pow(_y[0] - y, 2);

		for (int i = 1; i < _y.length; i++)
		{
			test = Math.pow(_x[i] - x, 2) + Math.pow(_y[i] - y, 2);
			if (test < shortestDist)
				shortestDist = test;
		}

		return Math.sqrt(shortestDist);
	}

	@Override
	protected Shape prepare(int zoneId)
	{
		if (_points.size() < 3)
		{
			_log.error("Invalid point amount in zone" + zoneId + ", must be >2");
			return null;
		}

		int size = _points.size();
		_x = new int[size];
		_y = new int[size];
		Tupel t;
		for (int i = 0; i < size; i++)
		{
			t = _points.get(i);
			_xMin = Math.min(_xMin, t.x);
			_xMax = Math.max(_xMax, t.x);
			_x[i] = t.x;
			_yMin = Math.min(_yMin, t.y);
			_yMax = Math.max(_yMax, t.y);
			_y[i] = t.y;
		}
		return this;
	}

	@Override
	public Location getRandomLocation()
	{
		int x, y;
		do
		{
			x = Rnd.get(_xMin, _xMax);
			y = Rnd.get(_yMin, _yMax);
		}
		while (!contains(x, y));

		return new Location(x, y, _zMin);
	}
}