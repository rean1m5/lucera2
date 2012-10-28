package ru.catssoftware.gameserver.instancemanager.clanhallsiege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javolution.util.FastList;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallSiege;
import ru.catssoftware.gameserver.instancemanager.SiegeGuardManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2SiegeClan;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.L2SiegeClan.SiegeClanType;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.zone.L2SiegeZone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SiegeInfo;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;

public class DevastatedCastleSiege extends ClanHallSiege
{
	protected static Logger 			_log				= Logger.getLogger(DevastatedCastleSiege.class.getName());
	public ClanHall 						_clanhall			= ClanHallManager.getInstance().getClanHallById(34);
	private FastList<L2SiegeClan>			_registeredClans	= new FastList<L2SiegeClan>();	// L2SiegeClan
	private FastList<L2DoorInstance>		_doors				= new FastList<L2DoorInstance>();
	private FastList<String>				_doorDefault		= new FastList<String>();
	private L2SiegeZone 					_zone				= null;	
	private L2MonsterInstance				_questMob			= null;
	protected boolean						_isRegistrationOver	= false;
	private SiegeGuardManager				_siegeGuardManager;	
	private static DevastatedCastleSiege	_instance;

	public static final DevastatedCastleSiege load()
	{
		_log.info("SiegeManager of Devastated Castle");
		if (_instance == null)
			_instance = new DevastatedCastleSiege();
		return _instance;
	}
	public static final DevastatedCastleSiege getInstance()
	{
		if (_instance == null)
			_instance = new DevastatedCastleSiege();
		return _instance;
	}
	private DevastatedCastleSiege()
	{
		long siegeDate=restoreSiegeDate(34);
		Calendar tmpDate=Calendar.getInstance();
		tmpDate.setTimeInMillis(siegeDate);
		setSiegeDate(tmpDate);
		setNewSiegeDate(siegeDate,34,22);
		loadSiegeClan();
		loadDoor();
		_siegeGuardManager = new SiegeGuardManager(_clanhall.getName(),_clanhall.getId(),_clanhall.getOwnerId());
		// Schedule siege auto start
		_startSiegeTask.schedule(1000);
		_isRegistrationOver = false;
	}
	public void startSiege()
	{
		if (!getIsInProgress())
		{
			if (getRegisteredClans().size() <= 0)
			{
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
				sm.addString(_clanhall.getName());
				Announcements.getInstance().announceToAll(sm);
				setNewSiegeDate(getSiegeDate().getTimeInMillis(),34,22);
				_startSiegeTask.schedule(1000);
				_isRegistrationOver = false;
				return;
			}
			setIsInProgress(true);
			_clanhall.setUnderSiege(true);
			_zone.updateSiegeStatus();
			announceToPlayer("Осада Холл Клана: " + _clanhall.getName() + " началась.");
			_isRegistrationOver = true;
			updatePlayerSiegeStateFlags(false);
			spawnDoor();
			_siegeGuardManager.spawnSiegeGuard(); // Spawn siege guard
			
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(35410);
			_questMob = new L2MonsterInstance(IdFactory.getInstance().getNextId(), template);
			_questMob.getStatus().setCurrentHpMp(_questMob.getMaxHp(), _questMob.getMaxMp());
			_questMob.spawnMe(178282,-17623,-2195);
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, 60);
			_endSiegeTask.schedule(1000);
		}
	}
	public void endSiege(L2Character par)
	{
		if (getIsInProgress())
		{
			setIsInProgress(false);
			if (par!=null)
			{
				if (par instanceof L2PcInstance)
				{
					L2PcInstance killer=((L2PcInstance)par);
					if ((killer.getClan()!=null)&& (checkIsRegistered(killer.getClan())))
					{
						ClanHallManager.getInstance().setOwner(_clanhall.getId(), killer.getClan());
						announceToPlayer("Осада Холл Клана: " + _clanhall.getName() + " окончена.");
						announceToPlayer("Владельцем Холл Клана стал "+killer.getClan().getName());
					}
					else
					{
						announceToPlayer("Осада Холл Клана: " + _clanhall.getName() + " окончена.");
						announceToPlayer("Владелец Холл Клана остался прежний");
					}
				}
			}
			else
			{
				announceToPlayer("Осада Холл Клана: " + _clanhall.getName() + " окончена.");
				announceToPlayer("Владелец Холл Клана остался прежний");
				_questMob.doDie(_questMob);
			}
			spawnDoor();
			_clanhall.setUnderSiege(false);
			_zone.updateSiegeStatus();
			updatePlayerSiegeStateFlags(true);
			clearSiegeClan(); // Clear siege clan from db
			if (_clanhall.getOwnerClan()!=null)
				saveSiegeClan(_clanhall.getOwnerClan());
			_siegeGuardManager.unspawnSiegeGuard();

			setNewSiegeDate(getSiegeDate().getTimeInMillis(),34,22);
			_startSiegeTask.schedule(1000);
			_isRegistrationOver = false;
		}
	}
	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;
		for (L2SiegeClan siegeClan : getRegisteredClans())
		{
			if (siegeClan == null)
				continue;

			clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (clear)
					member.setSiegeState((byte) 0);
				else
					member.setSiegeState((byte) 1);
				member.sendPacket(new UserInfo(member));
				member.revalidateZone(true);
			}
		}
	}
	public void spawnDoor()
	{
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if (door.getStatus().getCurrentHp() <= 0)
			{
				door.decayMe(); // Kill current if not killed already
				door = DoorTable.parseLine(_doorDefault.get(i));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
			}
			else if (door.getOpen())
				door.closeMe();
		}
	}
	public final FastList<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	public void registerSiegeZone(L2SiegeZone zone)
	{
		_zone = zone;
	}	
	private void loadDoor()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_door WHERE castleId = ?");
			statement.setInt(1, 34);
			ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				// Create list of the door default for use when respawning dead doors
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";"
						+ rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef"));

				L2DoorInstance door = DoorTable.parseLine(_doorDefault.get(_doorDefault.size() - 1));
				door.setCHDoor(true);
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				_doors.add(door);
				DoorTable.getInstance().putDoor(door);
				door.closeMe();
			}

			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: loadCastleDoor(): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}	
	private final ExclusiveTask _endSiegeTask = new ExclusiveTask() {
		@Override
		protected void onElapsed()
		{
			if (!getIsInProgress())
			{
				cancel();
				return;
			}
			final long timeRemaining = _siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
			if (timeRemaining <= 0)
			{
				endSiege(null);
				cancel();
				return;
			}
			if (3600000 > timeRemaining)
			{
				if (timeRemaining > 120000)
					announceToPlayer(Math.round(timeRemaining / 60000.0) + " минут(а) до конца осады Холл Клана " + _clanhall.getName() + ".");
				else
					announceToPlayer("Осада Холл Клана " + _clanhall.getName() + " закончится через " + Math.round(timeRemaining / 1000.0) + " секунд(а)!");
			}
			int divider;
			if (timeRemaining > 3600000)
				divider = 3600000; // 1 hour
			else if (timeRemaining > 600000)
				divider = 600000; // 10 min
			else if (timeRemaining > 60000)
				divider = 60000; // 1 min
			else if (timeRemaining > 10000)
				divider = 10000; // 10 sec
			else
				divider = 1000; // 1 sec
			schedule(timeRemaining-((timeRemaining-500) / divider * divider));
		}
	};
	private final ExclusiveTask _startSiegeTask = new ExclusiveTask()
	{
		@Override
		protected void onElapsed()
		{
			if (getIsInProgress())
			{
				cancel();
				return;
			}
			if (!getIsRegistrationOver())
			{
				long regTimeRemaining = (getSiegeDate().getTimeInMillis()-(2*3600000)) - System.currentTimeMillis();
				
				if (regTimeRemaining > 0)
				{
					schedule(regTimeRemaining);
					return;
				}
			}
			final long timeRemaining = getSiegeDate().getTimeInMillis() - System.currentTimeMillis();
			if (timeRemaining <= 0)
			{
				startSiege();
				cancel();
				return;
			}
			if (86400000 > timeRemaining)
			{
				if (!getIsRegistrationOver())
				{
					_isRegistrationOver = true;
					announceToPlayer("Период регистрации на осаду Холл Клана " + _clanhall.getName() + " окончен.");
				}
				if (timeRemaining > 7200000)
					announceToPlayer(Math.round(timeRemaining / 3600000.0) + " часов до начала осады Холл Клана: " + _clanhall.getName() + ".");
				
				else if (timeRemaining > 120000)
					announceToPlayer(Math.round(timeRemaining / 60000.0) + " минут до начала осады Холл Клана: " + _clanhall.getName() + ".");
				
				else
					announceToPlayer("Осада Холл Клана: " + _clanhall.getName() + " начнется через " + Math.round(timeRemaining / 1000.0) + " секунд!");
			}
			int divider;
			if (timeRemaining > 86400000)
				divider = 86400000; // 1 day
			else if (timeRemaining > 3600000)
				divider = 3600000; // 1 hour
			else if (timeRemaining > 600000)
				divider = 600000; // 10 min
			else if (timeRemaining > 60000)
				divider = 60000; // 1 min
			else if (timeRemaining > 10000)
				divider = 10000; // 10 sec
			else
				divider = 1000; // 1 sec
			schedule(timeRemaining-((timeRemaining-500) / divider * divider));
		}
	};
	
	public FastList<L2SiegeClan> getRegisteredClans()
	{
		return _registeredClans;
	}
	public void registerClan(L2PcInstance player)
	{
		if ((player.getClan() != null) && checkIfCanRegister(player))
			saveSiegeClan(player.getClan()); // Save to database
	}
	public void removeSiegeClan(L2PcInstance player)
	{
		L2Clan clan = player.getClan();
		if (clan == null || clan == _clanhall.getOwnerClan() || !checkIsRegistered(clan))
			return;
		removeSiegeClan(clan.getClanId());
	}	
	private boolean checkIfCanRegister(L2PcInstance player)
	{
		L2Clan clan = player.getClan();
		if (clan == null || clan.getLevel() < 4)
		{
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_WRONG_CLAN_LEVEL),"4"));
			return false;
		}
		else if (getIsRegistrationOver())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DEADLINE_FOR_SIEGE_S1_PASSED);
			sm.addString(_clanhall.getName());
			player.sendPacket(sm);
			return false;
		}
		else if (getIsInProgress())
		{
			player.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
			return false;
		}
		else if (clan.getClanId() == _clanhall.getOwnerId())
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CLAN_AUTO_REGISTERED));
			return false;
		}
		else
		{
			if (checkIsRegistered(player.getClan()))
			{
				player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
				return false;
			}
			if (FortressOfDeadSiege.getInstance().checkIsRegistered(player.getClan()))
			{
				player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
				return false;
			}
		}
		if (getRegisteredClans().size() >= Config.SIEGE_MAX_ATTACKER)
		{
			player.sendPacket(SystemMessageId.ATTACKER_SIDE_FULL);
			return false;
		}		
		return true;
	}
	public final boolean checkIsRegistered(L2Clan clan)
	{
		if (clan == null)
			return false;
		return SiegeManager.getInstance().checkIsRegistered(clan, _clanhall.getId());
	}	
	public void saveSiegeClan(L2Clan clan)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) VALUES (?,?,?,0)");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, _clanhall.getId());
			statement.setInt(3, 1);
			statement.execute();
			statement.close();
			SiegeManager.getInstance().registerClan(_clanhall.getId(), clan);
			addAttacker(clan.getClanId());
			announceToPlayer(clan.getName()+" зарегистрирован на атаку Холл Клана: "+_clanhall.getName());
		}
		catch (Exception e)
		{
			_log.error("Exception: saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}		
	}
	private void loadSiegeClan()
	{
		Connection con = null;
		try
		{
			getRegisteredClans().clear();
			if (_clanhall.getOwnerId() > 0)
				addAttacker(_clanhall.getOwnerId());
			PreparedStatement statement = null;
			ResultSet rs = null;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?");
			statement.setInt(1, _clanhall.getId());
			rs = statement.executeQuery();

			int typeId;
			int clanId;
			while (rs.next())
			{
				typeId = rs.getInt("type");
				clanId =rs.getInt("clan_id");
				if (typeId == 1)
					addAttacker(clanId);
			}

			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}		
	}
	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
			return;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?");
			statement.setInt(1, _clanhall.getId());
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			SiegeManager.getInstance().removeClan(_clanhall.getId(), clanId);
			loadSiegeClan();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	public void clearSiegeClan()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?");
			statement.setInt(1, _clanhall.getId());
			statement.execute();
			statement.close();

			this.getRegisteredClans().clear();
		}
		catch (Exception e)
		{
			_log.error("Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}	
	private void addAttacker(int clanId)
	{
		getRegisteredClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER));
	}	
	public void announceToPlayer(String message)
	{
		// Get all players
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendMessage(message);
		}
	}	
	public final boolean getIsRegistrationOver()
	{
		return _isRegistrationOver;
	}	
	public void listRegisterClan(L2PcInstance player)
	{
		player.sendPacket(new SiegeInfo(null,_clanhall,getSiegeDate()));
	}
}