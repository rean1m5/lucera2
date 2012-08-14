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

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.CTF.CTF;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.serverpackets.RadarControl;

import java.util.Vector;
import java.util.concurrent.ScheduledFuture;


/**
 * @author dalrond
 */
public final class L2Radar
{
	private L2PcInstance		_player;
	private Vector<RadarMarker>	_markers;

	private ScheduledFuture<?> _markerOnPlayer;

	
	public L2Radar(L2PcInstance player)
	{
		_player = player;
		_markers = new Vector<RadarMarker>();
	}

	public void addMarker(L2Character target)
	{
		if (_markerOnPlayer != null)
		{
			removeAllMarkers();
			_markerOnPlayer.cancel(true);
			_markerOnPlayer = null;
		}

		if (target.isPlayer())
		_markerOnPlayer = ThreadPoolManager.getInstance().schedule(new RadarOnTarget(_player, target.getPlayer()), 0);
	}

	public void addMarker(Location loc)
	{
		addMarker(loc.getX(),  loc.getY(), loc.getZ());
	}

	// Add a marker to player's radar
	public void addMarker(int x, int y, int z)
	{
		RadarMarker newMarker = new RadarMarker(x, y, z);

		_markers.add(newMarker);
		_player.sendPacket(new RadarControl(2, 2, x, y, z));
		_player.sendPacket(new RadarControl(0, 1, x, y, z));
	}

	// Remove a marker from player's radar
	public void removeMarker(int x, int y, int z)
	{
		RadarMarker newMarker = new RadarMarker(x, y, z);

		_markers.remove(newMarker);
		_player.sendPacket(new RadarControl(1, 1, x, y, z));
	}

	public void removeAllMarkers()
	{
		for (RadarMarker tempMarker : _markers)
			_player.sendPacket(new RadarControl(2, 2, tempMarker._x, tempMarker._y, tempMarker._z));

		_markers.removeAllElements();

		if (_markerOnPlayer != null)
		{
			_markerOnPlayer.cancel(true);
			_markerOnPlayer = null;
		}
	}

	public void loadMarkers()
	{
		_player.sendPacket(new RadarControl(2, 2, _player.getX(), _player.getY(), _player.getZ()));
		for (RadarMarker tempMarker : _markers)
			_player.sendPacket(new RadarControl(0, 1, tempMarker._x, tempMarker._y, tempMarker._z));
	}

	public class RadarMarker
	{
		// Simple class to model radar points.
		public int	_type, _x, _y, _z;

		public RadarMarker(int type, int x, int y, int z)
		{
			_type = type;
			_x = x;
			_y = y;
			_z = z;
		}

		public RadarMarker(int x, int y, int z)
		{
			_type = 1;
			_x = x;
			_y = y;
			_z = z;
		}

		@Override
		public boolean equals(Object obj)
		{
			try
			{
				RadarMarker temp = (RadarMarker) obj;

				return (temp._x == this._x) && (temp._y == this._y) && (temp._z == this._z) && (temp._type == this._type);
			}
			catch (Exception e)
			{
				return false;
			}
		}
	}

	public class RadarOnTarget implements Runnable
	{
		private final L2PcInstance _me;
		private final L2PcInstance _myTarget;

		public RadarOnTarget(L2PcInstance me, L2PcInstance target)
		{
			_me = me;
			_myTarget = target;
		}

		public void run()
		{
			try
			{
				if (_me == null || _me.isOnline() == 0)
					return;
				_me.sendPacket(new RadarControl(1, 1, _me.getX(), _me.getY(), _me.getZ()));
				if (_myTarget == null || _myTarget.isOnline() == 0)
					return;
				removeAllMarkers();
				_me.sendPacket(new RadarControl(0, 1, _myTarget.getX(), _myTarget.getY(), _myTarget.getZ()));
				ThreadPoolManager.getInstance().scheduleGeneral(this, 8000);
			}
			catch (Throwable t)
			{
			}
		}
	}

	public class RadarOnPlayer implements Runnable
	{
		private final L2PcInstance	_myTarget, _me;

		public RadarOnPlayer(L2PcInstance target, L2PcInstance me)
		{
			_me = me;
			_myTarget = target;
		}

		public void run()
		{
			try
			{
				if (_me == null || _me.isOnline() == 0)
					return;

				_me.sendPacket(new RadarControl(1, 1, _me.getX(), _me.getY(), _me.getZ()));
				if (_myTarget == null || _myTarget.isOnline() == 0 || 
						(_myTarget.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND)==null ||
						 CTF.hasFlag(_myTarget)))
					return;

				_me.sendPacket(new RadarControl(0, 1, _myTarget.getX(), _myTarget.getY(), _myTarget.getZ()));
				ThreadPoolManager.getInstance().scheduleGeneral(this, 15000);
			}
			catch (Throwable t)
			{
			}
		}
	}
}