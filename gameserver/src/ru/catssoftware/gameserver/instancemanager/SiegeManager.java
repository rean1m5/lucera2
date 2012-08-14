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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2ArtefactInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;


public class SiegeManager
{
	protected static Logger		_log	= Logger.getLogger(SiegeManager.class.getName());

	private static SiegeManager	_instance;

	public static final SiegeManager getInstance()
	{
		if (_instance == null) {
			_instance = new SiegeManager();
		}
		return _instance;
	}

	private FastMap<Integer, FastList<SiegeSpawn>>	_artefactSpawnList;

	private FastMap<Integer, FastList<SiegeSpawn>>	_controlTowerSpawnList;

	private FastMap<Integer, List<Integer>> _registredClans = new FastMap<Integer, List<Integer>>();
	public void load()
	{
		loadTowerArtefacts();
		_registredClans.clear();
		Connection con = null;
		try {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("SELECT castle_id,clan_id FROM siege_clans ORDER BY castle_id");
			ResultSet rs = stm.executeQuery();
			while(rs.next()) {
				List<Integer> lst = _registredClans.get(rs.getInt("castle_id"));
				if(lst==null) {
					lst = new FastList<Integer>();
					_registredClans.put(rs.getInt("castle_id"),lst);
				}
				lst.add(rs.getInt("clan_id"));
 			}
			rs.close();
			stm.close();
		} catch(SQLException e) {
			
		}
		finally {
			if(con!=null) try {
				con.close();
			} catch(Exception e) {} 
		}
		_log.info("SiegeManager: Loaded "+_registredClans.size()+" registred siege(s)");
	}

	public final void addSiegeSkills(L2PcInstance character)
	{
		for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble()))
			character.addSkill(sk, false);
	}

	/** Return true if object is inside zone */
	public final boolean checkIfInZone(L2Object obj)
	{
		return (getSiege(obj) != null);
	}

	/** Return true if object is inside zone */
	public final boolean checkIfInZone(int x, int y, int z)
	{
		return (getSiege(x, y, z) != null);
	}

	/**
	 * Return true if character can place a flag<BR><BR>
	 *
	 * @param player
	 *            The L2PcInstance of the character placing the flag
	 * @param isCheckOnly
	 *            if false, it will send a notification to the player telling
	 *            him why it failed
	 */
	public static boolean checkIfOkToPlaceFlag(L2PcInstance player, boolean isCheckOnly)
	{
		// get siege battleground
		L2Clan clan = player.getClan();
		Siege siege = SiegeManager.getInstance().getSiege(player);
		Castle castle = (siege == null) ? null : siege.getCastle();

		SystemMessageId sm = null;

		if (siege == null || !siege.getIsInProgress())
			sm = SystemMessageId.ONLY_DURING_SIEGE;
		else if (clan == null || clan.getLeaderId() != player.getObjectId() ||
				siege.getAttackerClan(clan) == null)
			sm = SystemMessageId.CANNOT_USE_ON_YOURSELF;
		else if (castle == null || !castle.checkIfInZoneHeadQuarters(player))
			sm = SystemMessageId.ONLY_DURING_SIEGE;


		else if (castle.getSiege().getAttackerClan(clan).getNumFlags() >= Config.SIEGE_FLAG_MAX_COUNT)
			sm = SystemMessageId.NOT_ANOTHER_HEADQUARTERS;
		else
			return true;

		if (!isCheckOnly)
			player.sendPacket(sm);
		return false;
	}

	/**
	 * Return true if character can summon<BR><BR>
	 *
	 * @param player
	 *            The L2PcInstance of the character can summon
	 */
	public final boolean checkIfOkToSummon(L2PcInstance player, boolean isCheckOnly)
	{
		// get siege battleground
		Siege siege = SiegeManager.getInstance().getSiege(player);

		SystemMessageId sm = null;

		if (siege == null)
			sm = SystemMessageId.YOU_ARE_NOT_IN_SIEGE;
		else if (!siege.getIsInProgress())
			sm = SystemMessageId.ONLY_DURING_SIEGE;
		else if (player.getClanId() != 0 && siege.getAttackerClan(player.getClanId()) == null)
			sm = SystemMessageId.CANNOT_USE_ON_YOURSELF;
		else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN
				&& siege.getCastle().getOwnerId() > 0)
			sm = SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING;
		else
			return true;

		if (!isCheckOnly)
			player.sendPacket(sm);
		return false;
	}

	/**
	 * Return true if character can use Strider Siege Assault skill <BR><BR>
	 *
	 * @param player
	 *            The L2PcInstance of the character placing the flag
	 * @param isCheckOnly
	 *            if false, it will send a notification to the player telling
	 *            him why it failed
	 */
	public static boolean checkIfOkToUseStriderSiegeAssault(L2PcInstance player, boolean isCheckOnly)
	{
		// get siege battleground
		Siege siege = SiegeManager.getInstance().getSiege(player);

		SystemMessageId sm = null;

		if (siege == null)
			sm = SystemMessageId.YOU_ARE_NOT_IN_SIEGE;
		else if (!siege.getIsInProgress())
			sm = SystemMessageId.ONLY_DURING_SIEGE;
		else if (!(player.getTarget() instanceof L2DoorInstance))
			sm = SystemMessageId.TARGET_IS_INCORRECT;
		else if (!player.isRidingStrider() && !player.isRidingRedStrider())
			sm = SystemMessageId.CANNOT_USE_ON_YOURSELF;
		else
			return true;

		if (!isCheckOnly)
			player.sendPacket(sm);
		return false;
	}

	public boolean checkIfOkToCastSealOfRule(L2Character activeChar, Castle castle, boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;

		SystemMessageId sm = null;
		L2PcInstance player = (L2PcInstance) activeChar;

		if (castle == null || castle.getCastleId() <= 0 || castle.getSiege().getAttackerClan(player.getClan()) == null)
			sm = SystemMessageId.YOU_ARE_NOT_IN_SIEGE;
		else if (player.getTarget() == null && !(player.getTarget() instanceof L2ArtefactInstance))
			sm = SystemMessageId.TARGET_IS_INCORRECT;
		else if (!castle.getSiege().getIsInProgress())
			sm = SystemMessageId.ONLY_DURING_SIEGE;
		else if (!player.isInsideZone(L2Zone.FLAG_ARTEFACTCAST))
			sm = SystemMessageId.TARGET_TOO_FAR;
		else
		{
			if (!isCheckOnly)
				castle.getSiege().announceToOpponent(new SystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING), player.getClan());
			return true;
		}
		if (!isCheckOnly)
			player.sendPacket(sm);

		return false;
	}

	/**
	 * Return true if the clan is registered or owner of a castle<BR>
	 * <BR>
	 *
	 * @param clan
	 *            The L2Clan of the player
	 */
	
	
	public final boolean checkIsRegistered(L2Clan clan, int castleid)
	{
		if (clan == null)
			return false;

		if (clan.getHasCastle() > 0)
			return true;
		
		List<Integer> lst = _registredClans.get(castleid);
		if(lst==null)
			return false;
		return lst.contains(clan.getClanId());
/*		Connection con = null;
		boolean register = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans WHERE clan_id=? AND castle_id=?");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, castleid);
			ResultSet rs = statement.executeQuery();

			if (rs.next())
				register = true;

			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: checkIsRegistered(): " + e.getMessage(), e);
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

		return register; */
	}

	public void registerClan(int fortid, L2Clan clan) {
		List<Integer> lst = _registredClans.get(fortid);
		if(lst==null) {
			lst = new FastList<Integer>();
			_registredClans.put(fortid,lst);
		}
		if(!lst.contains(clan.getClanId()))
			lst.add(clan.getClanId());
	}
	public void removeClan(int fortid, int clan) {
		List<Integer> lst = _registredClans.get(fortid);
		if(lst==null)
			return;
		if(lst.contains(clan)) try {
			lst.remove((Object)clan);
		} catch(Exception e) {
			
		}
	}

	public final void removeSiegeSkills(L2PcInstance character)
	{
		for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble()))
			character.removeSkill(sk);
	}

	private final void loadTowerArtefacts()
	{
		try
		{
			Properties siegeSettings = new L2Properties(Config.SIEGE_CONFIGURATION_FILE).setLog(false);

			// Siege spawns settings
			_controlTowerSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();
			_artefactSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();

			for (Castle castle : CastleManager.getInstance().getCastles().values())
			{
				FastList<SiegeSpawn> _controlTowersSpawns = new FastList<SiegeSpawn>();

				for (int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "ControlTower" + Integer.toString(i), "");

					if (_spawnParams.length() == 0)
						break;

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());
						int hp = Integer.parseInt(st.nextToken());

						_controlTowersSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, 0, npc_id, hp));
					}
					catch (Exception e)
					{
						_log.error("Error while loading control tower(s) for " + castle.getName() + " castle.", e);
					}
				}

				FastList<SiegeSpawn> _artefactSpawns = new FastList<SiegeSpawn>();

				for (int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "Artefact" + Integer.toString(i), "");

					if (_spawnParams.length() == 0)
						break;

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int heading = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());

						_artefactSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, heading, npc_id));
					}
					catch (Exception e)
					{
						_log.error("Error while loading artefact(s) for " + castle.getName() + " castle.", e);
					}
				}

				_controlTowerSpawnList.put(castle.getCastleId(), _controlTowersSpawns);
				_artefactSpawnList.put(castle.getCastleId(), _artefactSpawns);

//				_log.info("SiegeManager: loaded controltowers[" + Integer.toString(_controlTowersSpawns.size()) + "] artefacts["
//						+ Integer.toString(_artefactSpawns.size()) + "] castle[" + castle.getName() + "]");
			}
		}
		catch (Exception e)
		{
			_log.error("Error while loading siege data.", e);
		}
	}

	public final void reload()
	{
		_artefactSpawnList.clear();
		_controlTowerSpawnList.clear();
		Config.loadSiegeConfig();
		loadTowerArtefacts();
	}

	public final FastList<SiegeSpawn> getArtefactSpawnList(int _castleId)
	{
		if (_artefactSpawnList.containsKey(_castleId))
			return _artefactSpawnList.get(_castleId);
		return null;
	}

	public final FastList<SiegeSpawn> getControlTowerSpawnList(int _castleId)
	{
		if (_controlTowerSpawnList.containsKey(_castleId))
			return _controlTowerSpawnList.get(_castleId);
		return null;
	}

	public final Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	/** * get active siege for clan ** */
	public final Siege getSiege(L2Clan clan)
	{
		if (clan == null)
			return null;
		for (Castle castle : CastleManager.getInstance().getCastles().values())
		{
			Siege siege = castle.getSiege();
			if (siege.getIsInProgress() && (siege.checkIsAttacker(clan) || siege.checkIsDefender(clan)))
				return siege;
		}
		return null;
	}

	public final Siege getSiege(int x, int y, int z)
	{
		for (Castle castle : CastleManager.getInstance().getCastles().values())
			if (castle.getSiege().checkIfInZone(x, y, z))
				return castle.getSiege();
		return null;
	}

	public final List<Siege> getSieges()
	{
		FastList<Siege> sieges = new FastList<Siege>();
		for (Castle castle : CastleManager.getInstance().getCastles().values())
			sieges.add(castle.getSiege());
		return sieges;
	}

	public class SiegeSpawn
	{
		Location	_location;

		private int	_npcId;

		private int	_heading;

		private int	_castleId;

		private int	_hp;

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
		}

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id, int hp)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
			_hp = hp;
		}

		public int getCastleId()
		{
			return _castleId;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getHeading()
		{
			return _heading;
		}

		public int getHp()
		{
			return _hp;
		}

		public Location getLocation()
		{
			return _location;
		}
	}
}
