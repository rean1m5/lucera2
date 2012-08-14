package ru.catssoftware.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Couple;

public class CoupleManager
{
	private static final Logger		_log	= Logger.getLogger(CoupleManager.class.getName());

	private static CoupleManager	_instance;

	public static final CoupleManager getInstance()
	{
		if (_instance == null)
			_instance = new CoupleManager();
		return _instance;
	}

	private CoupleManager()
	{
		load();
	}

	private FastList<Couple>	_couples;

	public void reload()
	{
		getCouples().clear();
		load();
	}

	private final void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection(con);
			statement = con.prepareStatement("Select id from couples order by id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				getCouples().add(new Couple(rs.getInt("id")));
			}
			statement.close();
			_log.info("CoupleManager: loaded " + getCouples().size() + " couples(s)");
		}
		catch (Exception e)
		{
			_log.error("Exception: CoupleManager.load(): " + e.getMessage(), e);
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

	public final Couple getCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		if (index >= 0)
			return getCouples().get(index);
		return null;
	}

	public void createCouple(L2PcInstance player1, L2PcInstance player2)
	{
		if (player1 != null && player2 != null)
		{
			if (player1.getPartnerId() == 0 && player2.getPartnerId() == 0)
			{
				int _player1id = player1.getObjectId();
				int _player2id = player2.getObjectId();

				Couple _new = new Couple(player1, player2);
				getCouples().add(_new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());
			}
		}
	}

	public void deleteCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		Couple couple = getCouples().get(index);
		if (couple != null)
		{
			L2PcInstance player1 = L2World.getInstance().getPlayer(couple.getPlayer1Id());
			L2PcInstance player2 = L2World.getInstance().getPlayer(couple.getPlayer2Id());
			L2ItemInstance item = null;
			if (player1 != null)
			{
				player1.setPartnerId(0);
				player1.setMaried(false);
				player1.setCoupleId(0);
				item = player1.getInventory().getItemByItemId(9140);
				if (player1.isOnline() == 1 && item != null)
				{
					player1.destroyItem("Removing Cupids Bow", item, player1, true);
				}
				if (player1.isOnline() == 0 && item != null)
				{
					Integer PlayerId = player1.getObjectId();
					Integer ItemId = 9140;
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(con);
						PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
						statement.setInt(1, PlayerId);
						statement.setInt(2, ItemId);
						statement.execute();
						statement.close();
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
			}
			if (player2 != null)
			{
				player2.setPartnerId(0);
				player2.setMaried(false);
				player2.setCoupleId(0);
				item = player2.getInventory().getItemByItemId(9140);
				if (player2.isOnline() == 1 && item != null)
				{
					player2.destroyItem("Removing Cupids Bow", item, player2, true);
				}
				if (player2.isOnline() == 0 && item != null)
				{
					Integer Player2Id = player2.getObjectId();
					Integer Item2Id = 9140;
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(con);
						PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
						statement.setInt(1, Player2Id);
						statement.setInt(2, Item2Id);
						statement.execute();
						statement.close();
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
			}
			couple.divorce();
			getCouples().remove(index);
		}
	}

	public final int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (Couple temp : getCouples())
		{
			if (temp != null && temp.getId() == coupleId)
				return i;
			i++;
		}
		return -1;
	}

	public final FastList<Couple> getCouples()
	{
		if (_couples == null)
			_couples = new FastList<Couple>();
		return _couples;
	}
}