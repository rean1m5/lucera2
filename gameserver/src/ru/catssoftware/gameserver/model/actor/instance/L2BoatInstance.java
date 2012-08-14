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
package ru.catssoftware.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;


import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.knownlist.BoatKnownList;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.network.serverpackets.VehicleCheckLocation;
import ru.catssoftware.gameserver.network.serverpackets.VehicleDeparture;
import ru.catssoftware.gameserver.network.serverpackets.VehicleInfo;
import ru.catssoftware.gameserver.network.serverpackets.VehicleStarted;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.util.Util;


/**
 * @author Maktakien
 */
public class L2BoatInstance extends L2Character
{
	private final static Logger	_log	= Logger.getLogger(L2BoatInstance.class.getName());

	public float boatSpeed;
	public class L2BoatTrajet
	{
		private Map<Integer, L2BoatPoint>	_path;

		public int							idWaypoint1;
		public int							idWTicket1;
		public int							ntx1;
		public int							nty1;
		public int							ntz1;
		public int							max;
		public String						boatName;
		public String						npc1;
		public String						sysmess10_1;
		public String						sysmess5_1;
		public String						sysmess1_1;
		public String						sysmessb_1;
		public String						sysmess0_1;

		protected class L2BoatPoint
		{
			public int	speed1;
			public int	speed2;
			public int	x;
			public int	y;
			public int	z;
			public int	time;
		}

		
		/**
		 * @param idWaypoint1
		 * @param idWTicket1
		 * @param ntx1
		 * @param nty1
		 * @param ntz1
		 * @param idnpc1
		 * @param sysmess10_1
		 * @param sysmess5_1
		 * @param sysmess1_1
		 * @param sysmessb_1
		 * @param pSysmessb_1
		 * @param pBoatname
		 */
		public L2BoatTrajet(int pIdWaypoint1, int pIdWTicket1, int pNtx1, int pNty1, int pNtz1, String pNpc1, String pSysmess10_1, String pSysmess5_1,
				String pSysmess1_1, String pSysmess0_1, String pSysmessb_1, String pBoatname)
		{
			idWaypoint1 = pIdWaypoint1;
			idWTicket1 = pIdWTicket1;
			ntx1 = pNtx1;
			nty1 = pNty1;
			ntz1 = pNtz1;
			npc1 = pNpc1;
			sysmess10_1 = pSysmess10_1;
			sysmess5_1 = pSysmess5_1;
			sysmess1_1 = pSysmess1_1;
			sysmessb_1 = pSysmessb_1;
			sysmess0_1 = pSysmess0_1;
			boatName = pBoatname;
		}

		/**
		 *
		 */
		public void loadBoatPath(Node path)
		{
			if(path == null)
				_log.warn("No path for boat " + boatName + " !!!");
			else
			{
				if(Integer.parseInt(path.getAttributes().getNamedItem("route_id").getNodeValue()) == idWaypoint1)
				{
					_path = new FastMap<Integer, L2BoatPoint>();
					max = Integer.parseInt(path.getAttributes().getNamedItem("count").getNodeValue());
					int i = 0;
					for(Node n = path.getFirstChild();n!=null;n=n.getNextSibling()) {
						if(n.getNodeName().equals("point")) {
							L2BoatPoint bp = new L2BoatPoint();
							bp.speed1 = Integer.parseInt(n.getAttributes().getNamedItem("speed1").getNodeValue());
							bp.speed2 = Integer.parseInt(n.getAttributes().getNamedItem("speed2").getNodeValue());
							bp.x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
							bp.y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
							bp.z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
							bp.time = Integer.parseInt(n.getAttributes().getNamedItem("time").getNodeValue());
							_path.put(i++, bp);
						}
					}
				}
				else
					_log.warn("Wrong path for boat " + boatName + " !!!");
			}
		}

		/**
		 * @param state
		 * @return
		 */
		public int state(int state, L2BoatInstance _boat)
		{
			if (state < max)
			{
				L2BoatPoint bp = _path.get(state);
				double dx = (_boat.getX() - bp.x);
				double dy = (_boat.getY() - bp.y);
				double distance = Math.sqrt(dx * dx + dy * dy);
				double cos;
				double sin;
				sin = dy / distance;
				cos = dx / distance;

				_boat.getPosition().setHeading(Util.calculateHeadingFrom(cos, sin));

				_boat._vd = new VehicleDeparture(_boat, bp.speed1, bp.speed2, bp.x, bp.y, bp.z);
				boatSpeed = bp.speed1;
				_boat.moveToLocation(bp.x, bp.y, bp.z, (float) bp.speed1);
				if (bp.time == 0)
					bp.time = 1;
				Collection<L2PcInstance> knownPlayers = _boat.getKnownList().getKnownPlayers().values();
				if (knownPlayers != null && !knownPlayers.isEmpty())
				{
					for (L2PcInstance player : knownPlayers)
						player.sendPacket(_boat._vd);
				}
				return bp.time;
			}

			return 0;
		}
		public int getPathSize() {
			if(_path==null)
				return 0;
			return _path.size();
		}
	}

	private String						_name;
	protected L2BoatTrajet				_t1;
	protected L2BoatTrajet				_t2;
	protected int						_cycle	= 0;
	protected VehicleDeparture			_vd		= null;
	private Map<Integer, L2PcInstance>	_inboat;
	private boolean						_inCycle = true;
	
	public int getSizeInside()
	{
		return _inboat == null ? 0 : _inboat.size();
	}

	public String getBoatName()
	{
		return _name;
	}

	public int getCycle()
	{
		return _cycle;
	}

	public boolean isInCycle()
	{
		return _inCycle;
	}

	public void stopCycle()
	{
		_inCycle = false;
		stopMove(new L2CharPosition(getX(), getY(), getZ(), getPosition().getHeading()));
	}

	public void startCycle()
	{
		_inCycle = true;
		_cycle = 1;
		beginCycle();
	}

	public void reloadPath()
	{
		//_t1.loadBoatPath();
		//_t2.loadBoatPath();
		_cycle = 0;
		stopCycle();
		startCycle();
	}
	
	public L2BoatInstance(int objectId, L2CharTemplate template, String name)
	{
		super(objectId, template);
		getKnownList();
		setAI(new L2CharacterAI(new AIAccessor()));
		_name = name;
	}

	@Override
	public BoatKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new BoatKnownList(this);

		return (BoatKnownList) _knownList;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public void moveToLocation(int x, int y, int z, float speed)
	{
		final int curX = getX();
		final int curY = getY();

		// Calculate distance (dx,dy) between current position and destination
		final int dx = (x - curX);
		final int dy = (y - curY);
		double distance = Math.sqrt(dx * dx + dy * dy);

		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug("distance to target:" + distance);

		// Define movement angles needed
		// ^
		// | X (x,y)
		// | /
		// | /distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)

		double cos;
		double sin;
		sin = dy / distance;
		cos = dx / distance;
		// Create and Init a MoveData object
		MoveData m = new MoveData();

		// Calculate the Nb of ticks between the current position and the destination
		int ticksToMove = (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

		// Calculate and set the heading of the L2Character
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);
		heading += 32768;
		getPosition().setHeading(heading);

		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug("dist:" + distance + "speed:" + speed + " ttt:"
					+ ticksToMove + " heading:" + heading);

		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z;	// this is what was requested from client
		m._heading = 0;			// initial value for coordinate sync

		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m._moveStartTime = GameTimeController.getGameTicks();

		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug("time to target:" + ticksToMove);

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingChar(this);
	}

	private class BoatCaptain implements Runnable
	{
		private int				_state;
		private L2BoatInstance	_boat;

		/**
		 * @param i
		 * @param instance
		 */
		public BoatCaptain(int i, L2BoatInstance instance)
		{
			_state = i;
			_boat = instance;
		}

		public void run()
		{
			if (!_inCycle)
				return;
				
			if (_boat.getId() == 5) // Rune <-> Primeval Isle
			{
				switch (_state)
				{
				case 1:
					_boat.say(-1);
					_boat.begin();
					break;
				}
			}
			else
			{
				BoatCaptain bc;
				switch (_state)
				{
				case 1:
					_boat.say(5);
					bc = new BoatCaptain(2, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 240000);
					break;
				case 2:
					_boat.say(1);
					bc = new BoatCaptain(3, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 40000);
					break;
				case 3:
					_boat.say(0);
					bc = new BoatCaptain(4, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 20000);
					break;
				case 4:
					_boat.say(-1);
					_boat.begin();
					break;
				}
			}
		}
	}

	private class Boatrun implements Runnable
	{
		private int				_state;
		private L2BoatInstance	_boat;

		/**
		 * @param i
		 * @param instance
		 */
		public Boatrun(int i, L2BoatInstance instance)
		{
			_state = i;
			_boat = instance;
		}

		public void run()
		{
			if (!_inCycle)
				return;
			
			_boat._vd = null;
			_boat.needOnVehicleCheckLocation = false;
			if (_boat._cycle == 1)
			{
				int time = _boat._t1.state(_state, _boat);
				if (time > 0)
				{
					_state++;
					Boatrun bc = new Boatrun(_state, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, time);

				}
				else if (time == 0)
				{
					_boat._cycle = 2;
					_boat.say(10);
					BoatCaptain bc = new BoatCaptain(1, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
				}
				else
				{
					_boat.needOnVehicleCheckLocation = true;
					_state++;
					_boat._runstate = _state;
				}
			}
			else if (_boat._cycle == 2)
			{
				int time = _boat._t2.state(_state, _boat);
				if (time > 0)
				{
					_state++;
					Boatrun bc = new Boatrun(_state, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, time);
				}
				else if (time == 0)
				{
					_boat._cycle = 1;
					_boat.say(10);
					BoatCaptain bc = new BoatCaptain(1, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
				}
				else
				{
					_boat.needOnVehicleCheckLocation = true;
					_state++;
					_boat._runstate = _state;
				}
			}
		}
	}

	public int	_runstate	= 0;

	public void evtArrived()
	{

		if (_runstate != 0)
		{
			Boatrun bc = new Boatrun(_runstate, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 10);
			_runstate = 0;
		}
	}

	/**
	 * @return
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * @param activeChar
	 */
	public void sendVehicleDeparture(L2PcInstance activeChar)
	{
		if (_vd != null)
			activeChar.sendPacket(_vd);
	}

	public VehicleDeparture getVehicleDeparture()
	{
		return _vd;
	}

	public void beginCycle()
	{
		_inCycle = true;
		say(10);
		BoatCaptain bc = new BoatCaptain(1, this);
		if (getId() == 5)
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 180000);
		else
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
	}

	/**
	 * @param destination
	 * @param destination2
	 * @param destination3
	 */
	private int			lastx						= -1;
	private int			lasty						= -1;
	protected boolean	needOnVehicleCheckLocation	= false;
	private int			_id;

	public void updatePeopleInTheBoat(int x, int y, int z)
	{
		if (_inboat != null)
		{
			boolean check = false;
			if ((lastx == -1) || (lasty == -1))
			{
				check = true;
				lastx = x;
				lasty = y;
			}
			else if ((x - lastx) * (x - lastx) + (y - lasty) * (y - lasty) > 2250000) // 1500 sq
			{
				check = true;
				lastx = x;
				lasty = y;
			}
			for (int i = 0; i < _inboat.size(); i++)
			{
				L2PcInstance player = _inboat.get(i);
				if (player != null && player.isInBoat() && player.getBoat() == this)
				{
						player.getPosition().setXYZ(x, y, z);
						player.revalidateZone(false);
				}
				if (check)
				{
					if (needOnVehicleCheckLocation && player != null)
					{
						VehicleCheckLocation vcl = new VehicleCheckLocation(this, x, y, z);
						player.sendPacket(vcl);
					}
				}
			}
		}

	}

	public void begin()
	{
		if (!_inCycle)
			return;
		
		if (_cycle == 1)
		{
			Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
			if (knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2PcInstance>();
				int i = 0;
				for (L2PcInstance player : knownPlayers)
				{
					if (player.isInBoat() && player.getBoat() == this)
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t1.idWTicket1);
						if (it != null)
						{
							player.getInventory().destroyItem("Boat", it.getObjectId(), 1, player, this);
							InventoryUpdate iu = new InventoryUpdate();
							iu.addItem(it);
							player.sendPacket(iu);
							_inboat.put(i, player);
							i++;
						}
						else if (_t1.idWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
							player.teleToLocation(_t1.ntx1, _t1.nty1, _t1.ntz1, false);
					}
				}
			}
			Boatrun bc = new Boatrun(0, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 0);
		}
		else if (_cycle == 2)
		{
			Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
			if (knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2PcInstance>();
				int i = 0;
				for (L2PcInstance player : knownPlayers)
				{
					if (player.isInBoat())
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t2.idWTicket1);
						if (it != null)
						{
							player.getInventory().destroyItem("Boat", it.getObjectId(), 1, player, this);
							InventoryUpdate iu = new InventoryUpdate();
							iu.addItem(it);
							player.sendPacket(iu);
							_inboat.put(i, player);
							i++;
						}
						else if (_t2.idWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
							player.teleToLocation(_t2.ntx1, _t2.nty1, _t2.ntz1, false);
					}
				}

			}
			Boatrun bc = new Boatrun(0, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 0);
		}
	}

	/**
	 * @param i
	 */
	public void say(int i)
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		CreatureSay sm;
		CreatureSay sm2 = null;
		PlaySound ps;
		switch (i)
		{
		case 10:
			if (_cycle == 1)
			{
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmess10_1);
				if (getId() == 5)
					sm2 = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmess5_1);
			}
			else
			{
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmess10_1);
				if (getId() == 5)
					sm2 = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmess5_1);
			}
			ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getX(), getY(), getZ());
			if (knownPlayers == null || knownPlayers.isEmpty())
				return;
			for (L2PcInstance player : knownPlayers)
			{
				player.sendPacket(sm);
				player.sendPacket(ps);
				player.sendPacket(new VehicleStarted(this, 0));
				if (sm2 != null)
					player.sendPacket(sm2);
			}
			break;
		case 5:
			if (_cycle == 1)
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmess5_1);
			else
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmess5_1);
			ps = new PlaySound(0, "itemsound.ship_5min", 1, getObjectId(), getX(), getY(), getZ());
			if (knownPlayers == null || knownPlayers.isEmpty())
				return;
			for (L2PcInstance player : knownPlayers)
			{
				player.sendPacket(sm);
				player.sendPacket(ps);
			}
			break;
		case 1:
			if (_cycle == 1)
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmess1_1);
			else
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmess1_1);
			ps = new PlaySound(0, "itemsound.ship_1min", 1, getObjectId(), getX(), getY(), getZ());
			if (knownPlayers == null || knownPlayers.isEmpty())
				return;
			for (L2PcInstance player : knownPlayers)
			{
				player.sendPacket(sm);
				player.sendPacket(ps);
			}
			break;
		case 0:
			if (_cycle == 1)
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmess0_1);
			else
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmess0_1);
			if (knownPlayers == null || knownPlayers.isEmpty())
				return;
			for (L2PcInstance player : knownPlayers)
				player.sendPacket(sm);
			break;
		case -1:
			if (_cycle == 1)
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t1.npc1, _t1.sysmessb_1);
			else
				sm = new CreatureSay(0, SystemChatChannelId.Chat_Shout, _t2.npc1, _t2.sysmessb_1);
			ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getX(), getY(), getZ());
			for (L2PcInstance player : knownPlayers)
			{
				player.sendPacket(sm);
				player.sendPacket(ps);
			}
			break;
		}
	}

	public void spawn()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		_cycle = 1;
		beginCycle();
		if (knownPlayers == null || knownPlayers.isEmpty())
			return;
		VehicleInfo vi = new VehicleInfo(this);
		for (L2PcInstance player : knownPlayers)
			player.sendPacket(vi);
	}

	/**
	 * @param idWaypoint1
	 * @param idWTicket1
	 * @param ntx1
	 * @param nty1
	 * @param ntz1
	 * @param idnpc1
	 * @param sysmess10_1
	 * @param sysmess5_1
	 * @param sysmess1_1
	 * @param sysmessb_1
	 */
	public L2BoatTrajet setTrajet1(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1,
			String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t1 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
		return _t1;
	}

	public L2BoatTrajet setTrajet2(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1,
			String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t2 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
		return _t2;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Character#getActiveWeaponInstance()
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Character#getActiveWeaponItem()
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Character#getSecondaryWeaponInstance()
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Character#getSecondaryWeaponItem()
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Character#getLevel()
	 */
	@Override
	public int getLevel()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * Allow setup of the boat AI only once 
	 */
	@Override
	public void setAI(L2CharacterAI newAI)
	{
		if (_ai == null)
			_ai = newAI;
	}

	/*
	 * boat AI can't be detached
	 */
	public class AIAccessor extends L2Character.AIAccessor
	{
		@Override
		public void detachAI()
		{
		}
	}

	/**
	 * @param id
	 */
	public void setId(int id)
	{
		_id = id;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
	}
}