package ru.catssoftware.gameserver.threadmanager;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ScheduledFutureWrapper implements ScheduledFuture<Object>
{
	private final ScheduledFuture<?> _future;
	
	public ScheduledFutureWrapper(ScheduledFuture<?> future)
	{
		_future = future;
	}
	
	@Override
	public long getDelay(TimeUnit unit)
	{
		return _future.getDelay(unit);
	}
	
	@Override
	public int compareTo(Delayed o)
	{
		return _future.compareTo(o);
	}
	
	/**
	 * Just make sure to avoid wrong usage of Future.cancel(true).
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return _future.cancel(false);
	}
	
	@Override
	public Object get() throws InterruptedException, ExecutionException
	{
		return _future.get();
	}
	
	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return _future.get(timeout, unit);
	}
	
	@Override
	public boolean isCancelled()
	{
		return _future.isCancelled();
	}
	
	@Override
	public boolean isDone()
	{
		return _future.isDone();
	}
}
