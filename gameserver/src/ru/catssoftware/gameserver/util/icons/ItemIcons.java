package ru.catssoftware.gameserver.util.icons;

import org.apache.log4j.Logger;
import ru.catssoftware.L2DatabaseFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

/*
 * @author Ro0TT
 * @date 12.01.2012
 */

public class ItemIcons
{
	private static ItemIcons _instance = null;

	protected static Logger _log = Logger.getLogger(ItemIcons.class.getSimpleName());

	public static ItemIcons getInstance()
	{
		if (_instance==null)
			_instance = new ItemIcons();
		return _instance;
	}
	
	HashMap<Integer, String> _items;
	
	public String getIcon(int itemId)
	{
		return _items.containsKey(itemId) ? _items.get(itemId) : "";
	}
	
	public ItemIcons()
	{
		_instance = this;
		_items = new HashMap<Integer, String>();
		java.sql.Connection con = null;
		PreparedStatement statement;
		ResultSet rset;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT itemId,icon FROM `item_icons`");
			rset = statement.executeQuery();
			int itemId;
			String icon;
			int count = 0;
			while(rset.next())
			{
				itemId = rset.getInt("itemId");
				icon = rset.getString("icon");
				_items.put(itemId,icon);
				count++;
			}
			_log.info("Loaded " + count + " Item Icons.");
		}
		catch(final Exception e)
		{
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
}