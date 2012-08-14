package ru.catssoftware.gameserver.handler;

import java.util.Map;
import java.util.Set;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;


import javolution.util.FastMap;

public class ReloadHandler {
	private static ReloadHandler _instance = null;
	public static ReloadHandler getInstance() {
		if(_instance==null)
			_instance = new ReloadHandler();
		return _instance;
	}
	private Map<String, IReloadHandler> _handlers = new FastMap<String, IReloadHandler>();
	private ReloadHandler() {
		
	}
	public void registerHandler(String name, IReloadHandler handler) {
		_handlers.put(name, handler);
	}
	public Set<String> getHandlers() {
		return _handlers.keySet();
	}
	public boolean isRegistred(String handler) {
		return _handlers.containsKey(handler);
	}
	public void reload(String handler, L2PcInstance actor ) {
		IReloadHandler h = _handlers.get(handler);
		if(h!=null)
			h.reload(actor);
		else
			actor.sendMessage("//reload "+handler+" not define.");
	}
}
