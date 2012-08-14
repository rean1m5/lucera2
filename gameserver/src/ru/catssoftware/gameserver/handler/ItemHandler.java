package ru.catssoftware.gameserver.handler;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.util.JarUtils;

public class ItemHandler
{
	private final static Logger			_log	= Logger.getLogger(ItemHandler.class.getName());
	private static ItemHandler			_instance;

	private Map<Integer, IItemHandler>	_datatable;
	private Map<Integer, IExItemHandler>	_exHandlers = new FastMap<Integer, IExItemHandler>();

	public static ItemHandler getInstance()
	{
		if (_instance == null)
			_instance = new ItemHandler();
		return _instance;
	}

	private ItemHandler()
	{
		_datatable = new TreeMap<Integer, IItemHandler>();
		try {
			for(String handler : JarUtils.enumClasses("ru.catssoftware.gameserver.handler.itemhandlers")) try {
				Class<?> _handler = Class.forName(handler);
				if(_handler!=null && IItemHandler.class.isAssignableFrom(_handler)) {
					Constructor<?> ctor = _handler.getConstructor();
					if(ctor!=null) 
						registerItemHandler((IItemHandler)ctor.newInstance());
				}
			} catch(Exception e) {
				continue;
			}
		} catch(Exception e) {
		
		}
		
		
		_log.info("ItemHandler: Loaded " + _datatable.size() + " handlers.");
	}

	public void registerItemHandler(IItemHandler handler)
	{
		int[] ids = handler.getItemIds();
		for (int element : ids)
			_datatable.put(element, handler);
	}

	public void registerExHandler(IExItemHandler handler) {
		for(int i : handler.getItemIds())
			_exHandlers.put(i, handler);
	}
	public IExItemHandler getExHandler(int id) {
		return _exHandlers.get(id);
	}
	public IItemHandler getItemHandler(int itemId)
	{
		return _datatable.get(itemId);
	}
}