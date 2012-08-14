package ru.catssoftware.gameserver;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.mmocore.ReceivablePacket;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.threadmanager.L2ThreadFactory;
import ru.catssoftware.gameserver.threadmanager.ScheduledFutureWrapper;
import ru.catssoftware.lang.RunnableImpl;
import ru.catssoftware.util.concurrent.L2RejectedExecutionHandler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public final class ThreadPoolManager
{
	private static final long		MAX_DELAY						= TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
	private static final			ThreadPoolManager _instance		= new ThreadPoolManager();

	public static ThreadPoolManager getInstance()
	{
		return _instance;
	}
	
	private final ScheduledThreadPoolExecutor _general = new ScheduledThreadPoolExecutor(Config.GENERAL_THREAD_POOL_SIZE, new L2ThreadFactory("ThreadPool(General)", Thread.NORM_PRIORITY), new L2RejectedExecutionHandler());
	
	private final ScheduledThreadPoolExecutor _moving = new ScheduledThreadPoolExecutor(Config.GENERAL_THREAD_POOL_SIZE, new L2ThreadFactory("ThreadPool(Moving)", Thread.NORM_PRIORITY), new L2RejectedExecutionHandler());

	private final ScheduledThreadPoolExecutor _effect = new ScheduledThreadPoolExecutor(Config.EFFECT_THREAD_POOL_SIZE, new L2ThreadFactory("ThreadPool(Effect)", Thread.NORM_PRIORITY+1), new L2RejectedExecutionHandler());
	
	private final ScheduledThreadPoolExecutor _ai = new ScheduledThreadPoolExecutor(Config.AI_THREAD_POOL_SIZE, new L2ThreadFactory("ThreadPool(AI)", Thread.NORM_PRIORITY), new L2RejectedExecutionHandler());

	private final ScheduledThreadPoolExecutor _pcai = new ScheduledThreadPoolExecutor(Config.AI_THREAD_POOL_SIZE, new L2ThreadFactory("ThreadPool(PCAI)", Thread.NORM_PRIORITY+3), new L2RejectedExecutionHandler());
	private final ScheduledThreadPoolExecutor _lsgs = new ScheduledThreadPoolExecutor(10, new L2ThreadFactory("ThreadPool(LSGS)", Thread.NORM_PRIORITY), new L2RejectedExecutionHandler());
	
	private final ThreadPoolExecutor _packet = new ThreadPoolExecutor(Config.PACKET_THREAD_POOL_SIZE, Config.PACKET_THREAD_POOL_SIZE, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new L2ThreadFactory("Executor(Packet)", Thread.MAX_PRIORITY), new L2RejectedExecutionHandler()); 
	
	private ThreadPoolManager()
	{
	}
	
	public final void startPurgeTask(long period)
	{
		scheduleGeneralAtFixedRate(new PurgeTask(), period, period);
	}

	private final class PurgeTask extends RunnableImpl
	{
		public void runImpl()
		{
			purge();
		}
	}

	private final long validate(long delay)
	{
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}

	public ScheduledFuture<?> schedule(Runnable r, long delay)
	{
		
		return new ScheduledFutureWrapper(_general.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
	}

	public void executeLSGSPacket(Runnable r) {
		_lsgs.execute(r);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period)
	{
		return new ScheduledFutureWrapper(_general.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
	}

	public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay)
	{
		if(delay<= 0 ) {
			_general.execute(r);
			return null;
		}
		return new ScheduledFutureWrapper(_general.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
	}

	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r, long delay, long period)
	{
		return new ScheduledFutureWrapper(_general.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
	}

	public void executeGeneral(Runnable r)
	{
		_general.execute(r);
	}

	public ScheduledFuture<?> scheduleEffect(Runnable r, long delay)
	{
		return new ScheduledFutureWrapper(_effect.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
	}

	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long delay, long period)
	{
		return new ScheduledFutureWrapper(_effect.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
	}

	public void executeEffect(Runnable r)
	{
		_effect.execute(r);
	}

	public ScheduledFuture<?> scheduleAi(Runnable r, long delay, boolean pc)
	{
		if(pc)
			return new ScheduledFutureWrapper(_pcai.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
		return new ScheduledFutureWrapper(_ai.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
	}

	public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable r, long delay, long period, boolean pc)
	{
		if (pc)
			return new ScheduledFutureWrapper(_pcai.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
		return new ScheduledFutureWrapper(_ai.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
	}

	public void executeAi(Runnable r)
	{
		_ai.execute(r);
	}

	public void executePacket(Runnable r)
	{
		_packet.execute(r);
	}

	public CharSequence getStats()
	{
		StringBuilder list = new StringBuilder();
		
		String newline = System.getProperty("line.separator");
		
		list.append("ThreadPool (General)" + newline);
		list.append("=================================================" + newline);
		list.append("\tgetActiveCount: ...... " + _general.getActiveCount() + newline);
		list.append("\tgetCorePoolSize: ..... " + _general.getCorePoolSize() + newline);
		list.append("\tgetPoolSize: ......... " + _general.getPoolSize() + newline);
		list.append("\tgetLargestPoolSize: .. " + _general.getLargestPoolSize() + newline);
		list.append("\tgetMaximumPoolSize: .. " + _general.getMaximumPoolSize() + newline);
		list.append("\tgetCompletedTaskCount: " + _general.getCompletedTaskCount() + newline);
		list.append("\tgetQueuedTaskCount: .. " + _general.getQueue().size() + newline);
		list.append("\tgetTaskCount: ........ " + _general.getTaskCount() + newline);
		list.append(newline);
		list.append("ThreadPool (Effects)" + newline);
		list.append("=================================================" + newline);
		list.append("\tgetActiveCount: ...... " + _effect.getActiveCount() + newline);
		list.append("\tgetCorePoolSize: ..... " + _effect.getCorePoolSize()+ newline);
		list.append("\tgetPoolSize: ......... " + _effect.getPoolSize() + newline);
		list.append("\tgetLargestPoolSize: .. " + _effect.getLargestPoolSize() + newline);
		list.append("\tgetMaximumPoolSize: .. " + _effect.getMaximumPoolSize() + newline);
		list.append("\tgetCompletedTaskCount: " + _effect.getCompletedTaskCount() + newline);
		list.append("\tgetQueuedTaskCount: .. " + _effect.getQueue().size() + newline);
		list.append("\tgetTaskCount: ........ " + _effect.getTaskCount() + newline);
		list.append(newline);
		list.append("ThreadPool (AI)" + newline);
		list.append("=================================================" + newline);
		list.append("\tgetActiveCount: ...... " + _ai.getActiveCount() + newline);
		list.append("\tgetCorePoolSize: ..... " + _ai.getCorePoolSize() + newline);
		list.append("\tgetPoolSize: ......... " + _ai.getPoolSize() + newline);
		list.append("\tgetLargestPoolSize: .. " + _ai.getLargestPoolSize() + newline);
		list.append("\tgetMaximumPoolSize: .. " + _ai.getMaximumPoolSize() + newline);
		list.append("\tgetCompletedTaskCount: " + _ai.getCompletedTaskCount() + newline);
		list.append("\tgetQueuedTaskCount: .. " + _ai.getQueue().size() + newline);
		list.append("\tgetTaskCount: ........ " + _ai.getTaskCount() + newline);
		list.append(newline);
		list.append("ThreadPoolExecutor (Packet)" + newline);
		
		return list;
	}
		
	private boolean _inshutdown = false; 
	public void shutdown()
	{
		_inshutdown = true;
		_general.shutdown();
		_effect.shutdown();
		_ai.shutdown();
		_pcai.shutdown();
		_moving.shutdown();
		_lsgs.shutdown();
		_packet.shutdown();
		System.out.println("ThreadPoolManager: all threads stopped");
		
	}
	
	public void purge()
	{
		_general.purge();
		_effect.purge();
		_ai.purge();
		_pcai.purge();
		_moving.purge();
		_packet.purge();
	}

	public ScheduledFuture<?> scheduleMove(Runnable r, long delay) {
		return new ScheduledFutureWrapper(_moving.schedule(r, validate(delay), TimeUnit.MILLISECONDS));
	}

	public ScheduledFuture<?> scheduleMoveAtFixedRate(
			Runnable r, long delay,
			long period) {
		return new ScheduledFutureWrapper(_general.scheduleAtFixedRate(r, validate(delay), validate(period), TimeUnit.MILLISECONDS));
	}
	public static class PriorityThreadFactory implements ThreadFactory
	{
		private int _prio;
		private String _name;
		private AtomicInteger _threadNumber = new AtomicInteger(1);
		private ThreadGroup _group;

		public PriorityThreadFactory(String name, int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(_group, r);
			t.setName(_name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}

		public ThreadGroup getGroup()
		{
			return _group;
		}
	}

	public void executeIOPacket(ReceivablePacket<L2GameClient> r) {
		executePacket(r);
		
	}

	public boolean isShutdown() {
		return _inshutdown;
	}
	
}