package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import ru.catssoftware.L2DatabaseFactory;


import javolution.util.FastMap;

public class IconTable {
	private static IconTable _instance;
	public static IconTable getInstance() {
		if(_instance==null)
			_instance = new IconTable();
		return _instance;
	}
	private Map<Integer, String> _icons = new FastMap<Integer, String>();
	private IconTable() {
		try {
		Connection con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement stm = con.prepareStatement("select * from item_icons");
		ResultSet rs = stm.executeQuery();
		while(rs.next()) {
			if(rs.getString(2)!=null && !rs.getString(2).isEmpty())
				_icons.put(rs.getInt(1), rs.getString(2));
		}
		rs.close();
		stm.close();
		con.close();
		} catch(SQLException e) {}
	}
	public String getIcon(int id) {
		if(_icons.containsKey(id))
			return _icons.get(id);
		return "icon.skill4416_etc";
	}
}
