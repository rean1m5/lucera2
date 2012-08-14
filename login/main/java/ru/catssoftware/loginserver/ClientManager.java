package ru.catssoftware.loginserver;

import java.util.Map;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.loginserver.network.serverpackets.LoginFailReason;

public class ClientManager extends Thread {
	private static Logger _log = Logger.getLogger("ClientManager");
	private Map<L2LoginClient,Long> _clients;
	private static ClientManager _instance = null;
	private boolean _running = true;
	public static ClientManager getInstance() {
		if(_instance == null)
			_instance = new ClientManager();
		return _instance;
	}
	private ClientManager() {
		_clients = new FastMap<L2LoginClient,Long>();
		if(Config.DDOS_PROTECTION_ENABLED) {
			_log.info("DDoS-Proof: Started client manager for "+Config.MAX_SESSIONS+" sessions");
			_log.info("DDoS-Proof: Session time set to "+Config.SESSION_TTL/1000+" seconds");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					_running = false;
				}
			});
			start();
		}
	}
	public void addClient(L2LoginClient cl) {
		synchronized (_clients) {
			_clients.put(cl,System.currentTimeMillis());
		}
	}
	public void removeClient(L2LoginClient cl) {
		synchronized (_clients) {
			if(_clients.containsKey(cl))
				_clients.remove(cl);
		}
	}
	
	@Override
	public void run() {
		while(_running) {
			synchronized (_clients) {
				if(_clients.size()>Config.MAX_SESSIONS) {
					_log.warn("DDoS-Proof: To many connections. Flushing all");
					for(L2LoginClient cl : _clients.keySet())
						cl.close(LoginFailReason.REASON_ACCESS_FAILED);
					_clients.clear();
				} else
				for(L2LoginClient cl : _clients.keySet()) {
					if(_clients.get(cl)+Config.SESSION_TTL < System.currentTimeMillis()) {
						cl.close(LoginFailReason.REASON_ACCESS_FAILED);
						_clients.remove(cl);
					}
				}
			}
			try { Thread.sleep(Config.SESSION_TTL/2); } catch(Exception e) { }
		}
		System.out.println("DDoS-Proof stopped");
	}
}
