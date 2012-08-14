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

import java.util.List;
import java.util.Random;

import javolution.util.FastList;

import org.w3c.dom.Node;

import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.tools.geometry.Point3D;

/**
 * @author Noctarius
 */
public class L2MapRegionRestart
{
	private int				_id						= -1;
	private String			_name					= "";
	private int				_bbsId					= -1;
	private int				_locName				= -1;
	private Castle			_castle;
	private List<Point3D>	_restartPoints			= new FastList<Point3D>();
	private List<Point3D>	_chaosPoints			= new FastList<Point3D>();

	private Race			_bannedRace				= Race.Human;
	private int				_bannedRaceRestartId	= -1;

	public L2MapRegionRestart(Node node)
	{
		Node e = node.getAttributes().getNamedItem("id");
		if (e != null)
			_id = Integer.parseInt(e.getNodeValue());

		e = node.getAttributes().getNamedItem("name");
		if (e != null)
			_name = e.getNodeValue();

		e = node.getAttributes().getNamedItem("bbs");
		if (e != null)
			_bbsId = Integer.parseInt(e.getNodeValue());

		e = node.getAttributes().getNamedItem("locname");
		if (e != null)
			_locName = Integer.parseInt(e.getNodeValue());

		parsePoints(node);
	}

	private void parsePoints(Node node)
	{
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("point".equals(n.getNodeName()))
			{
				Point3D point = getPoint3D(n);
				_restartPoints.add(point);
			}
			else if ("chaospoint".equalsIgnoreCase(n.getNodeName()))
			{
				Point3D point = getPoint3D(n);
				_chaosPoints.add(point);
			}
			else if ("bannedrace".equalsIgnoreCase(n.getNodeName()))
			{
				Node e = n.getAttributes().getNamedItem("race");
				if (e != null)
					_bannedRace = Race.getRaceByName(e.getNodeValue());

				e = n.getAttributes().getNamedItem("restartId");
				if (e != null)
					_bannedRaceRestartId = Integer.parseInt(e.getNodeValue());
			}
		}
	}

	private Point3D getPoint3D(Node node)
	{
		int X = 0;
		int Y = 0;
		int Z = 0;

		Node e = node.getAttributes().getNamedItem("X");
		if (e != null)
			X = Integer.parseInt(e.getNodeValue());

		e = node.getAttributes().getNamedItem("Y");
		if (e != null)
			Y = Integer.parseInt(e.getNodeValue());

		e = node.getAttributes().getNamedItem("Z");
		if (e != null)
			Z = Integer.parseInt(e.getNodeValue());

		Point3D point = new Point3D(X, Y, Z);

		return point;
	}

	public int getId()
	{
		return _id;
	}

	public int getBbsId()
	{
		return _bbsId;
	}

	public int getLocName()
	{
		return _locName;
	}

	public String getName()
	{
		return _name;
	}

	public Point3D getRandomRestartPoint(Race race)
	{
		if (_bannedRaceRestartId > -1 && race == _bannedRace)
			return MapRegionManager.getInstance().getRestartPoint(_bannedRaceRestartId);

		return getRandomRestartPoint();
	}

	public Point3D getRandomRestartPoint()
	{
		Random rnd = new Random();
		int pointId = rnd.nextInt(_restartPoints.size());
		return _restartPoints.get(pointId);
	}

	public Point3D getRandomChaosRestartPoint(Race race)
	{
		if (_bannedRaceRestartId > -1 && race == _bannedRace)
			return MapRegionManager.getInstance().getChaosRestartPoint(_bannedRaceRestartId);

		return getRandomChaosRestartPoint();
	}

	public Point3D getRandomChaosRestartPoint()
	{
		Random rnd = new Random();
		int pointId = rnd.nextInt(_chaosPoints.size());
		return _chaosPoints.get(pointId);
	}

	public Race getBannedRace()
	{
		if (_bannedRaceRestartId > -1)
			return _bannedRace;

		return null;
	}

	public int getRedirectId()
	{
		return _bannedRaceRestartId;
	}
	public void setCastle(Castle c) {
		_castle = c;
	}
	public Castle getCastle() { return _castle; }
}