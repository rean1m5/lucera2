package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.util.ArrayUtils;

import javolution.util.FastMap;

public class ClassTreeTable {
	private class ClassInfo {
		public String name;
		private int parentClass;
	}
	private static ClassTreeTable _instance;
	private Map<Integer, ClassInfo> _classes = new FastMap<Integer, ClassInfo>();
	
	public static ClassTreeTable getInstance() {
		if(_instance==null)
			_instance = new ClassTreeTable();
		return _instance;
	}
	private ClassTreeTable() {
		try {
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("select * from class_list");
			ResultSet rs = stm.executeQuery();
			while(rs.next()) {
				ClassInfo info = new ClassInfo();
				info.name = rs.getString("class_name");
				info.parentClass = rs.getInt("parent_id");
				_classes.put(rs.getInt("id"), info);
			}
			rs.close();
			stm.close();
			con.close();
		} catch(SQLException e) {
			
		}
	}
	public ClassId getClassId(String shortName) {
		for(int id : _classes.keySet())
			if(_classes.get(id).name.equals(shortName))
				return ClassId.values()[id];
		return ClassId.spellhowler;
	}
	public String [] getParentClasses(int classId) {
		String [] result = null;
		ClassInfo info = _classes.get(classId);
		while(info!=null) {
			result = ArrayUtils.add(result,info.name );
			info = _classes.get(info.parentClass);
		}
		return result;
	}
	public Collection<ClassInfo> getAllClasses() {
		return _classes.values();
	}
}
