package ru.catssoftware.gameserver.idfactory;

import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class IdFactory
{
	protected final static Logger		_log				= Logger.getLogger(IdFactory.class.getName());

	protected static final String[]	ID_UPDATES			=
														{
			"UPDATE items                 SET owner_id = ?    WHERE owner_id = ?",
			"UPDATE items                 SET object_id = ?   WHERE object_id = ?",
			"UPDATE character_quests      SET charId = ?      WHERE charId = ?",
			"UPDATE character_friends     SET charId = ?      WHERE charId = ?",
			"UPDATE character_friends     SET friendId = ?    WHERE friendId = ?",
			"UPDATE character_hennas      SET charId = ?      WHERE charId = ?",
			"UPDATE character_recipebook  SET charId = ?      WHERE charId = ?",
			"UPDATE character_shortcuts   SET charId = ?      WHERE charId = ?",
			"UPDATE character_shortcuts   SET shortcut_id = ? WHERE shortcut_id = ? AND type = 1",
			"UPDATE character_macroses    SET charId = ?      WHERE charId = ?",
			"UPDATE character_skills      SET charId = ?      WHERE charId = ?",
			"UPDATE character_skills_save SET charId = ?      WHERE charId = ?",
			"UPDATE character_subclasses  SET charId = ?      WHERE charId = ?",
			"UPDATE characters            SET charId = ?      WHERE charId = ?",
			"UPDATE characters            SET clanid = ?      WHERE clanid = ?",
			"UPDATE clan_data             SET clan_id = ?     WHERE clan_id = ?",
			"UPDATE siege_clans           SET clan_id = ?     WHERE clan_id = ?",
			"UPDATE clan_data             SET ally_id = ?     WHERE ally_id = ?",
			"UPDATE clan_data             SET leader_id = ?   WHERE leader_id = ?",
			"UPDATE pets                  SET item_obj_id = ? WHERE item_obj_id = ?",
			"UPDATE auction_bid          SET bidderId = ?      WHERE bidderId = ?",
			"UPDATE auction_watch        SET charId = ?        WHERE charId = ?",
			"UPDATE character_hennas     SET charId = ?        WHERE charId = ?",
			"UPDATE clan_wars            SET clan1 = ?         WHERE clan1 = ?",
			"UPDATE clan_wars            SET clan2 = ?         WHERE clan2 = ?",
			"UPDATE clanhall             SET ownerId = ?       WHERE ownerId = ?",
			"UPDATE petitions            SET charId = ?        WHERE charId = ?",
			"UPDATE posts                SET post_ownerid = ?  WHERE post_ownerid = ?",
			"UPDATE seven_signs          SET charId = ?        WHERE charId = ?",
			"UPDATE topic                SET topic_ownerid = ? WHERE topic_ownerid = ?",
			"UPDATE items_on_ground      SET object_id = ?     WHERE object_id = ?",
			"UPDATE olympiad_nobles          SET charId = ?         WHERE charId = ?",
			"UPDATE clan_privs               SET clan_id = ?        WHERE clan_id = ?",
			"UPDATE clan_skills              SET clan_id = ?        WHERE clan_id = ?",
			"UPDATE clan_subpledges          SET clan_id = ?        WHERE clan_id = ?",
			"UPDATE character_recommends     SET charId = ?         WHERE charId = ?",
			"UPDATE character_recommends     SET target_id = ?      WHERE target_id = ?",
			"UPDATE character_raid_points    SET charId = ?         WHERE charId = ?",
			"UPDATE couples                  SET id = ?             WHERE id = ?",
			"UPDATE couples                  SET player1Id = ?      WHERE player1Id = ?",
			"UPDATE couples                  SET player2Id = ?      WHERE player2Id = ?",
			"UPDATE cursed_weapons           SET playerId = ?       WHERE playerId = ?",
			"UPDATE forums                   SET forum_owner_id = ? WHERE forum_owner_id = ?",
			"UPDATE heroes                   SET charId = ?         WHERE charId = ?" };

	protected static final String[]	ID_CHECKS			=
														{
			"SELECT owner_id    FROM items                 WHERE object_id >= ?   AND object_id < ?",
			"SELECT object_id   FROM items                 WHERE object_id >= ?   AND object_id < ?",
			"SELECT charId      FROM character_quests      WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_friends     WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_friends     WHERE friendId >= ?    AND friendId < ?",
			"SELECT charId      FROM character_hennas      WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_recipebook  WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_shortcuts   WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_macroses    WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_skills      WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_skills_save WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM character_subclasses  WHERE charId >= ?      AND charId < ?",
			"SELECT charId      FROM characters            WHERE charId >= ?      AND charId < ?",
			"SELECT clanid      FROM characters            WHERE clanid >= ?      AND clanid < ?",
			"SELECT clan_id     FROM clan_data             WHERE clan_id >= ?     AND clan_id < ?",
			"SELECT clan_id     FROM siege_clans           WHERE clan_id >= ?     AND clan_id < ?",
			"SELECT ally_id     FROM clan_data             WHERE ally_id >= ?     AND ally_id < ?",
			"SELECT leader_id   FROM clan_data             WHERE leader_id >= ?   AND leader_id < ?",
			"SELECT item_obj_id FROM pets                  WHERE item_obj_id >= ? AND item_obj_id < ?",
			// added by DaDummy
			"SELECT friendId    FROM character_friends     WHERE friendId >= ?    AND friendId < ?",
			"SELECT charId      FROM seven_signs           WHERE charId >= ?      AND charId < ?",
			"SELECT object_id   FROM items_on_ground       WHERE object_id >= ?   AND object_id < ?" };

	protected boolean				_initialized;

	public static final int			FIRST_OID			= 0x10000000;
	public static final int			LAST_OID			= 0x7FFFFFFF;
	public static final int			FREE_OBJECT_ID_SIZE	= LAST_OID - FIRST_OID;

	protected static IdFactory		_instance			= null;


	protected IdFactory()
	{
		setAllCharacterOffline();
		cleanUpDB();
	}


	/**
	 * Sets all character offline
	 */
	protected void setAllCharacterOffline()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			Statement s2 = con.createStatement();
			s2.executeUpdate("UPDATE characters SET online = 0;");
			s2.close();
		}
		catch (SQLException e)
		{
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
	 * Cleans up Database
	 */
	protected void cleanUpDB()
	{
		// TODO:
		// Check for more cleanup query
		// Check order

		Connection con = null;
		try
		{
			int cleanCount = 0;
			con = L2DatabaseFactory.getInstance().getConnection(con);
			Statement stmt = con.createStatement();

			// If a character not exists
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.friendId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_hennas WHERE character_hennas.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_macroses WHERE character_macroses.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_quests WHERE character_quests.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_recipebook WHERE character_recipebook.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_shortcuts WHERE character_shortcuts.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills WHERE character_skills.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_skills_save WHERE character_skills_save.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_subclasses WHERE character_subclasses.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM character_raid_points WHERE charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM couples WHERE couples.player1Id NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM couples WHERE couples.player2Id NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM heroes WHERE heroes.charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM olympiad_nobles WHERE charId NOT IN (SELECT charId FROM characters);");
			cleanCount += stmt.executeUpdate("DELETE FROM items WHERE loc <> 'clanwh' and items.owner_id NOT IN (SELECT charId FROM characters) && items.owner_id > 1000000;");
			cleanCount += stmt.executeUpdate("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);");
			cleanCount += stmt.executeUpdate("DELETE FROM seven_signs WHERE seven_signs.charId NOT IN (SELECT charId FROM characters);");

			// If a clan not exists
			cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auction_bid.bidderId NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_privs WHERE clan_privs.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_skills WHERE clan_skills.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM clan_wars WHERE clan_wars.clan1 NOT IN (SELECT clan_id FROM clan_data) OR clan_wars.clan2 NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM forums WHERE forum_owner_id <> 0 AND forums.forum_owner_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM items WHERE loc = 'clanwh' AND items.owner_id NOT IN (SELECT clan_id FROM clan_data);");
			cleanCount += stmt.executeUpdate("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);");

			stmt.executeUpdate("UPDATE characters SET `clanid`='0', `clan_privs`='0' WHERE characters.clanid NOT IN (SELECT clan_id FROM clan_data);");
			stmt.executeUpdate("UPDATE clan_subpledges SET leader_id=0 WHERE clan_subpledges.leader_id NOT IN (SELECT charId FROM characters) AND leader_id > 0;");
			stmt.executeUpdate("UPDATE clan_data SET ally_id=0 WHERE clan_data.ally_id NOT IN (SELECT clanid FROM characters WHERE clanid!=0 GROUP BY clanid);");
			stmt.executeUpdate("UPDATE clanhall SET ownerId=0, paidUntil=0, paid=0 WHERE clanhall.ownerId NOT IN (SELECT clan_id FROM clan_data);");

			// If the clanhall isn't free
			//cleanCount += stmt.executeUpdate("DELETE FROM auction WHERE auction.id IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
			//cleanCount += stmt.executeUpdate("DELETE FROM auction_bid WHERE auction_bid.auctionId IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
			stmt.executeUpdate("UPDATE clan_data SET auction_bid_at = 0 WHERE auction_bid_at NOT IN (SELECT auctionId FROM auction_bid);");
			// If the clanhall is free
			cleanCount += stmt.executeUpdate("DELETE FROM clanhall_functions WHERE clanhall_functions.hall_id NOT IN (SELECT id FROM clanhall WHERE ownerId <> 0);");

			// Others
			stmt.executeUpdate("DELETE FROM item_attributes WHERE `itemId` NOT IN (SELECT object_id FROM items);");

			stmt.close();
			_log.info("IdFactory: Cleaned " + cleanCount + " elements");
		}
		catch (SQLException e)
		{
			_log.error(e.getMessage(), e);
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
	 * @return
	 * @throws SQLException
	 */
	protected int[] extractUsedObjectIDTable() throws SQLException
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			//create a temporary table
			Statement s = con.createStatement();
			try
			{
				s.executeUpdate("DROP TABLE temporaryObjectTable");
			}
			catch (SQLException e)
			{
			}

			s.executeUpdate("CREATE TABLE temporaryObjectTable" + " (object_id int NOT NULL PRIMARY KEY)");

			s.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT charId FROM characters");
			if(Config.ID_FACTORY_CLEANUP) {
				_log.info("IdFactory: Cleanup items");
				s.executeUpdate("delete from items where object_id in (select object_id from temporaryObjectTable)");
			}
			s.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT object_id FROM items");
			if(Config.ID_FACTORY_CLEANUP) {
				_log.info("IdFactory: Cleanup clans");
				s.executeUpdate("delete from clan_data where clan_id in (select object_id from temporaryObjectTable)");
			}
			s.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT clan_id FROM clan_data");
			if(Config.ID_FACTORY_CLEANUP) {
				_log.info("IdFactory: Cleanup items on ground");
				s.executeUpdate("delete from items_on_ground where object_id in (select object_id from temporaryObjectTable)");
			}
			s.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT object_id FROM items_on_ground");
			s.executeUpdate("INSERT INTO temporaryObjectTable (object_id)" + " SELECT letterId from character_mail");
			ResultSet result = s.executeQuery("SELECT COUNT(object_id) FROM temporaryObjectTable");

			result.next();
			int size = result.getInt(1);
			int[] tmp_obj_ids = new int[size];
			result.close();

			result = s.executeQuery("SELECT object_id FROM temporaryObjectTable ORDER BY object_id");

			int idx = 0;
			while (result.next())
			{
				tmp_obj_ids[idx++] = result.getInt(1);
			}

			result.close();
			s.close();

			return tmp_obj_ids;
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

	public boolean isInitialized()
	{
		return _initialized;
	}

	public static IdFactory getInstance()
	{
		if(_instance==null)
		{
				switch (Config.IDFACTORY_TYPE)
				{
				case BitSet:
					_instance = new BitSetIDFactory();
					break;
				case Stack:
					_instance = new StackIDFactory();
					break;
				case Increment:
					_instance = new IncrementIDFactory();
					break;
				case Rebuild:
					_instance = new BitSetRebuildFactory();
					break;
				}
		}
		return _instance;
	}

	public abstract int getNextId();

	/**
	 * return a used Object ID back to the pool
	 * @param id
	 */
	public abstract void releaseId(int id);

	public abstract int getCurrentId();

	public abstract int size();
}