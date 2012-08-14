package ru.catssoftware.lang;

import org.apache.log4j.Logger;

public abstract class RunnableImpl implements Runnable
{
	public static final Logger _log = Logger.getLogger(RunnableImpl.class);

	public abstract void runImpl() throws Exception;

	@Override
	public final void run()
	{
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.error("Exception: RunnableImpl.run(): " + e, e);
		}
	}
}