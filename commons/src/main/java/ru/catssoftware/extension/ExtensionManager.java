package ru.catssoftware.extension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;



import ru.catssoftware.AbstractDatabaseFactory;
import ru.catssoftware.extension.IExtension.ExtensionInfo;
import sun.misc.Service;

public class ExtensionManager {
	protected static Logger _log = Logger.getLogger(ExtensionManager.class);
	protected static ExtensionManager _instance = null;
	protected Map<String, Integer> _plugins = new FastMap<String, Integer>();
	protected int nloaded = 0;
	protected Connection _con;
	
	public static ExtensionManager getInstance() {
		if(_instance == null) 
			_instance = new ExtensionManager();
		return _instance;
	}
	protected ExtensionManager() {
	}
	public void load() {
		try {
			_con = AbstractDatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = _con.prepareStatement("select * from plugins_info");
			ResultSet rs = stm.executeQuery();
			while(rs.next()) {
				_plugins.put(rs.getString("name"), rs.getInt("version"));
			}
			rs.close();
			stm.close();
			Iterator<?> it = Service.providers(IExtension.class);
			while(it.hasNext()) { 
				loadExt((IExtension)it.next());
			}
			loadAdditional();
			_log.info("ExtensionManager: Loaded "+nloaded+" extension(s)");
			_con.close();
		} catch(Exception e) {
			_log.warn("ExtensionManager:  Unable to access plugin database. Extensions disabled",e);
		}
	}
	
	protected void loadAdditional() {
		
	}
	
	protected boolean loadExt(IExtension ext) {
		ExtensionInfo info = ext.getInfo();
		if(info!=null && info.installRequired()) {
			if(!_plugins.containsKey(ext.getClass().getName()) || _plugins.get(ext.getClass().getName())<info.getVersion()) try {
				if(ext.install(_plugins.containsKey(ext.getClass().getName()))) {
					PreparedStatement stm = _con.prepareStatement("update plugins_info set version=? where name=?");
					stm.setInt(1, info.getVersion());
					stm.setString(2, ext.getClass().getName());
					if(stm.executeUpdate()==0) {
						stm.close();
						stm = _con.prepareStatement("insert into plugins_info (version,name) values(?,?)");
						stm.setInt(1, info.getVersion());
						stm.setString(2, ext.getClass().getName());
						stm.execute();
					}
					stm.close();
				} else 
					return false;
			} catch(Exception e) {
				_log.warn("ExtensionManager: Error while install extension "+ext.getClass().getName(),e);
			}
		}
		if(ext.load())  {
			ext.init();
			nloaded++;
			return true;
		} 
		return false;
	}

}
