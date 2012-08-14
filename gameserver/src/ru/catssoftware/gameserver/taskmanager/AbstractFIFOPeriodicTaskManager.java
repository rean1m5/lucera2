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

import ru.catssoftware.gameserver.taskmanager.AbstractPeriodicTaskManager;
import ru.catssoftware.util.L2FastSet;
import ru.catssoftware.util.concurrent.RunnableStatsManager;

/**
 * @author NB4L1
 */
public abstract class AbstractFIFOPeriodicTaskManager<T> extends AbstractPeriodicTaskManager
{
	private final L2FastSet<T> _queue = new L2FastSet<T>();
	
	protected AbstractFIFOPeriodicTaskManager(int period)
	{
		super(period);
	}
	
	public final void add(T cha)
	{
		synchronized(_queue)
		{
			_queue.add(cha);
		}
	}
	
	private  final T removeFirst()
	{
		try {
			synchronized(_queue)
			{
				return _queue.removeFirst();
			}
		} catch(NullPointerException npe) {
			return null;
		}
	}
	
	@Override
	public final void run()
	{
		for (T task; (task = removeFirst()) != null;)
		{
			final long begin = System.nanoTime();
			
			try
			{
				callTask(task);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				RunnableStatsManager.getInstance().handleStats(task.getClass(), getCalledMethodName(), System.nanoTime() - begin);
			}
		}
	}
	protected abstract void callTask(T task);
	protected abstract String getCalledMethodName();
}
