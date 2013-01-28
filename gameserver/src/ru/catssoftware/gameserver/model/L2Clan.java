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

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CrownManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.ClanWarehouse;
import ru.catssoftware.gameserver.model.listeners.ClanListenerList;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.skills.TimeStamp;
import ru.catssoftware.util.LinkedBunch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.7 $ $Date: 2005/04/06 16:13:41 $
 */
public class L2Clan
{
	private static final Logger				_log						= Logger.getLogger(L2Clan.class.getName());
	private String							_name;
	private int								_clanId;
	private L2ClanMember					_leader;
	private Map<Integer, L2ClanMember>		_members					= new FastMap<Integer, L2ClanMember>();

	private String							_allyName;
	private int								_allyId;
	private int								_level;
	private int								_hasCastle;
	private int								_hasHideout;
	private int								_hasFort;
	private boolean							_hasCrest;
	private int								_hiredGuards;
	private int								_crestId;
	private int								_crestLargeId;
	private int								_allyCrestId;
	private int								_auctionBiddedAt			= 0;

	private long							_allyPenaltyExpiryTime;
	private int								_allyPenaltyType;
	private long							_charPenaltyExpiryTime;
	private long							_dissolvingExpiryTime;
	// Ally Penalty Types
	/** Clan leaved ally */
	public static final int					PENALTY_TYPE_CLAN_LEAVED	= 1;
	/** Clan was dismissed from ally */
	public static final int					PENALTY_TYPE_CLAN_DISMISSED	= 2;
	/** Leader clan dismiss clan from ally */
	public static final int					PENALTY_TYPE_DISMISS_CLAN	= 3;
	/** Leader clan dissolve ally */
	public static final int					PENALTY_TYPE_DISSOLVE_ALLY	= 4;

	private ClanWarehouse					_warehouse					= new ClanWarehouse(this);
	private List<Integer>					_atWarWith					= new FastList<Integer>();
	private List<Integer>					_atWarAttackers				= new FastList<Integer>();

	private boolean							_hasCrestLarge;


	private List<L2Skill>					_skillList					= new FastList<L2Skill>();

	//  Clan Privileges
	public static final int					CP_NOTHING					= 0;
	public static final int					CP_CL_JOIN_CLAN				= 2;
	public static final int					CP_CL_GIVE_TITLE			= 4;
	public static final int					CP_CL_VIEW_WAREHOUSE		= 8;
	public static final int					CP_CL_MANAGE_RANKS			= 16;
	public static final int					CP_CL_PLEDGE_WAR			= 32;
	public static final int					CP_CL_DISMISS				= 64;
	public static final int					CP_CL_REGISTER_CREST		= 128;
	public static final int					CP_CL_MASTER_RIGHTS			= 256;
	public static final int					CP_CL_MANAGE_LEVELS			= 512;
	public static final int					CP_CH_OPEN_DOOR				= 1024;
	public static final int					CP_CH_OTHER_RIGHTS			= 2048;
	public static final int					CP_CH_AUCTION				= 4096;
	public static final int					CP_CH_DISMISS				= 8192;
	public static final int					CP_CH_SET_FUNCTIONS			= 16384;
	public static final int					CP_CS_OPEN_DOOR				= 32768;
	public static final int					CP_CS_MANOR_ADMIN			= 65536;
	public static final int					CP_CS_MANAGE_SIEGE			= 131072;
	public static final int					CP_CS_USE_FUNCTIONS			= 262144;
	public static final int					CP_CS_DISMISS				= 524288;
	public static final int					CP_CS_TAXES					= 1048576;
	public static final int					CP_CS_MERCENARIES			= 2097152;
	public static final int					CP_CS_SET_FUNCTIONS			= 4194304;
	public static final int					CP_ALL						= 8388606;

	// Дополнительно указываем тут ревизию.
	public static final int					CP_CHECK					= 165;

	// Sub-unit types
	public static final int					SUBUNIT_ACADEMY				= -1;
	public static final int					SUBUNIT_ROYAL1				= 100;
	public static final int					SUBUNIT_ROYAL2				= 200;
	public static final int					SUBUNIT_KNIGHT1				= 1001;
	public static final int					SUBUNIT_KNIGHT2				= 1002;
	public static final int					SUBUNIT_KNIGHT3				= 2001;
	public static final int					SUBUNIT_KNIGHT4				= 2002;

	/** FastMap(Integer, L2Skill) containing all skills of the L2Clan */
	protected final Map<Integer, L2Skill>	_skills						= new FastMap<Integer, L2Skill>();
	protected final Map<Integer, RankPrivs>	_privs						= new FastMap<Integer, RankPrivs>();
	protected final Map<Integer, SubPledge>	_subPledges					= new FastMap<Integer, SubPledge>();

	private int								_reputationScore			= 0;
	private int								_rank						= 0;

	private String							_notice = "";

	private ClanListenerList clanListnerList = new ClanListenerList(this);

	@SuppressWarnings("unused")
	private boolean							_noticeEnabled				= true;

	/**
	 * Called if a clan is referenced only by id.
	 * In this case all other data needs to be fetched from db
	 *
	 * @param clanId A valid clan Id to create and restore
	 */
	public L2Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		getWarehouse().restore();
		//L2EMU_EDIT
		if (getNotice() == null || getNotice().equals(""))
		//L2EMU_EDIT
			insertNotice(); // add this line so it inserts the new clan's (blank) notice into the DB
	}

	public static int checkClass()
	{
		return CP_CHECK;
	}

	// at the end of the file, before the last '}' that ends the L2Clan class, add the following codes:
	public void insertNotice()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_notices (clanID, notice, enabled) values (?,?,?)");
			statement.setInt(1, this.getClanId());
			statement.setString(2, "Change me");
			statement.setString(3, "false");
			statement.execute();
			statement.close();
			con.close();

		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while creating clan notice for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
	}

	/**
	 * @return Returns the clan notice.
	 */
	public String getNotice()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT notice FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				_notice = rset.getString("notice");
			}

			rset.close();
			statement.close();
			con.close();

		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while getting notice from DB for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}

		return _notice;
	}

	public String getNoticeForBBS()
	{
		String notice = "";
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT notice FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				notice = rset.getString("notice");
			}

			rset.close();
			statement.close();
			con.close();

		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while getting notice from DB for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		return notice.replaceAll("<br>", "\n");
	}

	/**
	 * @param notice The new clan notice.
	 */
	public void setNotice(String notice)
	{
		notice = notice.replaceAll("\n", "<br>");

		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE clan_notices SET notice=? WHERE clanID=?");

			statement.setString(1, notice);
			statement.setInt(2, this.getClanId());
			statement.execute();
			statement.close();
			con.close();

			_notice = notice;
		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while saving notice for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
	}

	/**
	 * @return Returns the noticeEnabled.
	 */
	public boolean isNoticeEnabled()
	{
		String result = "";
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT enabled FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				result = rset.getString("enabled");
			}

			rset.close();
			statement.close();
			con.close();

		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while reading _noticeEnabled for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		if (result.isEmpty())
		{
			insertNotice();
			return false;
		}
		else return result.compareToIgnoreCase("true") == 0;
	}

	/**
	 * @param noticeEnabled The noticeEnabled to set.
	 */
	public void setNoticeEnabled(boolean noticeEnabled)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE clan_notices SET enabled=? WHERE clanID=?");
			if (noticeEnabled)
				statement.setString(1, "true");
			else
				statement.setString(1, "false");
			statement.setInt(2, this.getClanId());
			statement.execute();
			statement.close();
			con.close();

		}
		catch (Exception e)
		{
			System.out.println("BBS: Error while updating notice status for clan " + this.getClanId() + "");
			if (e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}

		_noticeEnabled = noticeEnabled;

	}

	/**
	 * Called only if a new clan is created
	 *
	 * @param clanId  A valid clan Id to create
	 * @param clanName  A valid clan name
	 */
	public L2Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}

	/**
	 * @return Returns the clanId.
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * @param clanId The clanId to set.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}

	/**
	 * @return Returns the leaderId.
	 */
	public int getLeaderId()
	{
		return (_leader != null ? _leader.getObjectId() : 0);
	}

	/**
	 * @return L2ClanMember of clan leader.
	 */
	public L2ClanMember getLeader()
	{
		return _leader;
	}

	/**
	 * @param leaderId The leaderId to set.
	 */
	public void setLeader(L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}

	public void setNewLeader(L2ClanMember member)
	{
		if (!getLeader().isOnline())
			return;

		if (member == null)
			return;

		if (!member.isOnline())
			return;

		L2PcInstance exLeader = getLeader().getPlayerInstance();
		SiegeManager.getInstance().removeSiegeSkills(exLeader);
		exLeader.setClan(this);
		exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
		exLeader.broadcastUserInfo(true);

		setLeader(member);
		updateClanInDB();

		exLeader.setPledgeClass(L2ClanMember.getCurrentPledgeClass(exLeader));
		exLeader.broadcastUserInfo(true);
		exLeader.checkItemRestriction();
		L2PcInstance newLeader = member.getPlayerInstance();
		newLeader.setClan(this);
		newLeader.setPledgeClass(L2ClanMember.getCurrentPledgeClass(newLeader));
		newLeader.setClanPrivileges(L2Clan.CP_ALL);
		if (getLevel() >= Config.SIEGE_CLAN_MIN_LEVEL)
		{
			SiegeManager.getInstance().addSiegeSkills(newLeader);

			// Transferring siege skills TimeStamps from old leader to new leader to prevent unlimited headquarters
			if (!exLeader.getDisableSkills().isEmpty())
			{
				for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(newLeader.isNoble()))
				{
					if (exLeader.isSkillDisabled(sk))
					{
						TimeStamp ts = exLeader.getDisableSkill(sk);
						newLeader.disableSkill(sk, ts.getReuse());
					}
				}
				newLeader.sendPacket(new SkillCoolTime(newLeader));
			}
		}
		newLeader.broadcastUserInfo(true);

		broadcastClanStatus();

		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1);
		sm.addString(newLeader.getName());
		broadcastToOnlineMembers(sm);
		sm = null;

		CrownManager.getInstance().checkCrowns(exLeader);
		CrownManager.getInstance().checkCrowns(newLeader);
	}

	/**
	 * @return Returns the leaderName.
	 */
	public String getLeaderName()
	{
		return _leader == null ? "None" : _leader.getName();
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name)
	{
		_name = name;
	}

	private void addClanMember(L2ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}

	public void addClanMember(L2PcInstance player)
	{
		// Using a different constructor, to make it easier to read
		L2ClanMember member = new L2ClanMember(this, player);
		// store in memory
		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(L2ClanMember.getCurrentPledgeClass(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
	}

	public void updateClanMember(L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(player);
		if (player.isClanLeader())
			setLeader(member);

		addClanMember(member);
	}

	public L2ClanMember getClanMember(String name)
	{
		for (L2ClanMember temp : _members.values())
		{
			//L2EMU_EDIT Visor123 optimize
			if (temp.getName().equalsIgnoreCase(name))
			//L2EMU_EDIT
				return temp;
		}
		return null;
	}

	public L2ClanMember getClanMember(int objectID)
	{
		return _members.get(objectID);
	}

	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		L2ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			_log.warn("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}
		int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			// Sub-unit leader withdraws, position becomes vacant and leader
			// should appoint new via NPC
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}

		if (exMember.getApprentice() != 0)
		{
			L2ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
					apprentice.getPlayerInstance().setSponsor(0);
				else
					apprentice.initApprenticeAndSponsor(0, 0);

				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		if (exMember.getSponsor() != 0)
		{
			L2ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
					sponsor.getPlayerInstance().setApprentice(0);
				else
					sponsor.initApprenticeAndSponsor(0, 0);

				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		exMember.saveApprenticeAndSponsor(0, 0);

		if (Config.REMOVE_CASTLE_CIRCLETS)
			CastleManager.getInstance().removeCirclet(exMember, getHasCastle());

		if (exMember.isOnline())
		{
			L2PcInstance player = exMember.getPlayerInstance();
			player.setTitle("");
			player.setApprentice(0);
			player.setSponsor(0);

			if (player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L); //24*60*60*1000 = 86400000
			}

			// remove Clanskills from Player
			for (L2Skill skill : player.getClan().getAllSkills())
				player.removeSkill(skill, false);

			// remove Residential skills
			if (player.getClan().getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
			if (player.getClan().getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
			player.sendSkillList();

			player.setClan(null);
			player.setClanJoinExpiryTime(clanJoinExpiryTime);
			player.setPledgeClass(L2ClanMember.getCurrentPledgeClass(player));
			player.updateNameTitleColor();
			// disable clan tab
			player.sendPacket(new PledgeShowMemberListDeleteAll());
		}
		else
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderId() == objectId ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
	}

	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (L2ClanMember temp : _members.values())
		{
			if (temp.getSubPledgeType() == subpl)
				result++;
		}
		return result;
	}

	public int getMaxNrOfMembers(int subpledgetype)
	{
		int limit = 0;

		switch (subpledgetype)
		{
		case 0:
			switch (getLevel())
			{
			case 4:
				limit = 40;
				break;
			case 3:
				limit = 30;
				break;
			case 2:
				limit = 20;
				break;
			case 1:
				limit = 15;
				break;
			case 0:
				limit = 10;
				break;
			default:
				limit = 40;
				break;
			}
			break;
		case -1:
		case 100:
		case 200:
			limit = 20;
			break;
		case 1001:
		case 1002:
		case 2001:
		case 2002:
			switch (getLevel())
			{
			case 9:
			case 10:
				limit = 25;
				break;
			default:
				limit = 10;
				break;
			}
			break;
		default:
			break;
		}

		return limit;
	}

	public List<L2PcInstance> getOnlineMembersList()
	{
		List<L2PcInstance> result = new FastList<L2PcInstance>();
		for (L2ClanMember temp : _members.values())
		{
			if (temp != null)
			{
				if (temp.isOnline() && temp.getPlayerInstance() != null)
					result.add(temp.getPlayerInstance());
			}
		}

		return result;
	}

	public L2PcInstance[] getOnlineMembers(int exclude)
	{
		LinkedBunch<L2PcInstance> result = new LinkedBunch<L2PcInstance>();
		for (L2ClanMember temp : _members.values())
		{
			if (temp != null)
			{
				if (temp.isOnline() && temp.getObjectId() != exclude)
					result.add(temp.getPlayerInstance());
			}
		}

		return result.moveToArray(new L2PcInstance[result.size()]);
	}

	/**
	 * @return
	 */
	public int getAllyId()
	{
		return _allyId;
	}

	/**
	 * @return
	 */
	public String getAllyName()
	{
		return _allyName;
	}

	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}

	/**
	 * @return
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}

	/**
	 * @return
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * @return
	 */
	public int getHasCastle()
	{
		return _hasCastle;
	}

	/**
	 * @return
	 */
	public int getHasHideout()
	{
		return _hasHideout | _hasFort;
	}

	/**
	 * @return
	 */
	public int getHasFort()
	{
		return _hasFort;
	}

	/**
	 * @param crestId The id of pledge crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}

	/**
	 * @return Returns the clanCrestId.
	 */
	public int getCrestId()
	{
		return _crestId;
	}

	/**
	 * @param crestLargeId The id of pledge LargeCrest.
	 */
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}

	/**
	 * @return Returns the clan CrestLargeId
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}

	/**
	 * @param allyId The allyId to set.
	 */
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}

	/**
	 * @param allyName The allyName to set.
	 */
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}

	/**
	 * @param hasCastle The hasCastle to set.
	 */
	public void setHasCastle(int hasCastle)
	{
		_hasCastle = hasCastle;
	}

	/**
	 * @param hasHideout The hasHideout to set.
	 */
	public void setHasHideout(int hasHideout)
	{
		_hasHideout = hasHideout;
	}

	/**
	 * @param has Fortress The hasFortress to set.
	 */
	public void setHasFort(int hasFort)
	{
		_hasFort = hasFort;
	}

	/**
	 * @param level The level to set.
	 */
	public void setLevel(int level)
	{
		_level = level;
	}

	/**
	 * @param player name
	 * @return
	 */
	public boolean isMember(int id)
	{
		return (id != 0 && _members.containsKey(id));
	}

	public void updateClanInDB()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=?,ally_crest_id=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setInt(9, getAllyCrestId());
			statement.setInt(10, getClanId());

			statement.execute();
			statement.close();
			if (_log.isDebugEnabled() || Config.DEBUG)
				_log.info("New clan leader saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.error("Error while saving new clan leader.", e);
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

	public void store()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getHasCastle());
			statement.setInt(5, getAllyId());
			statement.setString(6, getAllyName());
			statement.setInt(7, getLeaderId());
			statement.setInt(8, getCrestId());
			statement.setInt(9, getCrestLargeId());
			statement.setInt(10, getAllyCrestId());
			statement.execute();
			statement.close();
			if (_log.isDebugEnabled() || Config.DEBUG)
				_log.info("New clan saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.error("Error saving new clan.", e);
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

	private void removeMemberInDatabase(L2ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE charId=?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			statement.close();
			if (_log.isDebugEnabled() || Config.DEBUG)
				_log.info("clan member removed in db: " + getClanId());

			statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error removing clan member.", e);
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

	@SuppressWarnings("unused")
	private void updateWarsInDB()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("UPDATE clan_wars SET wantspeace1=? WHERE clan1=?");
			statement.setInt(1, 0);
			statement.setInt(2, 0);

			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error updating clan wars data.", e);
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

	private void restore()
	{
		Connection con = null;
		try
		{
			L2ClanMember member;

			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con
					.prepareStatement("SELECT clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,reputation_score,auction_bid_at,ally_penalty_expiry_time,ally_penalty_type,char_penalty_expiry_time,dissolving_expiry_time FROM clan_data where clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet clanData = statement.executeQuery();

			if (clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setHasCastle(clanData.getInt("hasCastle"));
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
				if (getAllyPenaltyExpiryTime() < System.currentTimeMillis())
					setAllyPenaltyExpiryTime(0, 0);

				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
				if (getCharPenaltyExpiryTime() + Config.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis()) //24*60*60*1000 = 86400000
					setCharPenaltyExpiryTime(0);

				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));

				setCrestId(clanData.getInt("crest_id"));
				if (getCrestId() != 0)
					setHasCrest(true);

				setCrestLargeId(clanData.getInt("crest_large_id"));
				if (getCrestLargeId() != 0)
					setHasCrestLarge(true);

				setAllyCrestId(clanData.getInt("ally_crest_id"));
				setReputationScore(clanData.getInt("reputation_score"), false);
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);

				int leaderId = (clanData.getInt("leader_id"));

				PreparedStatement statement2 = con
						.prepareStatement("SELECT char_name,level,classid,charId,title,pledge_rank,subpledge,apprentice,sponsor,race,sex FROM characters WHERE clanid=?");
				statement2.setInt(1, getClanId());
				ResultSet clanMembers = statement2.executeQuery();

				while (clanMembers.next())
				{
					member = new L2ClanMember(this, clanMembers.getString("char_name"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers
							.getInt("charId"), clanMembers.getInt("subpledge"), clanMembers.getInt("pledge_rank"), clanMembers.getString("title"), clanMembers
							.getInt("sex"), clanMembers.getInt("race"));
					if (member.getObjectId() == leaderId)
						setLeader(member);
					else
						addClanMember(member);
					member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
				}
				clanMembers.close();
				statement2.close();
			}

			clanData.close();
			statement.close();

			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
		}
		catch (Exception e)
		{
			_log.error("Error restoring clan data.", e);
			_log.warn(String.valueOf(getClanId()), e);
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

	private void restoreSkills()
	{
		Connection con = null;

		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());

			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				// Add the L2Skill object to the L2Clan _skills
				if (skill!=null)
					_skills.put(skill.getId(), skill);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error restoring clan skills.", e);
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

	/** used to retrieve all skills */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];

		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}

	/** used to add a skill to skill list of this L2Clan */
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if (newSkill != null)
			oldSkill = addSkill(newSkill); // Replace oldSkill by newSkill or Add the newSkill

		return oldSkill;
	}

	/** used to add a new skill to the list, send a packet to all online clan members, update their stats and store it in db*/
	public L2Skill addNewSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		Connection con = null;

		if (newSkill != null)
		{

			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;

				if (oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getClanId());
					statement.execute();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
					statement.setInt(1, getClanId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.execute();
					statement.close();
				}
			}
			catch (Exception e)
			{
				_log.error("Error saving clan skills.", e);
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

			// notify clan members
			addSkillEffects(true);
		}

		return oldSkill;
	}

	public void addSkillEffects(boolean notify)
	{
		if (_skills.size() < 1)
			return;

		for (L2ClanMember temp : _members.values())
		{
			if (temp != null)
			{
				if (temp.isOnline() && temp.getPlayerInstance() != null)
					addSkillEffects(temp.getPlayerInstance(), notify);
			}
		}
	}

	public void addSkillEffects(L2PcInstance cm, boolean notify)
	{
		if (cm == null)
			return;

		// Add clan leader skills
		if (cm.isClanLeader() && getLevel()>=Config.SIEGE_CLAN_MIN_LEVEL)
			SiegeManager.getInstance().addSiegeSkills(cm);

		for (L2Skill skill : _skills.values())
		{
			if (skill.getMinPledgeClass() <= cm.getPledgeClass())
			{
				cm.addSkill(skill, false); // Skill is not saved to player DB
				if (notify)
					cm.sendPacket(new PledgeSkillListAdd(skill.getId(), skill.getLevel()));
			}
		}
	}

	public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	{
		if (getAllyId() == 0)
		{
			return;
		}
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId())
				clan.broadcastToOnlineMembers(packet);
		}
	}

	public void broadcastSnoopToOnlineAllyMembers(int type, String name, String text)
	{
		if (getAllyId() == 0)
			return;

		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId())
				clan.broadcastSnoopToOnlineMembers(type, name, text);
		}
	}

	public void broadcastSnoopToOnlineMembers(int type, String name, String text)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member == null || !member.isOnline())
				continue;

			L2PcInstance pl = member.getPlayerInstance();
			if (pl != null)
				pl.broadcastSnoop(type, name, text);
		}
	}

	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member == null)
				continue;

			if (member.isOnline() && member.getPlayerInstance() != null)
				member.getPlayerInstance().sendPacket(packet);
		}
	}

	public void broadcastCreatureSayToOnlineMembers(CreatureSay packet, L2PcInstance broadcaster)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member.isOnline() && member.getPlayerInstance() != null && !(Config.REGION_CHAT_ALSO_BLOCKED &&
					BlockList.isBlocked(member.getPlayerInstance(), broadcaster)))
				member.getPlayerInstance().sendPacket(packet);
		}
	}

	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2PcInstance player)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member.isOnline() && member.getPlayerInstance() != null && member.getPlayerInstance() != player)
				member.getPlayerInstance().sendPacket(packet);
		}
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * @return
	 */
	public boolean hasCrest()
	{
		return _hasCrest;
	}

	public boolean hasCrestLarge()
	{
		return _hasCrestLarge;
	}

	public void setHasCrest(boolean flag)
	{
		_hasCrest = flag;
	}

	public void setHasCrestLarge(boolean flag)
	{
		_hasCrestLarge = flag;
	}

	public ClanWarehouse getWarehouse()
	{
		return _warehouse;
	}

	public boolean isAtWarWith(Integer id)
	{
		if (_atWarWith != null && !_atWarWith.isEmpty())
		{
			if (_atWarWith.contains(id))
				return true;
		}

		return false;
	}

	public boolean isAtWarAttacker(Integer id)
	{
		if (_atWarAttackers != null && !_atWarAttackers.isEmpty())
		{
			if (_atWarAttackers.contains(id))
				return true;
		}

		return false;
	}

	public void setEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.add(id);
	}

	public void setEnemyClan(Integer clan)
	{
		_atWarWith.add(clan);
	}

	public void setAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.add(id);
	}

	public void setAttackerClan(Integer clan)
	{
		_atWarAttackers.add(clan);
	}

	public void deleteEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.remove(id);
	}

	public void deleteAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.remove(id);
	}

	public int getHiredGuards()
	{
		return _hiredGuards;
	}

	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}

	public boolean isAtWar()
	{
		return _atWarWith != null && !_atWarWith.isEmpty();
	}

	public List<Integer> getWarList()
	{
		return _atWarWith;
	}

	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}

	public void broadcastClanStatus()
	{
		for (L2PcInstance member : getOnlineMembers(0))
		{
			member.sendPacket(new PledgeShowMemberListDeleteAll());
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}

	public void removeSkill(int id)
	{
		L2Skill deleteSkill = null;
		for (L2Skill sk : _skillList)
		{
			if (sk.getId() == id)
			{
				deleteSkill = sk;
				return;
			}
		}
		_skillList.remove(deleteSkill);
	}

	public void removeSkill(L2Skill deleteSkill)
	{
		_skillList.remove(deleteSkill);
	}

	/**
	 * @return
	 */
	public List<L2Skill> getSkills()
	{
		return _skillList;
	}

	public class SubPledge
	{
		private int		_id;
		private String	_subPledgeName;
		private int		_leaderId;

		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}

		public int getId()
		{
			return _id;
		}

		public String getName()
		{
			return _subPledgeName;
		}

		public void setName(String newName)
		{
			_subPledgeName = newName;
		}

		public int getLeaderId()
		{
			return _leaderId;
		}

		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
	}

	public class RankPrivs
	{
		private int	_rankId;
		private int	_party;
		private int	_rankPrivs;

		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}

		public int getRank()
		{
			return _rankId;
		}

		public int getParty()
		{
			return _party;
		}

		public int getPrivs()
		{
			return _rankPrivs;
		}

		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}

	private void restoreSubPledges()
	{
		Connection con = null;

		try
		{
			// Retrieve all subpledges of this clan from the database
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int id = rset.getInt("sub_pledge_id");
				String name = rset.getString("name");
				int leaderId = rset.getInt("leader_id");
				// Create a SubPledge object for each record
				SubPledge pledge = new SubPledge(id, name, leaderId);
				_subPledges.put(id, pledge);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error restoring clan sub-units.", e);
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

	/** used to retrieve subPledge by type */
	public final SubPledge getSubPledge(int subpledgeType)
	{
		if (_subPledges == null)
			return null;

		return _subPledges.get(subpledgeType);
	}

	/** used to retrieve subPledge by type */
	public final SubPledge getSubPledge(String pledgeName)
	{
		if (_subPledges == null)
			return null;

		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
				return sp;
		}
		return null;
	}

	/** used to retrieve all subPledges */
	public final SubPledge[] getAllSubPledges()
	{
		if (_subPledges == null)
			return new SubPledge[0];

		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}

	public SubPledge createSubPledge(L2PcInstance player, int subPledgeType, int leaderId, String subPledgeName)
	{
		SubPledge subPledge = null;
		subPledgeType = getAvailablePledgeTypes(subPledgeType);
		if (subPledgeType == 0)
		{
			if (subPledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			else
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANT_CREATE_SUB_UNITS));
			return null;
		}
		if (_leader.getObjectId() == leaderId)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_LEADER_IS_INCORECT));
			return null;
		}

		int neededRepu = 0;
		if (subPledgeType != -1)
		{
			if (subPledgeType < L2Clan.SUBUNIT_KNIGHT1)
				neededRepu = 5000;
			else if (subPledgeType > L2Clan.SUBUNIT_ROYAL2)
				neededRepu = 10000;
		}

		// Royal Guard 5000 points per each
		// Order of Knights 10000 points per each
		if (getReputationScore() < neededRepu)
		{
			SystemMessage sp = new SystemMessage(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			player.sendPacket(sp);
			return null;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setInt(2, subPledgeType);
			statement.setString(3, subPledgeName);
			if (subPledgeType != -1)
				statement.setInt(4, leaderId);
			else
				statement.setInt(4, 0);
			statement.execute();
			statement.close();

			subPledge = new SubPledge(subPledgeType, subPledgeName, leaderId);
			_subPledges.put(subPledgeType, subPledge);

			if (subPledgeType != -1)
				setReputationScore(getReputationScore() - neededRepu, true);

			if (_log.isDebugEnabled())
				_log.debug("New sub_clan saved in db: " + getClanId() + "; " + subPledgeType);
		}
		catch (Exception e)
		{
			_log.error("Error saving sub clan data.", e);
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

		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));
		return subPledge;
	}

	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			switch (pledgeType)
			{
			case SUBUNIT_ACADEMY:
				return 0;
			case SUBUNIT_ROYAL1:
				pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
				break;
			case SUBUNIT_ROYAL2:
				return 0;
			case SUBUNIT_KNIGHT1:
				pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
				break;
			case SUBUNIT_KNIGHT2:
				pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
				break;
			case SUBUNIT_KNIGHT3:
				pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
				break;
			case SUBUNIT_KNIGHT4:
				return 0;
			}
		}
		return pledgeType;
	}

	public void updateSubPledgeInDB(int pledgeType)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setInt(1, getSubPledge(pledgeType).getLeaderId());
			statement.setInt(2, getClanId());
			statement.setInt(3, pledgeType);
			statement.execute();
			statement = con.prepareStatement("UPDATE clan_subpledges SET name=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setString(1, getSubPledge(pledgeType).getName());
			statement.setInt(2, getClanId());
			statement.setInt(3, pledgeType);
			statement.execute();
			statement.close();
			if (_log.isDebugEnabled() || Config.DEBUG)
				_log.info("New subpledge leader and/or name saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.error("Error saving new sub clan leader.", e);
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

	private void restoreRankPrivs()
	{
		Connection con = null;

		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT privilleges,rank,party FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int rank = rset.getInt("rank");
				int privileges = rset.getInt("privilleges");
				// Create a SubPledge object for each record
				if (rank == -1)
					continue;
				_privs.get(rank).setPrivs(privileges);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error restoring clan privs by rank.", e);
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

	public void initializePrivs()
	{
		RankPrivs privs;
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}
	}

	public int getRankPrivs(int rank)
	{
		if (_privs.get(rank) != null)
			return _privs.get(rank).getPrivs();

		return CP_NOTHING;
	}

	public void setRankPrivs(int rank, int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);

			Connection con = null;

			try
			{
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privilleges) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE privilleges = ?");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.setInt(5, privs);

				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not store clan privs for rank: " + e);
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

			for (L2ClanMember cm : getMembers())
			{
				if (cm.isOnline())
					if (cm.getPledgeRank() == rank)
						if (cm.getPlayerInstance() != null)
						{
							cm.getPlayerInstance().setClanPrivileges(privs);
							cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
						}
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));

			Connection con = null;

			try
			{
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privilleges) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not create new rank and store clan privs for rank: " + e);
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
	}

	/** used to retrieve all RankPrivs */
	public final RankPrivs[] getAllRankPrivs()
	{
		if (_privs == null)
			return new RankPrivs[0];

		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}

	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderId() == 0)
				continue;
			if (sp.getLeaderId() == leaderId)
			{
				id = sp.getId();
				break;
			}
		}
		return id;
	}

	public void setReputationScore(int value, boolean save)
	{
		if (_reputationScore >= 0 && value < 0)
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
						member.getPlayerInstance().removeSkill(sk, false);
				}
			}
		}
		else if (_reputationScore < 0 && value >= 0)
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
					{
						if (sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass())
							member.getPlayerInstance().addSkill(sk, false);
					}
				}
			}
		}

		_reputationScore = value;
		if (_reputationScore > 100000000)
			_reputationScore = 100000000;
		if (_reputationScore < -100000000)
			_reputationScore = -100000000;
		if (save)
			updateClanInDB();
	}

	public int getReputationScore()
	{
		return _reputationScore;
	}

	public void setRank(int rank)
	{
		_rank = rank;
	}

	public int getRank()
	{
		return _rank;
	}

	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}

	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;

		if (storeInDb)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
				statement.setInt(1, id);
				statement.setInt(2, getClanId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not store auction for clan: " + e);
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
	}

	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 *
	 * @param activeChar
	 * @param target
	 * @param pledgeType
	 * @return
	 */
	public boolean checkClanJoinCondition(L2PcInstance activeChar, L2PcInstance target, int pledgeType)
	{
		if (activeChar == null)
			return false;

		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return false;
		}
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		if (getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			return false;
		}
		if (target.getClanId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			activeChar.sendPacket(SystemMessageId.ACADEMY_REQUIREMENTS);
			return false;
		}
		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_FULL);
				sm.addString(getName());
				activeChar.sendPacket(sm);
				sm = null;
			}
			else
				activeChar.sendPacket(SystemMessageId.SUBCLAN_IS_FULL);

			return false;
		}
		return true;
	}

	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 *
	 * @param activeChar
	 * @param target
	 * @return
	 */
	public boolean checkAllyJoinCondition(L2PcInstance activeChar, L2PcInstance target)
	{
		if (activeChar == null)
			return false;

		if (activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId())
		{
			activeChar.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return false;
		}
		L2Clan leaderClan = activeChar.getClan();
		if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY);
				return false;
			}
		}
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_YOURSELF);
			return false;
		}
		if (target.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			return false;
		}
		if (!target.isClanLeader())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		L2Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE);
			sm.addString(targetClan.getName());
			sm.addString(targetClan.getAllyName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				sm.addString(target.getClan().getName());
				sm.addString(target.getClan().getAllyName());
				activeChar.sendPacket(sm);
				sm = null;
				return false;
			}
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				return false;
			}
		}
		if (SiegeManager.getInstance().checkIfInZone(activeChar) && SiegeManager.getInstance().checkIfInZone(target))
		{
			activeChar.sendPacket(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE);
			return false;
		}
		if (leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			activeChar.sendPacket(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE);
			return false;
		}

		int numOfClansInAlly = 0;
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == activeChar.getAllyId())
				++numOfClansInAlly;
		}
		if (numOfClansInAlly >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
			return false;
		}

		return true;
	}

	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}

	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}

	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}

	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}

	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}

	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}

	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}

	public void createAlly(L2PcInstance player, String allyName)
	{
		if (null == player)
			return;

		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.info(player.getObjectId() + "(" + player.getName() + ") requested ally creation from ");

		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			return;
		}
		if (getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.ALREADY_JOINED_ALLIANCE);
			return;
		}
		if (getLevel() < 5)
		{
			player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
			return;
		}
		if (getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION);
				return;
			}
		}
		if (getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING);
			return;
		}
		if (allyName.length() > 16 || allyName.length() < 3)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH);
			return;
		}
		if (!Config.CLAN_ALLY_NAME_PATTERN.matcher(allyName).matches())
		{
			player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME);
			return;
		}
		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(SystemMessageId.ALLIANCE_ALREADY_EXISTS);
			return;
		}

		setAllyId(getClanId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();

		player.sendPacket(new UserInfo(player));

		player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_CREATE_ALLIANCE),allyName));
	}

	public void dissolveAlly(L2PcInstance player)
	{
		if (getAllyId() == 0)
		{
			player.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}
		if (!player.isClanLeader() || getClanId() != getAllyId())
		{
			player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}
		if (SiegeManager.getInstance().checkIfInZone(player))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE);
			return;
		}

		broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.ALLIANCE_DISOLVED));

		long currentTime = System.currentTimeMillis();
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}

		setAllyId(0);
		setAllyName(null);
		setAllyPenaltyExpiryTime(currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, L2Clan.PENALTY_TYPE_DISSOLVE_ALLY); //24*60*60*1000 = 86400000
		updateClanInDB();

		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false, false);
	}

	public void levelUpClan(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (System.currentTimeMillis() < getDissolvingExpiryTime())
		{
			player.sendPacket(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
			return;
		}

		boolean increaseClanLevel = false;

		switch (getLevel())
		{
		case 0:
		{
			// Upgrade to 1
			if (player.getSp() >= 20000 && player.getAdena() >= 650000)
			{
				if (player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
				{
					player.setSp(player.getSp() - 20000);
					SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
					sp.addNumber(20000);
					player.sendPacket(sp);
					sp = null;
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 1:
		{
			// Upgrade to 2
			if (player.getSp() >= 100000 && player.getAdena() >= 2500000)
			{
				if (player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
				{
					player.setSp(player.getSp() - 100000);
					SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
					sp.addNumber(100000);
					player.sendPacket(sp);
					sp = null;
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 2:
		{
			// Upgrade to 3
			if (player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null)
			{
				// itemId 1419 == Blood Mark
				if (player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
				{
					player.setSp(player.getSp() - 350000);
					SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
					sp.addNumber(350000);
					player.sendPacket(sp);
					sp = null;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(1419);
					sm.addNumber(1);
					player.sendPacket(sm);
					sm = null;
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 3:
		{
			// Upgrade to 4
			if (player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null)
			{
				// itemId 3874 == Alliance Manifesto
				if (player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
				{
					player.setSp(player.getSp() - 1000000);
					SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
					sp.addNumber(1000000);
					player.sendPacket(sp);
					sp = null;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(3874);
					sm.addNumber(1);
					player.sendPacket(sm);
					sm = null;
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 4:
		{
			// Upgrade to 5
			if (player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null)
			{
				// itemId 3870 == Seal of Aspiration
				if (player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
				{
					player.setSp(player.getSp() - 2500000);
					SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
					sp.addNumber(2500000);
					player.sendPacket(sp);
					sp = null;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(3870);
					sm.addNumber(1);
					player.sendPacket(sm);
					sm = null;
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 5:
		{
			// Upgrade to 6
			if (getReputationScore() >= 10000 && getMembersCount() >= Config.MEMBER_FOR_LEVEL_SIX)
			{
				setReputationScore(getReputationScore() - 10000, true);
				SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(10000);
				player.sendPacket(cr);
				cr = null;
				increaseClanLevel = true;
			}
			break;
		}
		case 6:
		{
			// Upgrade to 7
			if (getReputationScore() >= 20000 && getMembersCount() >= Config.MEMBER_FOR_LEVEL_SEVEN)
			{
				setReputationScore(getReputationScore() - 20000, true);
				SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(20000);
				player.sendPacket(cr);
				cr = null;
				increaseClanLevel = true;
			}
			break;
		}
		case 7:
		{
			// Upgrade to 8
			if (getReputationScore() >= 40000 && getMembersCount() >= Config.MEMBER_FOR_LEVEL_EIGHT)
			{
				setReputationScore(getReputationScore() - 40000, true);
				SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(40000);
				player.sendPacket(cr);
				cr = null;
				increaseClanLevel = true;
			}
			break;
		}
		/*		case 8:
		{
			// Upgrade to 9
			if (getReputationScore() >= 40000 && player.getInventory().getItemByItemId(9910) != null && getMembersCount() >= Config.MEMBER_FOR_LEVEL_NINE)
			{
				// itemId 9910 == Blood Oath
				if (player.destroyItemByItemId("ClanLvl", 9910, 150, player.getTarget(), false))
				{
					setReputationScore(getReputationScore() - 40000, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(40000);
					player.sendPacket(cr);
					cr = null;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(9910);
					sm.addNumber(150);
					player.sendPacket(sm);
					increaseClanLevel = true;
				}
			}
			break;
		}
		case 9:
		{
			// Upgrade to 10
			if (getReputationScore() >= 40000 && player.getInventory().getItemByItemId(9911) != null && getMembersCount() >= Config.MEMBER_FOR_LEVEL_TEN)
			{
				// itemId 9911 == Blood Alliance
				if (player.destroyItemByItemId("ClanLvl", 9911, 5, player.getTarget(), false))
				{
					setReputationScore(getReputationScore() - 40000, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(40000);
					player.sendPacket(cr);
					cr = null;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(9911);
					sm.addNumber(5);
					player.sendPacket(sm);
					increaseClanLevel = true;
				}
			}
			break;
		} */
		default:
			return;
		}

		if (!increaseClanLevel)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			return;
		}

		// the player should know that he has less sp now :p
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);

		player.broadcastPacket(new SocialAction(player.getObjectId(), 15));

		ItemList il = new ItemList(player, false);
		player.sendPacket(il);

		changeLevel(getLevel() + 1);

		player.updateNameTitleColor();
	}

	public void changeLevel(int level)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getClanId());
			statement.execute();
			statement.close();

			con.close();
		}
		catch (Exception e)
		{
			_log.warn("could not increase clan level:" + e);
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

		setLevel(level);

		if (getLeader().isOnline())
		{
			L2PcInstance leader = getLeader().getPlayerInstance();
			if (level >=Config.SIEGE_CLAN_MIN_LEVEL)
				SiegeManager.getInstance().addSiegeSkills(leader);
			else 
				SiegeManager.getInstance().removeSiegeSkills(leader);
			if (level > 4)
				leader.sendPacket(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
		}

		// notify all the members about it
		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}

	public List<L2PcInstance> getOnlineAllyMembers()
	{
		List<L2PcInstance> list = new FastList<L2PcInstance>();
		if (getAllyId() == 0)
			return list;

		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId())
				list.addAll(clan.getOnlineMembersList());
		}
		return list;
	}

	public ClanListenerList getListners()
	{
		return clanListnerList;
	}
}
