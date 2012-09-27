package ru.catssoftware.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.TradeList.TradeItem;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

 /**
  * Auhor: Azagthtot / CatsSoftware
  * Optimized by m095
  * Last change 10.05.2010
  **/

public class L2PcOffline extends Thread
{
	private static final Logger		_log						= Logger.getLogger(L2PcOffline.class);

	public static void storeTradeItems(L2PcInstance pc, PreparedStatement stm) throws SQLException
	{
		switch(pc.getPrivateStoreType())
		{
			case L2PcInstance.STORE_PRIVATE_BUY:
				for(TradeItem it: pc.getBuyList().getItems())
				{
					stm.setInt(2, it.getItem().getItemId());
					stm.setInt(3, it.getCount());
					stm.setInt(4, it.getPrice());
					stm.execute();
				}
				break;
			case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
			case L2PcInstance.STORE_PRIVATE_SELL:
				for(TradeItem it: pc.getSellList().getItems())
				{
					stm.setInt(2, it.getObjectId());
					stm.setInt(3, it.getCount());
					stm.setInt(4, it.getPrice());
					stm.execute();
				}
				break;
			case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
				for(L2ManufactureItem it: pc.getCreateList().getList())
				{
					stm.setInt(2, it.getRecipeId() );
					stm.setInt(3, 0);
					stm.setInt(4, it.getCost());
					stm.execute();
				}
		}
	}

	public static void saveOffliners()
	{
		System.out.print("Save ofline traders...");
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement1 = con.prepareStatement("INSERT INTO character_offline VALUES(?,?,?,?,?,?)");
			PreparedStatement statement2 = con.prepareStatement("INSERT INTO character_offline_shop VALUES(?,?,?,?)");
			for(L2PcInstance pc : L2World.getInstance().getAllPlayers())
			{
				if (pc != null)
				{
					if (pc.isOfflineTrade() && pc.getEndOfflineTime() > System.currentTimeMillis())
					{
						synchronized(pc)
						{
							int shopId = IdFactory.getInstance().getNextId();
							statement2.setInt(1, shopId);
							storeTradeItems(pc, statement2);
							statement1.setInt(1, pc.getObjectId());
							statement1.setInt(2, shopId);
							statement1.setInt(3, pc.getPrivateStoreType());
							String title = "";
							int isPackage  = 0;
							if (pc.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY)
							{
								title = pc.getBuyList().getTitle();
								isPackage = pc.getBuyList().isPackaged()?1:0;
							}
							else if (pc.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || pc.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
							{
								if (pc.getSellList().getTitle() != null)
									title = pc.getSellList().getTitle();
								isPackage = pc.getSellList().isPackaged()||pc.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL ?1:0;
							}
							else
							{
								if (pc.getCreateList() != null)
								{
									if (pc.getCreateList().getStoreName() != null)
										title = pc.getCreateList().getStoreName();
								}
							}
							statement1.setInt(4, isPackage);
							statement1.setString(5, title);
							statement1.setLong(6,pc.getEndOfflineTime());
							statement1.execute();
						}
					}
				}
			}
			statement2.close();
			statement1.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.out.println("done");
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

	public static void loadOffliners()
	{
		_log.info("Offline Manager: load players from database.");
		int nTraders = 0;
		int remTraders = 0;
		Connection con = null;
		L2PcInstance offliner;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("SELECT charId, shopid, mode, packaged, title, endTime FROM character_offline");
			PreparedStatement stm2 = con.prepareStatement("SELECT itemid, count, price FROM character_offline_shop WHERE shopid=?");
			ResultSet rs = stm.executeQuery();
			while(rs.next())
			{
				offliner = L2PcInstance.load(rs.getInt(1));
				if(offliner != null)
				{
					if(rs.getLong(6) < System.currentTimeMillis())
					{
						remTraders++;
					}
					else
					{
						try
						{
							stm2.setInt(1, rs.getInt(2));
							ResultSet rs2 = stm2.executeQuery();
							while (rs2.next())
							{
								switch(rs.getInt(3))
								{
									case L2PcInstance.STORE_PRIVATE_BUY:
										offliner.getBuyList().addItemByItemId(rs2.getInt(1), rs2.getInt(2), rs2.getInt(3));
										break;
									case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
									case L2PcInstance.STORE_PRIVATE_SELL:
										offliner.getSellList().addItem(rs2.getInt(1), rs2.getInt(2), rs2.getInt(3));
										break;
									case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
										if(offliner.getCreateList()==null)
											offliner.setCreateList(new L2ManufactureList());
										offliner.getCreateList().add(new L2ManufactureItem(rs2.getInt(1),rs2.getInt(3)));
										break;
								}
							}
							rs2.close();
							offliner.setPrivateStoreType(rs.getInt(3));
	
							switch(rs.getInt(3))
							{
								case L2PcInstance.STORE_PRIVATE_BUY:
									offliner.getBuyList().setTitle(rs.getString(5));
									offliner.getBuyList().setPackaged(rs.getInt(4) != 0);
									break;
								case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
								case L2PcInstance.STORE_PRIVATE_SELL:
									offliner.getSellList().setTitle(rs.getString(5));
									offliner.getSellList().setPackaged(rs.getInt(4) != 0);
									break;
								case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
									offliner.getCreateList().setStoreName(rs.getString(5));
									break;
							}
							offliner.setOnlineStatus(true);
							offliner.setOfflineTrade(true);
							offliner.setEndOfflineTime(true, rs.getLong(6));
							offliner.spawnMe();
							offliner.sitDown();
							offliner.broadcastUserInfo(true);
							nTraders++;
						}
						catch(Exception e)
						{
							e.printStackTrace();
							offliner.decayMe();
							offliner = null;
						}
					}
				}
			}
			stm2.close();
			rs.close();
			stm.close();
			stm = con.prepareStatement("DELETE FROM character_offline");
			stm.execute();
			stm.close();
			stm = con.prepareStatement("DELETE FROM character_offline_shop");
			stm.execute();
			stm.close();
		}
		catch(Exception e)
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
		_log.info("Offline Manager: " + nTraders + " trader(s) loaded.");
		_log.info("Offline Manager: " + remTraders + " trader(s) deleted.");
		offliner = null;
	}

	public static void clearOffliner()
	{
		_log.info("Offline Manager: restore traders disables.");
		Connection con = null;
		try
		{
			PreparedStatement stm1;
			PreparedStatement stm2;

			con = L2DatabaseFactory.getInstance().getConnection();
			stm1 = con.prepareStatement("DELETE FROM character_offline");
			stm1.execute();
			stm1.close();
			stm2 = con.prepareStatement("DELETE FROM character_offline_shop");
			stm2.execute();
			stm2.close();
		}
		catch(Exception e)
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
}