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
package ru.catssoftware.lang;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;


/**
 * @author NB4L1
 */
public abstract class L2Thread extends Thread
{
	private static final Logger _log = Logger.getLogger(L2Thread.class);
	
	protected L2Thread()
	{
		super();
	}
	
	protected L2Thread(String name)
	{
		super(name);
	}
	
	private volatile boolean _isAlive = true;
	
	public final void shutdown()
	{
		onShutdown();
		
		_isAlive = false;
	}
	
	protected void onShutdown()
	{
	}
	
	@Override
	public final void run()
	{
		try
		{
			while (_isAlive)
			{
				runTurn();
			}
		}
		finally
		{
			onFinally();
		}
	}
	
	protected abstract void runTurn();
	
	protected void onFinally()
	{
	}
	
	public static List<String> getStats(Thread t)
	{
		List<String> list = new FastList<String>();
		
		list.add(t.toString() + " - ID: " + t.getId());
		list.add(" * State: " + t.getState());
		list.add(" * Alive: " + t.isAlive());
		list.add(" * Daemon: " + t.isDaemon());
		list.add(" * Interrupted: " + t.isInterrupted());
		for (ThreadInfo info : ManagementFactory.getThreadMXBean().getThreadInfo(new long[] { t.getId() }, true, true))
		{
			for (MonitorInfo monitorInfo : info.getLockedMonitors())
			{
				list.add("==========");
				list.add(" * Locked monitor: " + monitorInfo);
				list.add("\t[" + monitorInfo.getLockedStackDepth() + ".]: at " + monitorInfo.getLockedStackFrame());
			}
			
			for (LockInfo lockInfo : info.getLockedSynchronizers())
			{
				list.add("==========");
				list.add(" * Locked synchronizer: " + lockInfo);
			}
			
			list.add("==========");
			for (StackTraceElement trace : info.getStackTrace())
				list.add("\tat " + trace);
		}
		
		return list;
	}
	
	public static List<String> getStats()
	{
		List<String> list = new FastList<String>();
		
		list.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date()));
		list.add("");
		list.add("## Java Platform Information ##");
		list.add("Java Runtime Name: " + System.getProperty("java.runtime.name"));
		list.add("Java Version: " + System.getProperty("java.version"));
		list.add("Java Class Version: " + System.getProperty("java.class.version"));
		list.add("");
		list.add("## Virtual Machine Information ##");
		list.add("VM Name: " + System.getProperty("java.vm.name"));
		list.add("VM Version: " + System.getProperty("java.vm.version"));
		list.add("VM Vendor: " + System.getProperty("java.vm.vendor"));
		list.add("VM Info: " + System.getProperty("java.vm.info"));
		list.add("");
		list.add("## OS Information ##");
		list.add("Name: " + System.getProperty("os.name"));
		list.add("Architeture: " + System.getProperty("os.arch"));
		list.add("Version: " + System.getProperty("os.version"));
		list.add("");
		list.add("## Runtime Information ##");
		list.add("CPU Count: " + Runtime.getRuntime().availableProcessors());
		list.add("");
		for (String line : getMemoryUsageStatistics())
			list.add(line);
		list.add("");
		list.add("## Class Path Information ##\n");
		for (String lib : System.getProperty("java.class.path").split(File.pathSeparator))
			if (!list.contains(lib))
				list.add(lib);
		list.add("");
		
		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		list.add("## " + threads.size() + " thread(s) ##");
		list.add("=================================================");
		
		int i = 1;
		for (Thread thread : threads)
		{
			list.add("");
			list.add(i++ + ".");
			list.addAll(getStats(thread));
		}
		
		return list;
	}
	
	public static void dumpThreads()
	{
		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter("Thread-" + System.currentTimeMillis() + ".log"), true);
			
			for (String line : getStats())
				pw.println(line);
		}
		catch (Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			IOUtils.closeQuietly(pw);
		}
	}
	
	public static String[] getMemoryUsageStatistics()
	{
		double max = Runtime.getRuntime().maxMemory() / 1024; // maxMemory is the upper limit the jvm can use
		double allocated = Runtime.getRuntime().totalMemory() / 1024; //totalMemory the size of the current allocation pool
		double nonAllocated = max - allocated; //non allocated memory till jvm limit
		double cached = Runtime.getRuntime().freeMemory() / 1024; // freeMemory the unused memory in the allocation pool
		double used = allocated - cached; // really used memory
		double useable = max - used; //allocated, but non-used and non-allocated memory
		
		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
		DecimalFormat df = new DecimalFormat(" (0.0000'%')");
		DecimalFormat df2 = new DecimalFormat(" # 'KB'");
		return new String[] {
				"+----",
				"| Global Memory Informations at " + sdf.format(new Date()) + ":",
				"|    |",
				"| Allowed Memory:" + df2.format(max),
				"|    |= Allocated Memory:" + df2.format(allocated) + df.format(allocated / max * 100),
				"|    |= Non-Allocated Memory:" + df2.format(nonAllocated) + df.format(nonAllocated / max * 100),
				"| Allocated Memory:" + df2.format(allocated),
				"|    |= Used Memory:" + df2.format(used) + df.format(used / max * 100),
				"|    |= Unused (cached) Memory:" + df2.format(cached) + df.format(cached / max * 100),
				"| Useable Memory:" + df2.format(useable) + df.format(useable / max * 100),
				"+----" };
	}
}
