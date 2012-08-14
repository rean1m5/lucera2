package ru.catssoftware.extension;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.catssoftware.extension.ExtensionManager;
import ru.catssoftware.extension.IExtension;
import ru.catssoftware.extension.ObjectExtension.Action;

import javolution.util.FastList;
import javolution.util.FastMap;



public class GameExtensionManager extends ExtensionManager {
	 
	private Map<Class<?>, List<ObjectExtension>> _extensions = new FastMap<Class<?>, List<ObjectExtension>>();
	
	public static GameExtensionManager getInstance() {
		if(_instance==null)
			_instance = new GameExtensionManager();
		return (GameExtensionManager)_instance;
	}
	
	@Override
	protected boolean loadExt(IExtension ext) {
		if(_extensions==null)
			_extensions = new FastMap<Class<?>, List<ObjectExtension>>();
		if(super.loadExt(ext)) {
			if(ext instanceof ObjectExtension)  
				registerExtension((ObjectExtension)ext);
			return true;
		}
		return false;
	}
	
	public Object handleAction(Object object, Action action, Object...params) {
		List<ObjectExtension> list = _extensions.get(object.getClass());
		if(list!=null) {
			for(ObjectExtension oh : list) {
				
				Object result = oh.hanlde(object, action, params);
				if(result!=null)
					return result;
			}
		}
		return null;
	}
	
	@Override
	protected void loadAdditional() {
		File f = new File("./config/main/extension.properties");
		if(f.exists()) try {
			Properties p = new Properties();
			p.load(new FileInputStream(f));
			for (Object o : p.keySet())  try {
					Class<?> clazz = Class.forName(p.getProperty(o.toString()));
					if(clazz!=null && ObjectExtension.class.isAssignableFrom(clazz)) {
						Constructor<?> ctor = clazz.getConstructor();
						if(ctor!=null)
							loadExt((IExtension)ctor.newInstance());
					}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	public void registerExtension(ObjectExtension oe) {
		Class<?> [] forClasses = oe.forClasses();
		if(forClasses!=null) {
			for(Class<?> s : forClasses) {
				List<ObjectExtension> list = _extensions.get(s);
				if(list==null) {
					list = new FastList<ObjectExtension>();
					_extensions.put(s, list);
				}
				if(!list.contains(oe))
					list.add(oe);
			}
		}
		
	}
}
