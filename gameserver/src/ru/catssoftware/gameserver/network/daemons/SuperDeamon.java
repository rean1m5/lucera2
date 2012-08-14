package ru.catssoftware.gameserver.network.daemons;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.lang.RunnableImpl;
import sun.misc.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Azagthtot
 *	Класс, управляющий всеми демонами, которые обрабатываю результаты топов<br>
 *  В настоящий момент интервал полла определяется старым параметром L2TopDeamonPoll
 */
public class SuperDeamon extends RunnableImpl
{
	private static SuperDeamon	_instance 			= null;
	private static Logger			_log				= Logger.getLogger(SuperDeamon.class);
	private List<IDeamon>		_registredDeamons;

	public static SuperDeamon getInstance()
	{
		if(_instance==null)
			_instance = new SuperDeamon();
		return _instance;
	}

	private SuperDeamon()
	{
		_registredDeamons = new FastList<IDeamon>();
		Iterator<?> deamons = Service.providers(IDeamon.class);
		while(deamons.hasNext())
		{
				Object clazz = deamons.next();
				if(IDeamon.class.isAssignableFrom(clazz.getClass()))
				{
					IDeamon deamon = (IDeamon)clazz;
					if(deamon.load())
						_registredDeamons.add(deamon);
				}
		}

		// Регистрация "родного" демона для l2top
		L2TopDeamon l2top = new L2TopDeamon();
		if(l2top.load())
			_registredDeamons.add(l2top);
		MMOTop mmtop = new MMOTop();
		if(mmtop.load())
			_registredDeamons.add(mmtop);
		if(_registredDeamons.size() > 0)
		{
				_log.info("SuperDeamon: registred "+_registredDeamons.size()+" handler(s)");
				ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 10000, Config.L2TOPDEMON_POLLINTERVAL*60000);
				_log.info("SuperDeamon: staring with poll interval "+Config.L2TOPDEMON_POLLINTERVAL+" minute(s)");
		}
	}

	@Override
	public void runImpl()
	{
		for(IDeamon deamon : _registredDeamons)
		{
			try
			{
				String []urls = deamon.getUrl().split("@@@@");
				for(String s : urls) {
					if(s.trim().length()==0)
						continue;
					URL url = new URL(s.trim());
					InputStream stream = url.openStream(); 
					deamon.parse(stream);
					stream.close();
					url = null;
				}
			}
			catch(Exception e)
			{
				if (Config.DEBUG)
					_log.warn("SuperDeamon: Error while running " + deamon.getName() + ". Error: " + e);
				else
					_log.info("SuperDeamon: Error while running " + deamon.getName() + ".");
			}
		}
		SQLQueue.getInstance().run();
	}
}
