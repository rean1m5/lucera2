package ru.catssoftware.util.concurrent;

import ru.catssoftware.lang.RunnableImpl;

public class ExecuteWrapper extends RunnableImpl
{
	private final RunnableImpl _runnable;

	public ExecuteWrapper(RunnableImpl runnable)
	{
		_runnable = runnable;
	}

	public final void runImpl()
	{
		ExecuteWrapper.execute(_runnable, getMaximumRuntimeInMillisecWithoutWarning());
	}

	protected long getMaximumRuntimeInMillisecWithoutWarning()
	{
		return Long.MAX_VALUE;
	}

	public static void execute(RunnableImpl runnable)
	{
		execute(runnable, Long.MAX_VALUE);
	}

	public static void execute(RunnableImpl runnable, long maximumRuntimeInMillisecWithoutWarning)
	{
		long begin = System.nanoTime();

		try
		{
			runnable.run();
		}
		catch (Exception e)
		{
		}
		finally
		{
			long runtimeInNanosec = System.nanoTime() - begin;
			Class<? extends Runnable> clazz = runnable.getClass();

			RunnableStatsManager.getInstance().handleStats(clazz, runtimeInNanosec);
		}
	}
}