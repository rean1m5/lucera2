package ru.catssoftware.gameserver.threadmanager;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;


public final class L2Thread extends Timer
{
	private static final Logger _log = Logger.getLogger(L2Thread.class);
	private Thread _thread;

	public L2Thread(String name)
	{
		super(name);

		schedule(new Runnable() {
			public void run()
			{
				_thread = Thread.currentThread();
			}
		}, 0);
	}

	public L2Thread schedule(Runnable runnable, long delay)
	{
		schedule(new L2TimerTask(runnable), delay);
		return this;
	}

	public L2Thread scheduleAtFixedRate(Runnable runnable, long delay, long period)
	{
		scheduleAtFixedRate(new L2TimerTask(runnable), delay, period);
		return this;
	}

	public void interrupt()
	{
		cancel();
		_thread.interrupt();
	}

	private class L2TimerTask extends TimerTask
	{
		private final Runnable _runnable;

		private L2TimerTask(Runnable runnable)
		{
			_runnable = runnable;
		}

		@Override
		public void run()
		{
			try
			{
				_runnable.run();
			}
			catch (Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	@Override
	public String toString()
	{
		return "L2" + super.toString();
	}

	public static CharSequence getStats(Thread t)
	{
		StringBuilder list = new StringBuilder();
		String newline = System.getProperty("line.separator");

		list.append(t.toString() + " - ID: " + t.getId() + newline);
		list.append(" * State: " + t.getState() + newline);
		list.append(" * Alive: " + t.isAlive() + newline);
		list.append(" * Daemon: " + t.isDaemon() + newline);
		list.append(" * Interrupted: " + t.isInterrupted() + newline);

		for (ThreadInfo info : ManagementFactory.getThreadMXBean().getThreadInfo(new long[] { t.getId() }, true, true))
		{
			for (MonitorInfo monitorInfo : info.getLockedMonitors())
			{
				list.append(" * Locked monitor: " + monitorInfo + newline);
				list.append("\t* [" + monitorInfo.getLockedStackDepth() + ".]: at " + monitorInfo.getLockedStackFrame() + newline);
			}

			for (LockInfo lockInfo : info.getLockedSynchronizers())
				list.append(" * Locked synchronizer: " + lockInfo + newline);

			for (StackTraceElement trace : info.getStackTrace())
				list.append("\tat " + trace + newline);
		}
		return list;
	}

	public static CharSequence getStats()
	{
		StringBuilder list = new StringBuilder();
		String newline = System.getProperty("line.separator");

		list.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date()) + newline);
		list.append("## Java Platform Information ##" + newline);
		list.append("Java Runtime Name: " + System.getProperty("java.runtime.name") + newline);
		list.append("Java Version: " + System.getProperty("java.version") + newline);
		list.append("Java Class Version: " + System.getProperty("java.class.version") + newline);
		list.append("");
		list.append("## Virtual Machine Information ##" + newline);
		list.append("VM Name: " + System.getProperty("java.vm.name") + newline);
		list.append("VM Version: " + System.getProperty("java.vm.version") + newline);
		list.append("VM Vendor: " + System.getProperty("java.vm.vendor") + newline);
		list.append("VM Info: " + System.getProperty("java.vm.info") + newline);
		list.append(newline);
		list.append("## OS Information ##" + newline);
		list.append("Name: " + System.getProperty("os.name") + newline);
		list.append("Architeture: " + System.getProperty("os.arch") + newline);
		list.append("Version: " + System.getProperty("os.version") + newline);
		list.append(newline);
		list.append("## Runtime Information ##" + newline);
		list.append("CPU Count: " + Runtime.getRuntime().availableProcessors() + newline);
		list.append(newline);
		list.append(getMemUsage() + newline);
		list.append(newline);

		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		list.append("## " + threads.size() + " thread(s) ##" + newline);
		list.append("=================================================" + newline);

		int i = 1;
		for (Thread thread : threads)
		{
			list.append(newline);
			list.append(i++ + "." + newline);
			list.append(getStats(thread));
		}
		return list;
	}

	public static CharSequence getMemUsage()
	{
		double maxMem = ((Runtime.getRuntime().maxMemory() / 1024)); // maxMemory is the upper limit the jvm can use
		double allocatedMem = ((Runtime.getRuntime().totalMemory() / 1024)); //totalMemory the size of the current allocation pool
		double nonAllocatedMem = maxMem - allocatedMem; //non allocated memory till jvm limit
		double cachedMem = ((Runtime.getRuntime().freeMemory() / 1024)); // freeMemory the unused memory in the allocation pool
		double usedMem = allocatedMem - cachedMem; // really used memory
		double useableMem = maxMem - usedMem; //allocated, but non-used and non-allocated memory

		StringBuilder list = new StringBuilder();
		String newline = System.getProperty("line.separator");

		list.append("AllowedMemory: ........... " + ((int) (maxMem)) + " KB" + newline);
		list.append("     Allocated: .......... " + ((int) (allocatedMem)) + " KB (" + (((double) (Math.round(allocatedMem / maxMem * 1000000))) / 10000) + "%)" + newline);
		list.append("     Non-Allocated: ...... " + ((int) (nonAllocatedMem)) + " KB (" + (((double) (Math.round(nonAllocatedMem / maxMem * 1000000))) / 10000) + "%)" + newline);
		list.append("AllocatedMemory: ......... " + ((int) (allocatedMem)) + " KB" + newline);
		list.append("     Used: ............... " + ((int) (usedMem)) + " KB (" + (((double) (Math.round(usedMem / maxMem * 1000000))) / 10000) + "%)" + newline);
		list.append("     Unused (cached): .... " + ((int) (cachedMem)) + " KB (" + (((double) (Math.round(cachedMem / maxMem * 1000000))) / 10000) + "%)" + newline);
		list.append("UseableMemory: ........... " + ((int) (useableMem)) + " KB (" + (((double) (Math.round(useableMem / maxMem * 1000000))) / 10000) + "%)" + newline);
		return list;
	}
}