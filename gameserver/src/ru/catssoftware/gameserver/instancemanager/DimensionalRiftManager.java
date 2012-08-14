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
package ru.catssoftware.gameserver.instancemanager;

import java.awt.Polygon;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom.SpawnInfo;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.DimensionalRift;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

/**
* Thanks to L2Fortress and balancer.ru - kombat
*/
public class DimensionalRiftManager
{
	protected static Logger 								_log							= Logger.getLogger(DimensionalRiftManager.class.getName());

	
	private static DimensionalRiftManager						_instance;
	private FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>>	_rooms							= new FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>>();
	private final short											DIMENSIONAL_FRAGMENT_ITEM_ID	= 7079;
	private final static int									MAX_PARTY_PER_AREA				= 3;

	public static DimensionalRiftManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DimensionalRiftManager();
			new Quest(635, "RiftQuest", "Dummy Quest shown in players' questlist when inside the rift");
		}

		return _instance;
	}

	private DimensionalRiftManager()
	{
		load();
	}

	public DimensionalRiftRoom getRoom(byte type, byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}

	public boolean isAreaAvailable(byte area)
	{
		FastMap<Byte, DimensionalRiftRoom> tmap = _rooms.get(area);
		if (tmap == null)
			return false;
		int used = 0;
		for (DimensionalRiftRoom room : tmap.values())
		{
			if (room.isUsed())
				used++;
		}
		return used <= MAX_PARTY_PER_AREA;
	}

	public boolean isRoomAvailable(byte area, byte room)
	{
		if (_rooms.get(area) == null || _rooms.get(area).get(room) == null)
			return false;
		return !_rooms.get(area).get(room).isUsed();
	}

	public void load()
	{
		int countGood = 0, countBad = 0;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT, "data/dimensionalRift.xml");
			if (!file.exists())
				throw new IOException();

			Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			byte type, roomId;
			int mobId, x, y, z, delay, count;
			L2NpcTemplate template;
			int xMin = 0, xMax = 0, yMin = 0, yMax = 0, zMin = 0, zMax = 0, xT = 0, yT = 0, zT = 0;
			boolean isBossRoom;
			SpawnInfo TeleportInfo = null;
			for (Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if ("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if ("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());

							for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if ("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());
									Node boss = attrs.getNamedItem("isBossRoom");
									isBossRoom = boss != null && Boolean.parseBoolean(boss.getNodeValue());

									for (Node coord = room.getFirstChild(); coord != null; coord = coord.getNextSibling())
									{
										if ("teleport".equalsIgnoreCase(coord.getNodeName()))
										{
											
											attrs = coord.getAttributes();
											xT = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
											yT = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
											zT = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
											if(roomId!=0) {
												template = NpcTable.getInstance().getTemplate(31865);
												TeleportInfo = new SpawnInfo();
												TeleportInfo._template = template;
												TeleportInfo.x = xT;
												TeleportInfo.y = yT;
												TeleportInfo.delay = 10;
												TeleportInfo.z = zT;
												
											}
										}
										else if ("zone".equalsIgnoreCase(coord.getNodeName()))
										{
											attrs = coord.getAttributes();
											xMin = Integer.parseInt(attrs.getNamedItem("xMin").getNodeValue());
											xMax = Integer.parseInt(attrs.getNamedItem("xMax").getNodeValue());
											yMin = Integer.parseInt(attrs.getNamedItem("yMin").getNodeValue());
											yMax = Integer.parseInt(attrs.getNamedItem("yMax").getNodeValue());
											zMin = Integer.parseInt(attrs.getNamedItem("zMin").getNodeValue());
											zMax = Integer.parseInt(attrs.getNamedItem("zMax").getNodeValue());
										}
									}

									if (!_rooms.containsKey(type))
										_rooms.put(type, new FastMap<Byte, DimensionalRiftRoom>());

									_rooms.get(type).put(roomId,
											new DimensionalRiftRoom(type, roomId, xMin, xMax, yMin, yMax, zMin, zMax, xT, yT, zT, isBossRoom));

									for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											if(TeleportInfo!=null) {
												_rooms.get(type).get(roomId).getSpawnsInfo().add(TeleportInfo);
												TeleportInfo = null;
											}
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());

											template = NpcTable.getInstance().getTemplate(mobId);
											if (template == null)
												_log.warn("Template " + mobId + " not found!");
											if (!_rooms.containsKey(type))
												_log.warn("Type " + type + " not found!");
											else if (!_rooms.get(type).containsKey(roomId))
												_log.warn("Room " + roomId + " in Type " + type + " not found!");

											for (int i = 0; i < count; i++)
											{
												DimensionalRiftRoom riftRoom = _rooms.get(type).get(roomId);
												x = riftRoom.getRandomX();
												y = riftRoom.getRandomY();
												z = riftRoom.getTeleportCoords()[2];

												if (template != null && _rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
												{
													SpawnInfo info = new SpawnInfo();
													info._template = template;
													info.x = x;
													info.y = y;
													info.delay = delay;
													info.z = z;
													_rooms.get(type).get(roomId).getSpawnsInfo().add(info);
													countGood++;
												}
												else
													countBad++;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Error on loading dimensional rift spawns: ", e);
		}
		int typeSize = _rooms.keySet().size();
		int roomSize = 0;

		for (Byte b : _rooms.keySet())
			roomSize += _rooms.get(b).keySet().size();

		_log.info("DimensionalRiftManager: Loaded " + typeSize + " room types with " + roomSize + " rooms.");
		_log.info("DimensionalRiftManager: Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
	}

	public void reload()
	{
		for (Byte b : _rooms.keySet())
		{
			for (int i : _rooms.get(b).keySet())
				_rooms.get(b).get(i).getSpawnsInfo().clear();

			_rooms.get(b).clear();
		}
		_rooms.clear();
		load();
	}

	public boolean checkIfInRiftZone(int x, int y, int z, boolean excludePeaceZone)
	{
		if (excludePeaceZone)
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z) && !_rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);

		return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z);
	}

	public boolean checkIfInPeaceZone(int x, int y, int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}

	public void teleportToWaitingRoom(L2PcInstance player)
	{
		int[] coords = getRoom((byte) 0, (byte) 0).getTeleportCoords();
		player.setInstanceId(0);
		player.teleToLocation(coords[0], coords[1], coords[2]);
	}

	public Location getWaitingRoomTeleport()
	{
		int[] coords = getRoom((byte) 0, (byte) 0).getTeleportCoords();
		return new Location(coords[0], coords[1], coords[2]);
	}

	public void start(L2PcInstance player, byte type, L2NpcInstance npc)
	{
		boolean canPass = true;
			if (!player.isInParty())
			{
			showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
			return;
			}

			if (player.getParty().getPartyLeaderOID() != player.getObjectId())
			{
			showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
			}

			if (player.getParty().isInDimensionalRift())
			{
			handleCheat(player, npc);
			return;
			}

			if (player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
			{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.valueOf(Config.RIFT_MIN_PARTY_SIZE).toString());
			player.sendPacket(html);
			return;
			}
		
			for (L2PcInstance p : player.getParty().getPartyMembers())
			{
			if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
				canPass = false;
			}
		if (!canPass)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
			return;
		}

		L2ItemInstance i;
		for (L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);

			if (i == null)
			{
				canPass = false;
				break;
			}

			if (i.getCount() > 0)
			{
				if (i.getCount() < getNeededItems(type))
					canPass = false;
			}
		}

		if (!canPass)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.toString(getNeededItems(type)));
			player.sendPacket(html);
			return;
		}

		for (L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			p.destroyItem("RiftEntrance", i.getObjectId(), getNeededItems(type), null, false);
		}
		
		byte room = (byte)Rnd.get(1, 9);
		new DimensionalRift(player.getParty(), type, (byte) room);
	}

	public void killRift(DimensionalRift d)
	{
		if (d.getTeleportTimerTask() != null)
			d.getTeleportTimerTask().cancel();
		d.setTeleportTimerTask(null);

		if (d.getTeleportTimerTask() != null)
			d.getTeleportTimerTask().cancel();

		if (d.getSpawnTimerTask() != null)
			d.getSpawnTimerTask().cancel();
		d.setSpawnTimerTask(null);

		if (d.getSpawnTimer() != null)
			d.getSpawnTimer().cancel();
		d.setSpawnTimer(null);
	}

	public static class DimensionalRiftRoom
	{
		public static class SpawnInfo {
			public L2NpcTemplate _template;
			public int x,y,z,delay;
		}
		protected final byte					_type;
		protected final byte					_room;
		private final int						_xMin;
		private final int						_xMax;
		private final int						_yMin;
		private final int						_yMax;
		private final int						_zMin;
		private final int						_zMax;
		private final int[]						_teleportCoords;
		private final Shape						_s;
		private final boolean					_isBossRoom;
		private final FastList<SpawnInfo>			_roomSpawns;
		protected final FastList<L2NpcInstance>	_roomMobs;
		private boolean							_isUsed	= false;

		public DimensionalRiftRoom(byte type, byte room, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, int xT, int yT, int zT, boolean isBossRoom)
		{
			_type = type;
			_room = room;
			_xMin = (xMin + 128);
			_xMax = (xMax - 128);
			_yMin = (yMin + 128);
			_yMax = (yMax - 128);
			_zMin = zMin;
			_zMax = zMax;
			_teleportCoords = new int[]
			{ xT, yT, zT };
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<SpawnInfo>();
			_roomMobs = new FastList<L2NpcInstance>();
			_s = new Polygon(new int[]
			{ xMin, xMax, xMax, xMin }, new int[]
			{ yMin, yMin, yMax, yMax }, 4);
		}

		public int getRandomX()
		{
			return Rnd.get(_xMin, _xMax);
		}

		public int getRandomY()
		{
			return Rnd.get(_yMin, _yMax);
		}

		public int[] getTeleportCoords()
		{
			return _teleportCoords;
		}

		public boolean checkIfInZone(int x, int y, int z)
		{
			return _s.contains(x, y) && z >= _zMin && z <= _zMax;
		}

		public boolean isBossRoom()
		{
			return _isBossRoom;
		}

		public FastList<SpawnInfo> getSpawnsInfo()
		{
			return _roomSpawns;
		}

		/*public void spawn(DimensionalRift DR)
		{
			L2NpcInstance mob = null;
			for (L2Spawn spawn : _roomSpawns)
			{
				mob = spawn.doSpawn();
				mob.setInstanceId(instanceId);
				spawn.startRespawn();
				if (mob instanceof L2RiftBossInstance)
					((L2RiftBossInstance)mob).setDimensionalRift(DR);
			}
		}
		*/
/*		public void unspawn()
		{
			for (L2Spawn spawn : _roomSpawns)
			{
				spawn.stopRespawn();
				if (spawn.getLastSpawn() != null)
					spawn.getLastSpawn().deleteMe();
			}
			_isUsed = false;
		}
*/
		public void setUsed()
		{
			_isUsed = true;
		}

		public boolean isUsed()
		{
			return _isUsed;
		}
	}

	private int getNeededItems(byte type)
	{
		switch (type)
		{
		case 1:
			return Config.RIFT_ENTER_COST_RECRUIT;
		case 2:
			return Config.RIFT_ENTER_COST_SOLDIER;
		case 3:
			return Config.RIFT_ENTER_COST_OFFICER;
		case 4:
			return Config.RIFT_ENTER_COST_CAPTAIN;
		case 5:
			return Config.RIFT_ENTER_COST_COMMANDER;
		case 6:
			return Config.RIFT_ENTER_COST_HERO;
		default:
			return 999999;
		}
	}

	public void showHtmlFile(L2PcInstance player, String file, L2NpcInstance npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(file);
		html.replace("%npc_name%", npc.getName());
		player.sendPacket(html);
	}

	public void handleCheat(L2PcInstance player, L2NpcInstance npc)
	{
		showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
		if (!player.isGM())
		{
			_log.warn("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");
			Util.handleIllegalPlayerAction(player, "Warning! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
		}
	}
}