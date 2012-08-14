package ru.catssoftware.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.FriendList;

public class L2FriendList
{
	private final static Logger			_log					= Logger.getLogger(L2FriendList.class.getName());
	private static final String			RESTORE_FRIENDLIST		= "SELECT friendId,friend_name FROM character_friends WHERE charId=?";
	private static final String			RESTORE_FRIEND_ID		= "SELECT friendId FROM character_friends WHERE charId=? AND friend_name=?";
	private static final String			DELETE_FROM_FRIENDLIST	= "DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)";
	private static final String			ADD_TO_FRIENDLIST		= "INSERT INTO character_friends (charId, friendId, friend_name) VALUES (?, ?, ?),(?, ?, ?)";
	private static final String			UPDATE_FDRIEND_NAME		= "UPDATE character_friends SET friend_name=? WHERE friendId=?";

	private final Map<Integer, String>	friendlist;
	private final L2PcInstance			listOwner;

	public L2FriendList(L2PcInstance character)
	{
		friendlist = new FastMap<Integer, String>();
		listOwner = character;
		loadFriendList();
	}

	/**
	 * Restore frien list from DB
	 */
	private void loadFriendList()
	{
		if (listOwner != null)
		{
			Connection con = null;

			try
			{

				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				statement = con.prepareStatement(RESTORE_FRIENDLIST);
				statement.setInt(1, listOwner.getObjectId());
				ResultSet rset = statement.executeQuery();

				while (rset.next())
					friendlist.put(rset.getInt("friendId"), rset.getString("friend_name"));

				statement.close();
			}
			catch (Exception e)
			{
				_log.error("Error restoring friend data.", e);
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
	 * Add friend list record in DB
	 *
	 * @param character - friend instance
	 */
	private void addToFriendList(L2PcInstance character)
	{
		if (character != null && !isInFriendList(character))
		{
			Connection con = null;

			try
			{

				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				statement = con.prepareStatement(ADD_TO_FRIENDLIST);
				statement.setInt(1, listOwner.getObjectId());
				statement.setInt(2, character.getObjectId());
				statement.setString(3, character.getName());
				statement.setInt(4, character.getObjectId());
				statement.setInt(5, listOwner.getObjectId());
				statement.setString(6, listOwner.getName());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not insert friend data:" + e);
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

			friendlist.put(character.getObjectId(), character.getName());
			character.getFriendList().getFriendList().put(listOwner.getObjectId(), listOwner.getName());
		}
	}

	/**
	 * Remove friend list record from DB
	 *
	 * @param _character - friend name
	 */
	private void removeFromFriendList(String _character)
	{
		if (isInFriendList(_character))
		{
			Connection con = null;

			int _friendId = restoreFriendId(_character);

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				statement = con.prepareStatement(DELETE_FROM_FRIENDLIST);
				statement.setInt(1, listOwner.getObjectId());
				statement.setInt(2, _friendId);
				statement.setInt(3, _friendId);
				statement.setInt(4, listOwner.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("Could not delete friend data:" + e);
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

			friendlist.remove(_friendId);
		}
	}

	/**
	 * Remove friend list record from DB
	 *
	 * @param character - friend instance
	 */
	private void removeFromFriendList(L2PcInstance character)
	{
		Connection con = null;

		try
		{

			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement(DELETE_FROM_FRIENDLIST);
			statement.setInt(1, listOwner.getObjectId());
			statement.setInt(2, character.getObjectId());
			statement.setInt(3, character.getObjectId());
			statement.setInt(4, listOwner.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not delete friend data:" + e);
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

		friendlist.remove(character.getObjectId());
		character.getFriendList().getFriendList().remove(listOwner.getObjectId());
	}

	/**
	 * Get friend ID
	 *
	 * @param _character - friend name
	 * @return - character ID
	 */
	private int restoreFriendId(String _character)
	{
		int _friendId = 0;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement(RESTORE_FRIEND_ID);
			statement.setInt(1, listOwner.getObjectId());
			statement.setString(2, _character);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
				_friendId = rset.getInt("friendId");
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not get friend id:" + e);
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

		return _friendId;
	}

	/**
	 * Check is character in friend list
	 *
	 * @param character
	 * @return
	 */
	private boolean isInFriendList(L2PcInstance character)
	{
		return friendlist.containsKey(character.getObjectId());
	}

	/**
	 * Check is character in friend list
	 *
	 * @param _character - character name
	 * @return
	 */
	private boolean isInFriendList(String _character)
	{
		return friendlist.containsValue(_character);
	}

	/**
	 * Get all friend names from friend list
	 *
	 * @return List of friends names
	 */
	private String[] getFriendListNames()
	{
		return friendlist.values().toArray(new String[friendlist.size()]);
	}

	/**
	 * Get friend list
	 *
	 * @return Friend list
	 */
	private Map<Integer, String> getFriendList()
	{
		return friendlist;
	}

	/**
	 * Add character to friend list
	 *
	 * @param requestor
	 * @param character
	 */
	public static void addToFriendList(L2PcInstance requestor, L2PcInstance character)
	{
		requestor.getFriendList().addToFriendList(character);
		requestor.sendPacket(new FriendList(requestor));
		character.sendPacket(new FriendList(character));
	}

	/**
	 * Remove offline character from friend list by character's name
	 *
	 * @param requestor
	 * @param character
	 */
	public static void removeFromFriendList(L2PcInstance requestor, String character)
	{
		requestor.getFriendList().removeFromFriendList(character);
		requestor.sendPacket(new FriendList(requestor));
	}

	/**
	 * Remove online character from friend list
	 *
	 * @param requestor
	 * @param character
	 */
	public static void removeFromFriendList(L2PcInstance requestor, L2PcInstance character)
	{
		requestor.getFriendList().removeFromFriendList(character);

		requestor.sendPacket(new FriendList(requestor));
		character.sendPacket(new FriendList(character));
	}

	/**
	 * Check character is in friend list
	 *
	 * @param requestor
	 * @param _character
	 * @return is character with name _character is in requestor's friend list
	 */
	public static boolean isInFriendList(L2PcInstance requestor, String _character)
	{
		return requestor.getFriendList().isInFriendList(_character);
	}

	/**
	 * Check character is in friend list
	 *
	 * @param requestor
	 * @param character
	 * @return is character with is in requestor's friend list
	 */
	public static boolean isInFriendList(L2PcInstance requestor, L2PcInstance character)
	{
		return requestor.getFriendList().isInFriendList(character);
	}

	/**
	 * Get all friend names from friend list
	 *
	 * @param requestor
	 * @return List of friends names
	 */
	public static String[] getFriendListNames(L2PcInstance requestor)
	{
		return requestor.getFriendList().getFriendListNames();
	}

	/**
	 * Get friend list
	 *
	 * @param requestor
	 * @return Friend list
	 */
	public static Map<Integer, String> getFriendList(L2PcInstance requestor)
	{
		return requestor.getFriendList().getFriendList();
	}

	/**
	 * Обновление имен друзей при смене ника
	 * Данное обновление меняет ник только в базе
	 * Применение для friendlist(Map) происходит только после рестарта сервера
	 */
	public static void updateFriendName(L2PcInstance player)
	{
		if (player == null)
			return;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(UPDATE_FDRIEND_NAME);
			statement.setString(1, player.getName());
			statement.setInt(2, player.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("L2FriendList:  Could not change friend name...");
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
