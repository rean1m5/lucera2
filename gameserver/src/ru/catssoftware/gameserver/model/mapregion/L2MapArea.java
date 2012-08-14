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


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.world.L2Polygon;

/**
 * @author Noctarius
 */
public class L2MapArea
{
	protected final static Logger	_log				= Logger.getLogger(L2MapArea.class.getName());

	private final int			_tileHeightWidth	= 32768;

	private int					_id					= 0;
	private int					_restartId			= 0;

	private int					_X					= 0;
	private int					_Y					= 0;

	private L2MapRegion			_region				= null;

	public L2MapArea(int restartId, int x, int y)
	{
		_id = IdFactory.getInstance().getNextId();

		_restartId = restartId;
		_X = (x - 15) * _tileHeightWidth - 164608;
		_Y = (y - 10) * _tileHeightWidth - 262144;

		L2Polygon poly = new L2Polygon();
		poly.addPoint(_X, _Y);
		poly.addPoint(_X + _tileHeightWidth, _Y);
		poly.addPoint(_X + _tileHeightWidth, _Y + _tileHeightWidth);
		poly.addPoint(_X, _Y + _tileHeightWidth);

		_region = new L2MapRegion(_id, _restartId, poly, this);
	}

	public int getId()
	{
		return _id;
	}

	public int getRestartId()
	{
		return _restartId;
	}

	public boolean checkIfInRegion(int x, int y)
	{
		return (x >= _X && x <= (_X + _tileHeightWidth) && y >= _Y && y <= (_Y + _tileHeightWidth));
	}

	public L2MapRegion getMapRegion()
	{
		return _region;
	}
}