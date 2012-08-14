package ru.catssoftware.gameserver.model.entity.events;

import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.CTF.CTF;
import ru.catssoftware.gameserver.model.entity.events.DeathMatch.DeathMatch;
import ru.catssoftware.gameserver.model.entity.events.LastHero.LastHero;
import ru.catssoftware.gameserver.model.entity.events.TvT.TvT;
import sun.misc.Service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GameEventManager
{
	private static Logger _log = Logger.getLogger("Event");
	private L2Properties _eventStartup;
	private static GameEventManager _instance;
	private Map<GameEvent,Long> _events = new FastMap<GameEvent,Long>();

	public static GameEventManager getInstance()
	{
		if(_instance==null)
			_instance = new GameEventManager();
		return _instance;
	}


	private GameEventManager()
	{
			try
			{
				_eventStartup = new L2Properties("./config/mods/events_start.properties");
			}
			catch(IOException e)
			{
				_log.warn("GameEventManager: Unable to read startup file. Startup will be disabled");
				_eventStartup = null;
			}
			Iterator<?> iterator = Service.providers(GameEvent.class);
			while(iterator.hasNext())
			{
				GameEvent evt = (GameEvent)iterator.next();
				registerEvent(evt);
			}
		registerEvent(TvT.getInstance());
		registerEvent(LastHero.getInstance());
		registerEvent(CTF.getInstance());
		registerEvent(DeathMatch.getInstance());
		_log.info("GameEventManager: Loaded " + _events.size() + " events.");
	}
	
	private class EventStart implements Runnable
	{
		private GameEvent _evt;

		public EventStart(GameEvent evt)
		{
			_evt = evt;
		}

		@Override
		public void run()
		{
			if(_evt.isState(GameEvent.State.STATE_OFFLINE))
			{
				_log.info("GameEventManager: Starting event "+_evt.getName());
				_evt.start();
			}
			else 
				_log.info("GameEventManager: Event "+_evt.getName()+" active now unable to start");

			if(_events.get(_evt)!=-1)
			{
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, (int)(_events.get(_evt)/60000));
				_log.info("GameEventManager: Event "+_evt.getName()+" scheduled at "+String.format("%tT",cal));
				ThreadPoolManager.getInstance().scheduleGeneral(this,_events.get(_evt));
			}
		}
	}

	public void registerEvent(GameEvent evt)
	{
		if(evt.load())
		{
			
			long restart = -1;

			if(_eventStartup.getProperty(evt.getName()+".AutoStart")!=null)
			{
				if(Boolean.parseBoolean(_eventStartup.getProperty(evt.getName()+".AutoStart")))
				{
					String s  = _eventStartup.getProperty(evt.getName()+".NextDelay");
					if (s!=null && s.length()!=0)
						restart = Long.parseLong(s) *60000;
					s  = _eventStartup.getProperty(evt.getName()+".DelayOnBoot");

					if(s!=null && s.length()!=0)
					{
						long delay = Long.parseLong(s)*60000;
						ThreadPoolManager.getInstance().scheduleGeneral(new EventStart(evt), delay);
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.MINUTE, (int)delay/60000);
						_log.info("GameEventManager: Event "+evt.getName()+" scheduled at "+String.format("%tT",cal));
					} 
					else if(restart!=-1)
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new EventStart(evt), restart);
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.MINUTE, (int)restart/60000);
						_log.info("GameEventManager: Event "+evt.getName()+" scheduled at "+String.format("%tT",cal));
					}	
				}
			}
			_events.put(evt,restart);
		}
	}

	public GameEvent findEvent(String name)
	{
		for(GameEvent evt : _events.keySet()) 
			if(name.equals(evt.getName())) 
				return evt;
		return null;
	}
	
	public Set<GameEvent> getAllEvents()
	{
		return _events.keySet();
	}

	public GameEvent participantOf(L2PcInstance player)
	{
		for(GameEvent evt : _events.keySet())
		{
			if(evt.isParticipant(player)) 
				return evt;
		}
		return null;
	}
}
