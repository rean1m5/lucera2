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
package ru.catssoftware.util.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author NB4L1
 */
public final class ScheduledFutureWrapper implements ScheduledFuture<Object>
{
	private final ScheduledFuture<?> _future;

	public ScheduledFutureWrapper(ScheduledFuture<?> future)
	{
		_future = future;
	}

	public long getDelay(TimeUnit unit)
	{
		return _future.getDelay(unit);
	}

	public int compareTo(Delayed o)
	{
		return _future.compareTo(o);
	}

	/**
	 * Just make sure to avoid wrong usage of Future.cancel(true).
	 */
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return _future.cancel(false);
	}

	public Object get() throws InterruptedException, ExecutionException
	{
		return _future.get();
	}

	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return _future.get(timeout, unit);
	}

	public boolean isCancelled()
	{
		return _future.isCancelled();
	}

	public boolean isDone()
	{
		return _future.isDone();
	}
}