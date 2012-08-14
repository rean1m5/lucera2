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
public class ShapeExCylinder extends Shape
{
	private int		_innerRad;
	private int		_outerRad;
	private long	_innerRadSq;
	private long	_outerRadSq;
	private int		_x, _y;

	@Override
	public boolean contains(int x, int y)
	{
		long dist = (long) (Math.pow(_x - x, 2) + Math.pow(_y - y, 2));
		return dist <= _outerRadSq && dist >= _innerRadSq;
	}

	@Override
	public int getMiddleX()
	{
		return _x;
	}

	@Override
	public int getMiddleY()
	{
		return _y;
	}

	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		// Circles point inside the rectangle?
		if (_x > ax1 && _x < ax2 && _y > ay1 && _y < ay2)
			return true;

		// Any point of the rectangle intersecting the Circle?
		long dist = (long) (Math.pow(ax1 - _x, 2) + Math.pow(ay1 - _y, 2));
		if (dist < _outerRadSq && dist > _innerRadSq)
			return true;
		dist = (long) (Math.pow(ax1 - _x, 2) + Math.pow(ay2 - _y, 2));
		if (dist < _outerRadSq && dist > _innerRadSq)
			return true;
		dist = (long) (Math.pow(ax2 - _x, 2) + Math.pow(ay1 - _y, 2));
		if (dist < _outerRadSq && dist > _innerRadSq)
			return true;
		dist = (long) (Math.pow(ax2 - _x, 2) + Math.pow(ay2 - _y, 2));
		if (dist < _outerRadSq && dist > _innerRadSq)
			return true;

		// Collision on any side of the rectangle?
		if (_x > ax1 && _x < ax2)
		{
			if (Math.abs(_y - ay2) < _outerRad && Math.abs(_y - ay2) > _innerRad)
				return true;
			if (Math.abs(_y - ay1) < _outerRad && Math.abs(_y - ay1) > _innerRad)
				return true;
		}
		if (_y > ay1 && _y < ay2)
		{
			if (Math.abs(_x - ax2) < _outerRad && Math.abs(_x - ax2) > _innerRad)
				return true;
			if (Math.abs(_x - ax1) < _outerRad && Math.abs(_x - ax1) > _innerRad)
				return true;
		}

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		return (Math.sqrt(Math.pow(_x - x, 2) + Math.pow(_y - y, 2)) - _outerRad);
	}

	@Override
	public Location getRandomLocation()
	{
		int angle = Rnd.get(360); // 0-359
		int range = Rnd.get(_innerRad, _outerRad);
		int x = _x + (int) (Math.sin(angle) * range);
		int y = _y + (int) (Math.cos(angle) * range);
		return new Location(x, y, _zMin);
	}

	public void setRadius(int inner, int outer)
	{
		_innerRad = inner;
		_innerRadSq = inner * inner;
		_outerRad = outer;
		_outerRadSq = outer * outer;
	}

	@Override
	protected Shape prepare(int zoneId)
	{
		if (_points.size() != 1)
		{
			_log.error("Invalid point amount in zone" + zoneId + ", must be 1");
			return null;
		}
		if (_innerRad <= 0)
		{
			_log.error("Inner radius must be > 0 in zone " + zoneId);
			return null;
		}
		if (_outerRad <= 0)
		{
			_log.error("Outer radius must be > 0 in zone " + zoneId);
			return null;
		}
		if (_outerRad <= _innerRad)
		{
			_log.error("Outer radius must be > inner radius in zone " + zoneId);
			return null;
		}

		_x = _points.get(0).x;
		_y = _points.get(0).y;
		return this;
	}
}