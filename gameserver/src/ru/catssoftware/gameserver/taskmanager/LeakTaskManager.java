/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.taskmanager;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.actor.reference.ImmutableReference;

/**
 * @author NB4L1
 */
public final class LeakTaskManager
{
	private static final Logger		_log	= Logger.getLogger(LeakTaskManager.class);

	private static LeakTaskManager	_instance;

	public static LeakTaskManager getInstance()
	{
		if (_instance == null)
			_instance = new LeakTaskManager();

		return _instance;
	}

	private final Map<Class<?>, LeakHandler>	_handlers	= new FastMap<Class<?>, LeakHandler>().setShared(true);

	private LeakTaskManager()
	{
		_log.info("LeakTaskManager: Initialized.");
	}

	public void add(ImmutableReference<?> ref)
	{
		getHandler(ref.getReferentClass()).add(ref);
	}

	private LeakHandler getHandler(Class<?> cl)
	{
		LeakHandler handler = _handlers.get(cl);
		if (handler == null)
			_handlers.put(cl, (handler = new LeakHandler(cl)));

		return handler;
	}

	private class LeakHandler implements Runnable
	{
		private static final int						DELAY	= 3600000;

		private final Class<?>							_cl;
		private final List<ImmutableReference<?>>		_list	= new FastList<ImmutableReference<?>>();
		private final Map<ImmutableReference<?>, Long>	_map	= new FastMap<ImmutableReference<?>, Long>();

		private LeakHandler(Class<?> cl)
		{
			_cl = cl;

			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, DELAY, DELAY);
		}

		private void add(ImmutableReference<?> ref)
		{
			_list.add(ref);
			_map.put(ref, System.currentTimeMillis());
		}

		public void run()
		{
			cleanup();
			check();
		}

		private void cleanup()
		{
			_log.info("LeakTaskManager: " + _list.size() + " " + _cl + " are waiting for cleanup.");
			for (ImmutableReference<?> ref : _list)
			{
				Object obj = ref.get();
				if (obj != null)
				{
					/* here comes a deep cleanup */;
				}
			}
			_list.clear();
		}

		private void check()
		{
			DecimalFormat df = new DecimalFormat("##0.00");

			for (ImmutableReference<?> ref : _map.keySet())
			{
				if (ref.get() == null)
				{
					double diff = (double) (System.currentTimeMillis() - _map.get(ref)) / 60000;
					_log.info("LeakTaskManager: Removed after " + df.format(diff) + " minutes -> (" + ref.getName() + ")");
					_map.remove(ref);
				}
			}
			_log.info("LeakTaskManager: " + _map.size() + " " + _cl + " are leaking.");
		}
	}
}