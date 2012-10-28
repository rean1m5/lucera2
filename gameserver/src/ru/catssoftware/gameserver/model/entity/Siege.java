package ru.catssoftware.gameserver.model.entity;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.MercTicketManager;
import ru.catssoftware.gameserver.instancemanager.SiegeGuardManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager.SiegeSpawn;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.L2SiegeClan.SiegeClanType;
import ru.catssoftware.gameserver.model.actor.instance.L2ControlTowerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2SiegeZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SiegeInfo;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.gameserver.util.Broadcast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Set;


public class Siege
{
	private final static Logger	_log	= Logger.getLogger(Siege.class.getName());

	public static enum TeleportWhoType
	{
		All, Attacker, DefenderNotOwner, Owner, Spectator
	}

	private int	_controlTowerCount;
	private int	_controlTowerMaxCount;

    // ===============================================================
    // Schedule task
	
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
				endSiege();
				cancel();
				return;
			}
			
			if (3600000 > timeRemaining)
			{
				if (timeRemaining > 120000)
					announceToPlayer(Math.round(timeRemaining / 60000.0) + " минут(а) до конца осады замка " + getCastle().getName() + ".", true);
				
				else
					announceToPlayer("Осада замка " + getCastle().getName() + " закончится через " + Math.round(timeRemaining / 1000.0) + " секунд(а)!", true);
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

			if (!getIsTimeRegistrationOver())
			{
				long regTimeRemaining = getTimeRegistrationOverDate().getTimeInMillis() - System.currentTimeMillis();
				
				if (regTimeRemaining > 0)
				{
					schedule(regTimeRemaining);
					return;
				}
				
				endTimeRegistration(true);
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
				if (!_isRegistrationOver)
				{
					_isRegistrationOver = true;
					announceToPlayer("Период регистрации на атаку замка " + getCastle().getName() + " окончен.", false);
					clearSiegeWaitingClan();
				}
				
				if (timeRemaining > 7200000)
					announceToPlayer(Math.round(timeRemaining / 3600000.0) + " часов до начала осады замка: " + getCastle().getName() + ".", false);
				
				else if (timeRemaining > 120000)
					announceToPlayer(Math.round(timeRemaining / 60000.0) + " минут до начала осады замка: " + getCastle().getName() + ".", false);
				
				else
					announceToPlayer("Осада замка: " + getCastle().getName() + " начнется через " + Math.round(timeRemaining / 1000.0) + " секунд!", false);
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
	
	// =========================================================
	// Data Field
	// Attacker and Defender
	private FastList<L2SiegeClan>				_attackerClans			= new FastList<L2SiegeClan>();				// L2SiegeClan

	private FastList<L2SiegeClan>				_defenderClans			= new FastList<L2SiegeClan>();				// L2SiegeClan
	private FastList<L2SiegeClan>				_defenderWaitingClans	= new FastList<L2SiegeClan>();				// L2SiegeClan

	// Castle setting
	private FastList<L2ControlTowerInstance>	_controlTowers			= new FastList<L2ControlTowerInstance>();

	private Castle								_castle;
	private boolean								_isInProgress			= false;
	private boolean								_isNormalSide			= true;		// true = Atk is Atk, false = Atk is Def
	protected boolean							_isRegistrationOver		= false;
	protected Calendar							_siegeEndDate;
	private SiegeGuardManager					_siegeGuardManager;
	private int oldOwner = 0; // 0 - NPC, > 0 - clan
	
	public Siege(Castle castle)
	{
		_castle = castle;
		_siegeGuardManager = new SiegeGuardManager(getCastle().getName(),getCastle().getCastleId(),getCastle().getOwnerId());
		startAutoTask();
	}

	/**
	 * When siege ends<BR><BR>
	 */
	public void endSiege()
	{
		if (getIsInProgress())
		{
			announceToPlayer(new SystemMessage(SystemMessageId.CASTLE_SIEGE_HAS_ENDED), true);
			SystemMessage sm;
			sm = new SystemMessage(SystemMessageId.SIEGE_OF_S1_FINISHED);
			sm.addString(getCastle().getName());
			announceToPlayer(sm, false);
			announceToParticipants(SystemMessageId.TEMPORARY_ALLIANCE_DISSOLVED.getSystemMessage());

			if (getCastle().getOwnerId() <= 0)
			{
				sm = new SystemMessage(SystemMessageId.SIEGE_S1_DRAW);
				sm.addString(getCastle().getName());
				announceToPlayer(sm, false);
			}
			_castle.setBloodAliance(0);			
			if (oldOwner != getCastle().getOwnerId())
			{
				announceToPlayer(new SystemMessage(SystemMessageId.NEW_CASTLE_LORD), true); //is it really true?
				sm = new SystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE);
				sm.addString(ClanTable.getInstance().getClan(getCastle().getOwnerId()).getName());
				sm.addString(getCastle().getName());
				announceToPlayer(sm, false);
			}
			else if (oldOwner>0)
			{
				// Owner is unchanged
				_castle.setBloodAliance(Config.SIEGE_BLOODALIANCE_REWARD_CNT);
			}
			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
			teleportPlayer(Siege.TeleportWhoType.Attacker, TeleportWhereType.Town); // Teleport to the second closest town
			teleportPlayer(Siege.TeleportWhoType.DefenderNotOwner, TeleportWhereType.Town); // Teleport to the second closest town
			teleportPlayer(Siege.TeleportWhoType.Spectator, TeleportWhereType.Town); // Teleport to the second closest town
			_isInProgress = false; // Flag so that siege instance can be started
			updatePlayerSiegeStateFlags(true);
			getZone().updateSiegeStatus();
			saveCastleSiege(); // Save castle specific data
			clearSiegeClan(); // Clear siege clan from db
			removeControlTower(); // Remove all control tower from this castle
			_siegeGuardManager.unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
			if (getCastle().getOwnerId() > 0)
				_siegeGuardManager.removeMercs();
			getCastle().spawnDoor(); // Respawn door to castle
		}
	}

	private void removeDefender(L2SiegeClan sc)
	{
		if (sc != null)
			getDefenderClans().remove(sc);
	}

	private void removeAttacker(L2SiegeClan sc)
	{
		if (sc != null)
			getAttackerClans().remove(sc);
	}

	private void addDefender(L2SiegeClan sc, SiegeClanType type)
	{
		if (sc == null)
			return;
		sc.setType(type);
		getDefenderClans().add(sc);
	}

	private void addAttacker(L2SiegeClan sc)
	{
		if (sc == null)
			return;
		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}

	/**
	 * When control of castle changed during siege<BR><BR>
	 */
	public void midVictory()
	{
		if (getIsInProgress()) // Siege still in progress
		{
			if (getCastle().getOwnerId() > 0)
				_siegeGuardManager.removeMercs(); // Remove all merc entry from db

			if (getDefenderClans().size() == 0 && // If defender doesn't exist (Pc vs Npc)
					getAttackerClans().size() == 1 // Only 1 attacker
			)
			{
				L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);
				endSiege();
				return;
			}
			if (getCastle().getOwnerId() > 0)
			{
				int allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
				if (getDefenderClans().size() == 0) // If defender doesn't exist (Pc vs Npc)
				// and only an alliance attacks
				{
					// The player's clan is in an alliance
					if (allyId != 0)
					{
						boolean allinsamealliance = true;
						for (L2SiegeClan sc : getAttackerClans())
						{
							if (sc != null)
							{
								if (ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId)
									allinsamealliance = false;
							}
						}
						if (allinsamealliance)
						{
							L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
							removeAttacker(sc_newowner);
							addDefender(sc_newowner, SiegeClanType.OWNER);
							endSiege();
							return;
						}
					}
				}

				for (L2SiegeClan sc : getDefenderClans())
				{
					if (sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}

				L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);

				// The player's clan is in an alliance
				if (allyId != 0)
				{
					L2Clan[] clanList = ClanTable.getInstance().getClans();

					for (L2Clan clan : clanList)
					{
						if (clan.getAllyId() == allyId)
						{
							L2SiegeClan sc = getAttackerClan(clan.getClanId());
							if (sc != null)
							{
								removeAttacker(sc);
								addDefender(sc, SiegeClanType.DEFENDER);
							}
						}
					}
				}
				for(L2Character cha : getZone().getCharactersInside().values()) {
					if(cha instanceof L2PcInstance)
						if(!checkIsDefender(((L2PcInstance)cha).getClan()))
								cha.teleToLocation(TeleportWhereType.Town);
						else
							cha.broadcastFullInfo();
				}
				removeDefenderFlags(); // Removes defenders' flags
				getCastle().removeUpgrade(); // Remove all castle upgrade
				getCastle().spawnDoor(true); // Respawn door to castle but make them weaker (50% hp)
				removeControlTower(); // Remove all control tower from this castle
				_controlTowerCount = 0;//Each new siege midvictory CT are completely respawned.
				_controlTowerMaxCount = 0;
				spawnControlTower(getCastle().getCastleId());
				updatePlayerSiegeStateFlags(false);
				announceToParticipants(SystemMessageId.TEMPORARY_ALLIANCE.getSystemMessage());
			}
		}
	}

	/**
	 * When siege starts<BR><BR>
	 */
	public void startSiege()
	{
		if (!getIsInProgress())
		{
			if (getAttackerClans().size() <= 0)
			{
				SystemMessage sm;
				if (getCastle().getOwnerId() <= 0)
					sm = new SystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
				else
					sm = new SystemMessage(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
				sm.addString(getCastle().getName());
				Announcements.getInstance().announceToAll(sm);
				saveCastleSiege();
				return;
			}

			_isNormalSide = true; // Atk is now atk
			_isInProgress = true; // Flag so that same siege instance cannot be started again
			loadSiegeClan(true); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			for(L2Character cha : getZone().getCharactersInside().values()) {
				if(cha instanceof L2PcInstance) {
					L2PcInstance pc = (L2PcInstance)cha;
					if(!checkIsDefender(pc.getClan()))
						pc.teleToLocation(TeleportWhereType.Town);
					else
						pc.broadcastFullInfo();
				}
			}

			_controlTowerCount = 0;
			_controlTowerMaxCount = 0;
			spawnControlTower(getCastle().getCastleId()); // Spawn control tower
			getCastle().resetArtifact(); // artifact prepair
			getCastle().spawnDoor(); // Spawn door
			spawnSiegeGuard(); // Spawn siege guard
			MercTicketManager.getInstance().deleteTickets(getCastle().getCastleId()); // remove the tickets from the ground
			getZone().updateSiegeStatus();
			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, Config.SIEGE_LENGTH_MINUTES);
			_endSiegeTask.schedule(1000);

			announceToPlayer(new SystemMessage(SystemMessageId.CASTLE_SIEGE_HAS_BEGUN), true);
			SystemMessage sm;
			sm = new SystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_STARTED);
			sm.addString(getCastle().getName());
			announceToPlayer(sm, false);
			sm = new SystemMessage(SystemMessageId.S1);
			sm.addString("Вы зарегистрированы на осаду "+getCastle().getName());
			announceToParticipants(sm);
			oldOwner = getCastle().getOwnerId();
		}
	}

	/**
	 * Announce to player.<BR><BR>
	 * @param message The String of the message to send to player
	 * @param inAreaOnly The boolean flag to show message to players in area only.
	 */
	public void announceToPlayer(String message, boolean inAreaOnly)
	{
		// Get all players
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (!inAreaOnly || (inAreaOnly && checkIfInZone(player.getX(), player.getY(), player.getZ())))
				player.sendMessage(message);
		}
	}

	public void announceToPlayer(SystemMessage sm, boolean inAreaOnly)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (!inAreaOnly || (inAreaOnly && checkIfInZone(player.getX(), player.getY(), player.getZ())))
				player.sendPacket(sm);
		}
	}

	public void announceToParticipants(SystemMessage sm)
	{
		for(L2SiegeClan siegeclan : getAttackerClans())
			if(ClanTable.getInstance().getClan(siegeclan.getClanId())!=null)
				ClanTable.getInstance().getClan(siegeclan.getClanId()).broadcastToOnlineMembers(sm);
		for(L2SiegeClan siegeclan : getDefenderClans())
			if(ClanTable.getInstance().getClan(siegeclan.getClanId())!=null)
				ClanTable.getInstance().getClan(siegeclan.getClanId()).broadcastToOnlineMembers(sm);
	}

	public void announceToOpponent(SystemMessage sm, boolean toAtk) {
		FastList<L2SiegeClan> clans = (toAtk ? getAttackerClans() : getDefenderClans());
		for (L2SiegeClan siegeclan : clans)
			ClanTable.getInstance().getClan(siegeclan.getClanId()).broadcastToOnlineMembers(sm);
	}

	public void announceToOpponent(SystemMessage sm, L2Clan self)
	{
		if (self != null) {
			boolean atk = true;
			if (getAttackerClan(self) != null)
				atk = false;
			else if (getDefenderClan(self) == null)
				return;
			announceToOpponent(sm, atk);
		}
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;
		for (L2SiegeClan siegeClan : getAttackerClans())
		{
			if (siegeClan == null)
				continue;

			clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if(clan==null)
				continue;
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (clear)
					member.setSiegeState((byte) 0);
				else
					member.setSiegeState((byte) 1);
				member.sendPacket(new UserInfo(member));
				member.revalidateZone(true);
				L2SiegeStatus.getInstance().addStatus(member.getClanId(), member.getObjectId());
			}
		}
		for (L2SiegeClan siegeClan : getDefenderClans())
		{
			if (siegeClan == null)
				continue;

			clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (clear)
					member.setSiegeState((byte) 0);
				else
					member.setSiegeState((byte) 2);
				member.sendPacket(new UserInfo(member));
				member.revalidateZone(true);
				L2SiegeStatus.getInstance().addStatus(member.getClanId(), member.getObjectId());
			}
		}
	}

	/**
	 * Approve clan as defender for siege<BR><BR>
	 * @param clanId The int of player's clan id
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if (clanId <= 0)
			return;
		saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
		loadSiegeClan(false);
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(L2Object object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y, int z)
	{
		Town town = TownManager.getInstance().getTown(x, y, z);
		return (getIsInProgress() && (getCastle().checkIfInZone(x, y, z) || getZone().isInsideZone(x, y) || (town != null && getCastle().getCastleId() == town.getCastleId())));
	}

	/**
	 * Return true if clan is attacker<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public boolean checkIsAttacker(L2Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}

	/**
	 * Return true if clan is defender<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public boolean checkIsDefender(L2Clan clan)
	{
		if(clan==null)
			return false;
		return (getDefenderClan(clan) != null);
	}

	/**
	 * Return true if clan is defender waiting approval<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public boolean checkIsDefenderWaiting(L2Clan clan)
	{
		return (getDefenderWaitingClan(clan) != null);
	}

	/** Clear all registered siege clans from database for castle */
	public void clearSiegeClan()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();

			if (getCastle().getOwnerId() > 0)
			{
				PreparedStatement statement2 = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?");
				statement2.setInt(1, getCastle().getOwnerId());
				statement2.execute();
				statement2.close();
			}

			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();
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

	/** Clear all siege clans waiting for approval from database for castle */
	public void clearSiegeWaitingClan()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();

			getDefenderWaitingClans().clear();
		}
		catch (Exception e)
		{
			_log.error("Exception: clearSiegeWaitingClan(): " + e.getMessage(), e);
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

	/** Return list of L2PcInstance registered as attacker in the zone. */
	public FastList<L2PcInstance> getAttackersInZone()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;
		for (L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if(clan==null)
				continue;
			for (L2PcInstance player : clan.getOnlineMembers(0))
			{
				if (checkIfInZone(player.getX(), player.getY(), player.getZ()))
					players.add(player);
			}
		}
		return players;
	}

	/** Return list of L2PcInstance registered as defender but not owner in the zone. */
	public FastList<L2PcInstance> getDefendersButNotOwnersInZone()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;
		for (L2SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan.getClanId() == getCastle().getOwnerId())
				continue;
			for (L2PcInstance player : clan.getOnlineMembers(0))
			{
				if (checkIfInZone(player.getX(), player.getY(), player.getZ()))
					players.add(player);
			}
		}
		return players;
	}

	/** Return list of L2PcInstance in the zone. */
	public FastList<L2PcInstance> getPlayersInZone()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();

		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			// quick check from player states, which don't include siege number however
			if (!player.isInsideZone(L2Zone.FLAG_SIEGE))
				continue;
			if (checkIfInZone(player.getX(), player.getY(), player.getZ()))
				players.add(player);
		}

		return players;
	}

	/** Return list of L2PcInstance owning the castle in the zone. */
	public FastList<L2PcInstance> getOwnersInZone()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;
		for (L2SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan.getClanId() != getCastle().getOwnerId())
				continue;
			for (L2PcInstance player : clan.getOnlineMembers(0))
			{
				if (checkIfInZone(player.getX(), player.getY(), player.getZ()))
					players.add(player);
			}
		}
		return players;
	}

	/** Return list of L2PcInstance not registered as attacker or defender in the zone. */
	public FastList<L2PcInstance> getSpectatorsInZone()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();

		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			// quick check from player states, which don't include siege number however
			if (!player.isInsideZone(L2Zone.FLAG_SIEGE) || player.getSiegeState() != 0)
				continue;
			if (checkIfInZone(player.getX(), player.getY(), player.getZ()))
				players.add(player);
		}

		return players;
	}

	/** Control Tower was skilled
	 * @param ct */
	public void killedCT(L2NpcInstance ct)
	{
		_controlTowerCount--;
		if (_controlTowerCount < 0)
			_controlTowerCount = 0;
	}

	/** Remove the flag that was killed */
	public void killedFlag(L2NpcInstance flag)
	{
		if (flag == null)
			return;
		for (L2SiegeClan clan : getAttackerClans())
		{
			if (clan.removeFlag(flag))
				return;
		}
	}

	/** Display list of registered clans */
	public void listRegisterClan(L2PcInstance player)
	{
		player.sendPacket(new SiegeInfo(getCastle(),null,null));
	}

	/**
	 * Register clan as attacker<BR><BR>
	 * @param player The L2PcInstance of the player trying to register
	 */
	public void registerAttacker(L2PcInstance player)
	{
		registerAttacker(player, false);
	}
	
	public void registerToSiege(L2PcInstance player, int id, int mode)
	{
		if (player.getClan() != null && !SiegeManager.getInstance().checkIsRegistered(player.getClan(), id))
			saveSiegeClan(player.getClan(), mode, false);
	}

	public void registerAttacker(L2PcInstance player, boolean force)
	{
		if (!force)
		{
			int allyId = 0;
			if (getCastle().getOwnerId() != 0)
				allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();

			if (allyId != 0 && player.getClan().getAllyId() == allyId)
			{
				player.sendPacket(SystemMessageId.CANNOT_ATTACK_ALLIANCE_CASTLE);
				return;
			}
		}

		if ((force && player.getClan() != null) || checkIfCanRegister(player, 1))
			saveSiegeClan(player.getClan(), 1, false); // Save to database
	}

	/**
	 * Register clan as defender<BR><BR>
	 * @param player The L2PcInstance of the player trying to register
	 */
	public void registerDefender(L2PcInstance player)
	{
		registerDefender(player, false);
	}

	public void registerDefender(L2PcInstance player, boolean force)
	{
		if(!force)
		{
			if (getCastle().getOwnerId() <= 0)
			{
				player.sendPacket(SystemMessageId.DEFENDER_SIDE_FULL);
				return;
			}
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
			{
				//strife restricts defenders to only clan's alliance
				int allyId = 0;
				if (getCastle().getOwnerId() != 0)
					allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
				if (allyId != 0 && player.getClan().getAllyId() != allyId)
				{
					player.sendPacket(SystemMessageId.DEFENDER_SIDE_FULL);
					return; 
				}
			}
		}

		if ((force && player.getClan() != null) || checkIfCanRegister(player, 2))
			saveSiegeClan(player.getClan(), 2, false); // Save to database
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * @param clanId The int of player's clan id
	 */
	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
			return;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?");
			statement.setInt(1, getCastle().getCastleId());
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			SiegeManager.getInstance().removeClan(getCastle().getCastleId(), clanId);
			loadSiegeClan(false);
			
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

	/**
	 * Remove clan from siege<BR><BR>
	 * @param player The L2PcInstance of player/clan being removed
	 */
	public void removeSiegeClan(L2Clan clan)
	{
		if (clan == null || clan.getHasCastle() == getCastle().getCastleId() || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getCastleId()))
			return;
		removeSiegeClan(clan.getClanId());
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * @param player The L2PcInstance of player/clan being removed
	 */
	public void removeSiegeClan(L2PcInstance player)
	{
		removeSiegeClan(player.getClan());
	}

	/**
	 * Start the auto tasks<BR><BR>
	 */
	public void startAutoTask()
	{
		correctSiegeDateTime();

		_log.info("Siege of " + getCastle().getName() + ": " + getCastle().getSiegeDate().getTime());

		loadSiegeClan(false);

		// Schedule siege auto start
		_startSiegeTask.schedule(1000);
	}

	/**
	 * Teleport players
	 */
	public void teleportPlayer(TeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		FastList<L2PcInstance> players;
		switch (teleportWho)
		{
			case Owner:
				players = getOwnersInZone();
				break;
			case Attacker:
				players = getAttackersInZone();
				break;
			case DefenderNotOwner:
				players = getDefendersButNotOwnersInZone();
				break;
			case Spectator:
				players = getSpectatorsInZone();
				break;
			default:
				players = getPlayersInZone();
		}

		for (L2PcInstance player : players)
		{
			if (player.isGM() || player.isInJail())
				continue;
			L2SiegeStatus.getInstance().addStatus(player.getClanId(), player.getObjectId());
			player.teleToLocation(teleportWhere);
		}
	}

	/**
	 * Add clan as attacker<BR><BR>
	 * @param clanId The int of clan's id
	 */
	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
	}

	/**
	 * Add clan as defender<BR><BR>
	 * @param clanId The int of clan's id
	 */
	private void addDefender(int clanId)
	{
		getDefenderClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list
	}

	/**
	 * <p>Add clan as defender with the specified type</p>
	 * @param clanId The int of clan's id
	 * @param type the type of the clan
	 */
	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new L2SiegeClan(clanId, type));
	}

	/**
	 * Add clan as defender waiting approval<BR><BR>
	 * @param clanId The int of clan's id
	 */
	private void addDefenderWaiting(int clanId)
	{
		getDefenderWaitingClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list
	}

	/**
	 * Return true if the player can register.<BR><BR>
	 * @param player The L2PcInstance of the player trying to register
	 */
	private boolean checkIfCanRegister(L2PcInstance player, int typeId)
	{
		L2Clan clan = player.getClan();
		if (clan == null || clan.getLevel() < Config.SIEGE_CLAN_MIN_LEVEL)
		{
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_LOW_CLAN_LEVEL),Config.SIEGE_CLAN_MIN_LEVEL));
			return false;
		}
		else if (clan.getMembersCount() < Config.SIEGE_CLAN_MIN_MEMBERCOUNT)
		{
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_NOT_ENOUGH_CLAN_MEMBERS),Config.SIEGE_CLAN_MIN_MEMBERCOUNT));
			return false;
		}
		else if (getIsRegistrationOver())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DEADLINE_FOR_SIEGE_S1_PASSED);
			sm.addString(getCastle().getName());
			player.sendPacket(sm);
			return false;
		}
		else if (getIsInProgress())
		{
			player.sendPacket(SystemMessageId.NOT_SIEGE_REGISTRATION_TIME2);
			return false;
		}
		else if (clan.getHasCastle() > 0)
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_CANNOT_PARTICIPATE_OTHER_SIEGE);
			return false;
		}
		else if (clan.getClanId() == getCastle().getOwnerId())
		{
			player.sendPacket(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING);
			return false;
		}
		else if (checkIfAlreadyRegisteredForSameDay(player.getClan()))
		{
			player.sendPacket(SystemMessageId.APPLICATION_DENIED_BECAUSE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
			return false;
		}
		else
		{
			for (int i = 0; i < 10; i++)
			{
				if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), i))
				{
					player.sendPacket(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE);
					return false;
				}
			}
		}

		if (typeId == 0 || typeId == 2 || typeId == -1)
		{
			if (getDefenderClans().size() + getDefenderWaitingClans().size() >= Config.SIEGE_MAX_DEFENDER)
			{
				player.sendPacket(SystemMessageId.DEFENDER_SIDE_FULL);
				return false;
			}
		}
		if (typeId == 1)
		{
			if (getAttackerClans().size() >= Config.SIEGE_MAX_ATTACKER)
			{
				player.sendPacket(SystemMessageId.ATTACKER_SIDE_FULL);
				return false;
			}
		}

		return true;
	}

	/**
	 * Return true if the clan has already registered to a siege for the same day.<BR><BR>
	 * @param clan The L2Clan of the player trying to register
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(L2Clan clan)
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege == this)
				continue;
			if (siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == this.getSiegeDate().get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
					return true;
				if (siege.checkIsDefender(clan))
					return true;
				if (siege.checkIsDefenderWaiting(clan))
					return true;
			}
		}
		return false;
	}

	/**
	 * Коректировка времени осад
	 **/
	public void correctSiegeDateTime()
	{
		boolean corrected = false;

		if (getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis())
		{
			corrected = true;
			setNextSiegeDate();
		}

		if (!SevenSigns.getInstance().isDateInSealValidPeriod(getCastle().getSiegeDate()))
		{
			corrected = true;
			setNextSiegeDate();
		}

		if (corrected)
			saveSiegeDate();
	}

	/**
	 * Загружаем зарегестрированы на осаду кланы
	 */
	private void loadSiegeClan(boolean clearStatus)
	{
		Connection con = null;
		try
		{
			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();

			// Add castle owner as defender (add owner first so that they are on the top of the defender list)
			if (getCastle().getOwnerId() > 0)
				addDefender(getCastle().getOwnerId(), SiegeClanType.OWNER);

			PreparedStatement statement = null;
			ResultSet rs = null;

			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?");
			statement.setInt(1, getCastle().getCastleId());
			rs = statement.executeQuery();

			int typeId;
			int clanId;
			while (rs.next())
			{
				typeId = rs.getInt("type");
				clanId =rs.getInt("clan_id");
				if (typeId == 0)
					addDefender(clanId);
				else if (typeId == 1)
					addAttacker(clanId);
				else if (typeId == 2)
					addDefenderWaiting(clanId);
				if (clearStatus)
					L2SiegeStatus.getInstance().clearClanStatus(clanId);
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

	/** Remove all control tower spawned. */
	private void removeControlTower()
	{
		if (_controlTowers != null)
		{
			// Remove all instance of control tower for this castle
			for (L2ControlTowerInstance ct : _controlTowers)
			{
				if (ct != null)
					ct.decayMe();
			}

			_controlTowers = null;
		}
	}

	/** Remove all flags. */
	private void removeFlags()
	{
		for (L2SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
				sc.removeFlags();
		}
		for (L2SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
				sc.removeFlags();
		}
	}

	/** Remove flags from defenders. */
	private void removeDefenderFlags()
	{
		for (L2SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
				sc.removeFlags();
		}
	}

	private void saveCastleSiege()
	{
		setNextSiegeDate();
		getTimeRegistrationOverDate().setTimeInMillis(Calendar.getInstance().getTimeInMillis());
		getTimeRegistrationOverDate().add(Calendar.DAY_OF_MONTH, 1);
		getCastle().setIsTimeRegistrationOver(false);
		saveSiegeDate();
		startAutoTask();
	}

	public void saveSiegeDate()
	{
		if (_startSiegeTask.isScheduled())
			_startSiegeTask.schedule(1000);

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET siegeDate = ?, regTimeEnd = ?, regTimeOver = ?, AutoTime = ?  WHERE id = ?");
			statement.setLong(1, getSiegeDate().getTimeInMillis());
			statement.setLong(2, getTimeRegistrationOverDate().getTimeInMillis());
			statement.setString(3, String.valueOf(getIsTimeRegistrationOver()));
			statement.setString(4, "false");
			statement.setInt(5, getCastle().getCastleId());
			statement.execute();

			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: saveSiegeDate(): " + e.getMessage(), e);
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

	/**
	 * Save registration to database.<BR><BR>
	 * @param clan The L2Clan of player
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 */
	private void saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration)
	{
		if (clan.getHasCastle() > 0)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			if (!isUpdateRegistration)
			{
				statement = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) VALUES (?,?,?,0)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, getCastle().getCastleId());
				statement.setInt(3, typeId);
				statement.execute();
				statement.close();
				SiegeManager.getInstance().registerClan(getCastle().getCastleId(), clan);
			}
			else
			{
				statement = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?");
				statement.setInt(1, typeId);
				statement.setInt(2, getCastle().getCastleId());
				statement.setInt(3, clan.getClanId());
				statement.execute();
				statement.close();
			}

			if (typeId == 0 || typeId == -1)
			{
				addDefender(clan.getClanId());
				announceToPlayer(clan.getName()+" зарегистрирован на защиту замка: "+getCastle().getName(), false);
			}
			else if (typeId == 1)
			{
				addAttacker(clan.getClanId());
				announceToPlayer(clan.getName()+" зарегистрирован на атаку замка: "+getCastle().getName(), false);
			}
			else if (typeId == 2)
			{
				addDefenderWaiting(clan.getClanId());
				announceToPlayer(clan.getName()+" обращается с просьбой защитить замок: "+getCastle().getName(), false);
			}
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

	/**
	 * Устанавливаем дату следующей осады
	 * Если нужно коректируем время осад
	 **/
	private void setNextSiegeDate()
	{
		while (getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis())
		{
			if (getCastle().getSiegeDate().get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && getCastle().getSiegeDate().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
			{
				switch (getCastle().getCastleId())
				{
					case 1:
					case 2:
					case 5:
					case 8:
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
						break;
					case 3:
					case 4:
					case 6:
					case 7:
					case 9:
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						break;
					default:
						_log.info("Could't set siege day for castle ID: " + getCastle().getCastleId());
						break;
				}
			}

			/**
			 * Переносим время осад на неделю позже
			 * Если необходимо, коректируем дату осад
			 **/
			getCastle().getSiegeDate().add(Calendar.DAY_OF_MONTH, 7);
			if (!getCastle().isAutoTime())
			{
				switch (getCastle().getCastleId())
				{
					case 1:
					case 2:
					case 5:
					case 8:
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
						getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, 20);
						break;
					case 3:
					case 4:
					case 6:
					case 7:
					case 9:
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, 16);
						break;
					default:
						_log.info("Could't set siege day for castle ID: " + getCastle().getCastleId());
						break;
				}
				getCastle().getSiegeDate().set(Calendar.MINUTE, 00);
			}
		}

		if (!SevenSigns.getInstance().isDateInSealValidPeriod(getCastle().getSiegeDate()) && Config.CORECT_SIEGE_DATE_BY_7S)
			getCastle().getSiegeDate().add(Calendar.DAY_OF_MONTH, 7);

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_ANNOUNCED_SIEGE_TIME);
		sm.addString(getCastle().getName());
		Announcements.getInstance().announceToAll(sm);

		_isRegistrationOver = false; // Allow registration for next siege
	}

	/** Spawn control tower. */
	private void spawnControlTower(int Id)
	{
		//Set control tower array size if one does not exist
		if (_controlTowers == null)
			_controlTowers = new FastList<L2ControlTowerInstance>();

		for (SiegeSpawn _sp : SiegeManager.getInstance().getControlTowerSpawnList(Id))
		{
			L2ControlTowerInstance ct;

			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_sp.getNpcId());

			template.setBaseHpMax(_sp.getHp());

			ct = new L2ControlTowerInstance(IdFactory.getInstance().getNextId(), template);
			ct.getStatus().setCurrentHpMp(_sp.getHp(), ct.getMaxMp());
			ct.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 20);

			_controlTowerCount++;
			_controlTowerMaxCount++;
			_controlTowers.add(ct);
		}
	}

	/**
	 * Spawn siege guard.<BR><BR>
	 */
	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();

		// Register guard to the closest Control Tower
		// When CT dies, so do all the guards that it controls
		if (!getSiegeGuardManager().getSiegeGuardSpawn().isEmpty() && !_controlTowers.isEmpty())
		{
			L2ControlTowerInstance closestCt;
			double distance, x, y, z;
			double distanceClosest = 0;
			for (L2Spawn spawn : getSiegeGuardManager().getSiegeGuardSpawn())
			{
				if (spawn == null)
					continue;
				closestCt = null;
				distanceClosest = 0;
				for (L2ControlTowerInstance ct : _controlTowers)
				{
					if (ct == null)
						continue;
					x = (spawn.getLocx() - ct.getX());
					y = (spawn.getLocy() - ct.getY());
					z = (spawn.getLocz() - ct.getZ());

					distance = (x * x) + (y * y) + (z * z);

					if (closestCt == null || distance < distanceClosest)
					{
						closestCt = ct;
						distanceClosest = distance;
					}
				}

				if (closestCt != null)
					closestCt.registerGuard(spawn);
			}
		}
	}

	public final L2SiegeClan getAttackerClan(L2Clan clan)
	{
		if (clan == null)
			return null;
		return getAttackerClan(clan.getClanId());
	}

	public final L2SiegeClan getAttackerClan(int clanId)
	{
		for (L2SiegeClan sc : getAttackerClans())
			if (sc != null && sc.getClanId() == clanId)
				return sc;
		return null;
	}

	public final FastList<L2SiegeClan> getAttackerClans()
	{
		if (_isNormalSide)
			return _attackerClans;
		return _defenderClans;
	}

	public final int getAttackerRespawnDelay()
	{
		return (Config.SIEGE_RESPAWN_DELAY_ATTACKER);
	}

	public final Castle getCastle()
	{
		return _castle;
	}

	public final L2SiegeClan getDefenderClan(L2Clan clan)
	{
		if (clan == null)
			return null;
		return getDefenderClan(clan.getClanId());
	}

	public final L2SiegeClan getDefenderClan(int clanId)
	{
		for (L2SiegeClan sc : getDefenderClans())
		{
			if (sc != null && sc.getClanId() == clanId)
				return sc;
		}
		return null;
	}

	public final FastList<L2SiegeClan> getDefenderClans()
	{
		if (_isNormalSide)
			return _defenderClans;
		return _attackerClans;
	}

	public final L2SiegeClan getDefenderWaitingClan(L2Clan clan)
	{
		if (clan == null)
			return null;
		return getDefenderWaitingClan(clan.getClanId());
	}

	public final L2SiegeClan getDefenderWaitingClan(int clanId)
	{
		for (L2SiegeClan sc : getDefenderWaitingClans())
			if (sc != null && sc.getClanId() == clanId)
				return sc;
		return null;
	}

	public final FastList<L2SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}

	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}

	public final boolean getIsRegistrationOver()
	{
		return _isRegistrationOver;
	}

	public final boolean getIsTimeRegistrationOver()
	{
		return getCastle().getIsTimeRegistrationOver();
	}

	public final Calendar getSiegeDate()
	{
		return getCastle().getSiegeDate();
	}

	public final Calendar getTimeRegistrationOverDate()
	{
		return getCastle().getTimeRegistrationOverDate();
	}

	public void endTimeRegistration(boolean automatic)
	{
		getCastle().setIsTimeRegistrationOver(true);
		if (!automatic) {
			saveSiegeDate();
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.S1_ANNOUNCED_SIEGE_TIME).addString(getCastle().getName()));
		}
	}

	public Set<L2NpcInstance> getFlag(L2Clan clan)
	{
		if (clan != null)
		{
			L2SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
				return sc.getFlag();
		}
		return null;
	}

	public L2NpcInstance getClosestFlag(L2Object obj)
	{
		if ((obj != null) && (obj instanceof L2PcInstance))
		{
			if (((L2PcInstance) obj).getClan() != null)
			{
				L2SiegeClan sc = getAttackerClan(((L2PcInstance) obj).getClan());
				if (sc != null)
					return sc.getClosestFlag(obj);
			}
		}
		return null;
	}

	public final SiegeGuardManager getSiegeGuardManager()
	{
		if (_siegeGuardManager == null)
			_siegeGuardManager = new SiegeGuardManager(getCastle().getName(),getCastle().getCastleId(),getCastle().getOwnerId());

		return _siegeGuardManager;
	}

	public final L2SiegeZone getZone()
	{
		return getCastle().getBattlefield();
	}

	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}
}