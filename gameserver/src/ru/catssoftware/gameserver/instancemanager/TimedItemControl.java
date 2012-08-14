package ru.catssoftware.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.taskmanager.OfflineManager;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;

import javolution.util.FastMap;

public class TimedItemControl
{
	private static TimedItemControl				_instance;
	private FastMap<Integer,Info>				_timedItems		= new FastMap<Integer,Info>();
	private static Logger 							_log			= Logger.getLogger(TimedItemControl.class);
	public static final TimedItemControl getInstance()
	{
		if (_instance == null)
			_instance = new TimedItemControl();
		return _instance;
	}
	
	private TimedItemControl()
	{
		restore();
		_startControlTask.schedule(60000);
	}
	
	public boolean getActiveTimedItem(L2PcInstance pl, boolean trade)
	{
//		if(!Config.ReadyToPts)
//			return false;
		for (Info i:_timedItems.values())
		{
			if (i!=null && i._charId==pl.getObjectId())
			{
				L2ItemInstance item = pl.getInventory().getItemByObjectId(i._itemId);
				if (item!=null)
				{
					if (trade && (item.getItemId()==Config.OFFLINE_TRADE_PRICE_ITEM_ID))
					{
						if (System.currentTimeMillis()<i._activationTime)
							return true;
					}
					else if (item.getItemId()==Config.OFFLINE_CRAFT_PRICE_ITEM_ID)
					{
						if (System.currentTimeMillis()<i._activationTime)
							return true;
					}
				}
			}
		}
		return false;
	}
	
	public synchronized void destroyItem(L2ItemInstance item) {
		Info inf = _timedItems.get(item.getObjectId());
		if(inf !=null) {
			_timedItems.remove(inf._itemId);
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
					statement = con.prepareStatement("DELETE FROM character_timed_items WHERE charId=? AND itemId=?");
					statement.setInt(1, inf._charId);
					statement.setInt(2, inf._itemId);
					statement.execute();
					statement.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally {
				try {
					con.close();
				} catch(Exception e) { }
			}
		}
	}
	public synchronized void setTimedItem(L2ItemInstance item) {
		Info inf = _timedItems.get(item.getObjectId());
		if(inf!=null) {
				inf._charId = item.getOwnerId();
		} else {
			inf = new Info();
			inf._activationTime = System.currentTimeMillis()/1000+item.getItem().getLifetime()*60;
			inf._charId = item.getOwnerId();
			inf._itemId = item.getObjectId();
			_timedItems.put(inf._itemId, inf);
		}
		saveToDb(inf);
	}
	public synchronized boolean setActiveTimedItem(L2PcInstance pl, boolean trade)
	{
//		if(!Config.ReadyToPts)
//			return false;
		if (trade)
		{
			for (L2ItemInstance item : pl.getInventory().getItemsByItemId(Config.OFFLINE_TRADE_PRICE_ITEM_ID))
			{
				if (!isActiveItem(item) && pl.getSellList().getItem(item.getObjectId())==null)
				{
					long finishTime=System.currentTimeMillis()+(Config.OFFLINE_TRADE_PRICE_ITEM_ID_TIME*(1000*60*60));
					Info inf=new Info();
					inf._activationTime=finishTime;
					inf._charId=pl.getObjectId();
					inf._itemId=item.getObjectId();
					_timedItems.put(inf._itemId,inf);
					saveToDb(inf);
					return true;
				}
			}
		}
		else
		{
			for (L2ItemInstance item : pl.getInventory().getItemsByItemId(Config.OFFLINE_CRAFT_PRICE_ITEM_ID))
			{
				if (!isActiveItem(item))
				{
					long finishTime=System.currentTimeMillis()+(Config.OFFLINE_CRAFT_PRICE_ITEM_ID_TIME*(1000*60*60));
					Info inf=new Info();
					inf._activationTime=finishTime;
					inf._charId=pl.getObjectId();
					inf._itemId=item.getObjectId();
					_timedItems.put(inf._itemId, inf);
					saveToDb(inf);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isActiveItem(L2ItemInstance item)
	{
		for (Info i:_timedItems.values())
		{
			if (i._itemId==item.getObjectId())
				return true;
		}		
		return false;
	}
	
	private void restore()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT charId,itemId,time FROM character_timed_items");
			ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				Info inf=new Info();
				inf._activationTime=rs.getLong("time");
				inf._charId=rs.getInt("charId");
				inf._itemId=rs.getInt("itemId");
				_timedItems.put(inf._itemId, inf);
			}
			rs.close();
			statement.close();
			_log.info("TimedItems: loaded "+_timedItems.size()+" items ");
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
		
	private void saveToDb(Info temp)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("update character_timed_items set charId=? where itemId=?");
			statement.setInt(1, temp._charId);
			statement.setInt(2, temp._itemId);
			if(statement.executeUpdate()==0) {
				statement.close();
				statement = con.prepareStatement("INSERT INTO character_timed_items (charId,itemId,time) VALUES (?,?,?)");
				statement.setInt(1, temp._charId);
				statement.setInt(2, temp._itemId);
				statement.setLong(3, temp._activationTime);
				statement.execute();
			}
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
	private void deleteItem(Info temp)
	{
		_timedItems.remove(temp._itemId);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
				statement = con.prepareStatement("DELETE FROM character_timed_items WHERE charId=? AND itemId=?");
				statement.setInt(1, temp._charId);
				statement.setInt(2, temp._itemId);
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
		L2PcInstance pl = L2World.getInstance().getPlayer(temp._charId);
		if (pl!=null)
		{
			L2ItemInstance item = pl.getInventory().getItemByObjectId(temp._itemId);
			if (item!=null)
			{
				if(item.isEquipped())
					pl.getInventory().unEquipItemInSlot(item.getLocationSlot());
				pl.getInventory().destroyItem("timeLost", item, pl, pl);
				pl.sendPacket(new ItemList(pl, false));
			}
			if (pl.isOfflineTrade() && (item.getItemId() == Config.OFFLINE_CRAFT_PRICE_ITEM_ID ||
					item.getItemId() == Config.OFFLINE_TRADE_PRICE_ITEM_ID))
			{
				OfflineManager.getInstance().removeTrader(pl);
				new Disconnection(pl).defaultSequence(false);
			} else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				msg.addItemName(item);
				pl.sendPacket(msg);
			}
		}
		else
		{
			con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				if(temp._charId!=0) {
					statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND object_id=?");
					statement.setInt(1, temp._charId);
					statement.setInt(2, temp._itemId);
					statement.execute();
					statement.close();
				} else  {
					for(L2Object o: L2World.getInstance().getAllVisibleObjects()) {
						if(o.getObjectId()==temp._itemId) {
							L2World.getInstance().removeVisibleObject(o, o.getWorldRegion());
							break;
						}
					}
				}
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
	private final ExclusiveTask _startControlTask = new ExclusiveTask(){
		@Override
		protected void onElapsed()
		{
			for (Info temp : _timedItems.values())
			{
				if(temp._activationTime<(System.currentTimeMillis()/1000))
				{
					
					deleteItem(temp);
				}
			}
			schedule(60000);
		}
	};	
	private class Info
	{
		int _charId;
		int _itemId;
		long _activationTime;
	}
}