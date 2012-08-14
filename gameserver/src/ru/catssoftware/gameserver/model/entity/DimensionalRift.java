package ru.catssoftware.gameserver.model.entity;

import java.util.Timer;
import java.util.TimerTask;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom;
import ru.catssoftware.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom.SpawnInfo;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RiftBossInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;

public class DimensionalRift
{
	protected FastList<Byte>			_completedRooms			= new FastList<Byte>();
	protected FastList<L2PcInstance>	deadPlayers				= new FastList<L2PcInstance>();
	protected FastList<L2PcInstance>	revivedInWaitingRoom	= new FastList<L2PcInstance>();
	private boolean						_hasJumped				= false;
	private FastMap<Byte, FastList<L2Spawn>> _spawns			= new FastMap<Byte, FastList<L2Spawn>>();
	private boolean						isBossRoom				= false;
	private static final long			seconds_5				= 5000L;
	protected byte						jumps_current			= 0;
	protected byte						_choosenRoom			= -1;
	private ExclusiveTask				teleporterTimerTask;
	private Timer						spawnTimer;
	private TimerTask					spawnTimerTask;
	protected byte						_roomType;
	protected L2Party					_party;
	protected long						_jumpTime;
	private	int 						_instanceid;
	public DimensionalRift(L2Party party, byte roomType, byte roomId)
	{
		_instanceid = InstanceManager.getInstance().createDynamicInstance(null);
		Location loc = DimensionalRiftManager.getInstance().getWaitingRoomTeleport();
		InstanceManager.getInstance().getInstance(_instanceid).setReturnTeleport(loc.getX(), loc.getY(), loc.getZ());
		_roomType = roomType;
		_party = party;
		_choosenRoom = roomId;
		int[] coords = getRoomCoord(roomId);
		party.setDimensionalRift(this);

		Quest riftQuest = QuestManager.getInstance().getQuest(635);
		for (L2PcInstance p : party.getPartyMembers())
		{
			if (riftQuest != null)
			{
				QuestState qs = p.getQuestState(riftQuest.getName());
				if (qs == null)
					qs = riftQuest.newQuestState(p);
				if (qs.getInt("cond") != 1)
					qs.set("cond", "1");
			}
			p.setInstanceId(_instanceid);
			p.teleToLocation(coords[0], coords[1], coords[2]);
		}
		createSpawnTimer(_choosenRoom);
	}

	public byte getType()
	{
		return _roomType;
	}

	public byte getCurrentRoom()
	{
		return _choosenRoom;
	}

	private void unspawnRoom(byte room) {
		FastList<L2Spawn> _roomSpawn = _spawns.get(room);
		if(_roomSpawn!=null) {
			for(L2Spawn spawn : _roomSpawn) {
				spawn.stopRespawn();
				if(spawn.getLastSpawn()!=null)
					spawn.getLastSpawn().deleteMe();
			}
			_roomSpawn.clear();
		}
		
		
	}
	protected void createTeleporterTimer(final boolean reasonTP)
	{
		if (teleporterTimerTask != null)
		{
			teleporterTimerTask.cancel();
			teleporterTimerTask = null;
		}

		teleporterTimerTask = new ExclusiveTask()
		{
			@Override
			public void onElapsed()
			{
				if(_jumpTime>0) {
					_jumpTime-=1000;
					this.schedule(1000);
					return;
				}
				if (_choosenRoom > -1) {
					unspawnRoom(_choosenRoom);
//					DimensionalRiftManager.getInstance().getRoom(_roomType, _choosenRoom).unspawn();
				}

				if (reasonTP && jumps_current < getMaxJumps() && _party.getMemberCount() > deadPlayers.size())
				{
					jumps_current++;

					_completedRooms.add(_choosenRoom);
					_choosenRoom = -1;

					for (L2PcInstance p : _party.getPartyMembers())
					{
						if (!revivedInWaitingRoom.contains(p))
							teleportToNextRoom(p);
					}
					createSpawnTimer(_choosenRoom);
					
				}
				else
				{
					for (L2PcInstance p : _party.getPartyMembers())
					{
						if (!revivedInWaitingRoom.contains(p))
							teleportToWaitingRoom(p);
					}
					killRift();
					cancel();
				}
			}
		};

		_jumpTime =  calcTimeToNextJump();
		if (reasonTP)
			teleporterTimerTask.schedule(1000); //Teleporter task, 8-10 minutes
		else {
			_jumpTime = 0;
			teleporterTimerTask.schedule(seconds_5); //incorrect party member invited.
		}
	}

	public void createSpawnTimer(final byte room)
	{
		if (spawnTimerTask != null)
		{
			spawnTimerTask.cancel();
			spawnTimerTask = null;
		}

		if (spawnTimer != null)
		{
			spawnTimer.cancel();
			spawnTimer = null;
		}

		final DimensionalRiftRoom riftRoom = DimensionalRiftManager.getInstance().getRoom(_roomType, room);
		final DimensionalRift DR = this;
		riftRoom.setUsed();
		spawnTimer = new Timer();
		spawnTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				FastList<L2Spawn> _roomspawn = _spawns.get(room);
				if(_roomspawn==null) {
					_roomspawn = new FastList<L2Spawn>();
					_spawns.put(room, _roomspawn);
				}
				for(SpawnInfo info : riftRoom.getSpawnsInfo()) {
					L2Spawn spawn = new L2Spawn(info._template);
					spawn.setInstanceId(_instanceid);
					spawn.setLocx(info.x);
					spawn.setLocy(info.y);
					spawn.setLocz(info.z);
					spawn.setAmount(1);
					spawn.setRespawnDelay(info.delay);
					spawn.setHeading(-1);
					_roomspawn.add(spawn);
					L2NpcInstance mob = spawn.doSpawn();
					
					if (mob instanceof L2RiftBossInstance) {
						isBossRoom = true;
						((L2RiftBossInstance)mob).setDimensionalRift(DR);
					}
					else
						spawn.startRespawn();
					
					
				}
				createTeleporterTimer(true);
			}
		};

		spawnTimer.schedule(spawnTimerTask, Config.RIFT_SPAWN_DELAY);
	}

	public void partyMemberInvited()
	{
		createTeleporterTimer(false);
	}

	public void partyMemberExited(L2PcInstance player)
	{
		if (deadPlayers.contains(player))
			deadPlayers.remove(player);

		if (revivedInWaitingRoom.contains(player))
			revivedInWaitingRoom.remove(player);

		if (_party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE || _party.getMemberCount() == 1)
		{
			for (L2PcInstance p : _party.getPartyMembers())
				teleportToWaitingRoom(p);
			killRift();
		}
	}

	public void manualTeleport(L2PcInstance player, L2NpcInstance npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}

		if (_hasJumped)
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/AlreadyTeleported.htm", npc);
			return;
		}

		_hasJumped = true;

		unspawnRoom(_choosenRoom);
		_completedRooms.add(_choosenRoom);
		_choosenRoom = -1;

		for (L2PcInstance p : _party.getPartyMembers())
			teleportToNextRoom(p);

		createSpawnTimer(_choosenRoom);
	}

	public void manualExitRift(L2PcInstance player, L2NpcInstance npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}

		for (L2PcInstance p : player.getParty().getPartyMembers())
			teleportToWaitingRoom(p);
		killRift();
	}

	protected synchronized void teleportToNextRoom(L2PcInstance player)
	{
		if (_choosenRoom == -1)
		{ //Do not tp in the same room a second time
			do
				_choosenRoom = (byte) Rnd.get(1, 9);
			while (_completedRooms.contains(_choosenRoom));
		}

		checkBossRoom(_choosenRoom);
		int[] coords = getRoomCoord(_choosenRoom);
		player.setInstanceId(_instanceid);
		player.teleToLocation(coords[0], coords[1], coords[2]);
	}

	protected void teleportToWaitingRoom(L2PcInstance player)
	{
		DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		Quest riftQuest = QuestManager.getInstance().getQuest(635);
		if (riftQuest != null)
		{
			QuestState qs = player.getQuestState(riftQuest.getName());
			if (qs != null && qs.getInt("cond") == 1)
				qs.set("cond", "0");
		}
	}

	public void killRift()
	{
		_completedRooms = null;
		
		if (_party != null)
			_party.setDimensionalRift(null);

		_party = null;
		revivedInWaitingRoom = null;
		deadPlayers = null;
		unspawnRoom(_choosenRoom);
		DimensionalRiftManager.getInstance().killRift(this);
		InstanceManager.getInstance().destroyInstance(_instanceid);
	}

	public ExclusiveTask getTeleportTimerTask()
	{
		return teleporterTimerTask;
	}

	public Timer getSpawnTimer()
	{
		return spawnTimer;
	}

	public TimerTask getSpawnTimerTask()
	{
		return spawnTimerTask;
	}

	public void setTeleportTimerTask(ExclusiveTask tt)
	{
		teleporterTimerTask = tt;
	}

	public void setSpawnTimer(Timer t)
	{
		spawnTimer = t;
	}

	public void setSpawnTimerTask(TimerTask st)
	{
		spawnTimerTask = st;
	}

	private long calcTimeToNextJump()
	{
		int time = Rnd.get(Config.RIFT_AUTO_JUMPS_TIME_MIN, Config.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;
		checkBossRoom(_choosenRoom);
		if (isBossRoom) {
			time *= Config.RIFT_BOSS_ROOM_TIME_MUTIPLY;
		}
		return time;
	}

	public void memberDead(L2PcInstance player)
	{
		if (player == null || _party == null)
			return;

		if (!deadPlayers.contains(player))
			deadPlayers.add(player);

		if (_party.getMemberCount() == deadPlayers.size())
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					for (L2PcInstance p : _party.getPartyMembers())
						if (!revivedInWaitingRoom.contains(p))
							teleportToWaitingRoom(p);
					killRift();
				}
			}, 5000);
		}
	}

	public void memberRessurected(L2PcInstance player)
	{
		if (deadPlayers.contains(player))
			deadPlayers.remove(player);
	}

	public void usedTeleport(L2PcInstance player)
	{
		if (player == null)
			return;

		if (!revivedInWaitingRoom.contains(player))
			revivedInWaitingRoom.add(player);

		if (!deadPlayers.contains(player))
			deadPlayers.add(player);

		if (_party == null)
			return;

		if (_party.getMemberCount() - revivedInWaitingRoom.size() < Config.RIFT_MIN_PARTY_SIZE)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					if(_party!=null)
						for (L2PcInstance p : _party.getPartyMembers())
						{
							if (p != null)
							{
								if (!revivedInWaitingRoom.contains(p))
									teleportToWaitingRoom(p);
							}
						}
					killRift();
				}
			}, 5000);
		}
	}

	public FastList<L2PcInstance> getDeadMemberList()
	{
		return deadPlayers;
	}

	public FastList<L2PcInstance> getRevivedAtWaitingRoom()
	{
		return revivedInWaitingRoom;
	}

	public void checkBossRoom(byte roomId)
	{
		isBossRoom = DimensionalRiftManager.getInstance().getRoom(_roomType, roomId).isBossRoom();
	}

	public int[] getRoomCoord(byte roomId)
	{
		return DimensionalRiftManager.getInstance().getRoom(_roomType, roomId).getTeleportCoords();
	}

	public byte getMaxJumps()
	{
		if (Config.RIFT_MAX_JUMPS <= 8 && Config.RIFT_MAX_JUMPS >= 1)
			return (byte) Config.RIFT_MAX_JUMPS;

		return 4;
	}
}