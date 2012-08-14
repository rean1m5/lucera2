package ru.catssoftware.gameserver.threadmanager;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.Shutdown.ShutdownModeType;

public final class DeadlockDetector extends Thread
{
	private static final Logger			_log			= Logger.getLogger(DeadlockDetector.class.getName());
	private static DeadlockDetector		_instance;

	public static DeadlockDetector getInstance()
	{
		if (_instance == null)
		{ 
			_log.info("DeadlockDetector: Initializing");
			_instance = new DeadlockDetector();
		}
		return _instance;
	}

	private final class DeadlockDetectorTask implements Runnable
	{
		private final Set<Long> _logged = new FastSet<Long>();

		public void run()
		{
			checkForDeadlocks();
		}

		private void checkForDeadlocks()
		{
			long[] ids = findDeadlockedThreadIDs();
			if (ids == null) return;

			List<Thread> deadlocked = new ArrayList<Thread>();

			for (long id : ids)
				if (_logged.add(id)) deadlocked.add(findThreadById(id));

			if (!deadlocked.isEmpty())
			{
				_log.fatal("*** Deadlocked Thread(s) ***");
				for (Thread thread : deadlocked)
				{
					_log.fatal(thread);

					for (StackTraceElement trace : thread.getStackTrace())
						_log.fatal("\tat " + trace);
				}
				new Halt().start();
				Shutdown.getInstance().startShutdown("DeadlockDetector", 1, ShutdownModeType.RESTART);
			}
		}

		private long[] findDeadlockedThreadIDs()
		{
			return ManagementFactory.getThreadMXBean().findDeadlockedThreads();
		}

		private Thread findThreadById(long id)
		{
			for (Thread thread : Thread.getAllStackTraces().keySet())
				if (thread.getId() == id) return thread;

			throw new IllegalStateException("Deadlocked Thread not found!");
		}
	}

	private DeadlockDetector()
	{
		start();
	}

	private DeadlockDetectorTask _task = new DeadlockDetectorTask();
	@Override
	public void run() {
		for(;;) try {
			Thread.sleep(Config.DEADLOCKCHECK_INTERVAL);
			_task.run();
			
		} catch(Exception e) { }
	}
	private static final class Halt extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				sleep(40000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			finally
			{
				Shutdown.getInstance().halt("DeadlockDetector");
			}
		}
	}
}