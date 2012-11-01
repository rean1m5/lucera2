package ru.catssoftware.gameserver.util;

/*
 * @author Ro0TT
 * @date 31.10.2012
 */

public class Debug
{
	public static String getStackTrace()
	{
		String ret = "";
		Throwable t = new Throwable();
		t.fillInStackTrace();
		for(StackTraceElement e : t.getStackTrace() )
			if(!e.getClassName().startsWith("ru."))
				break;
			else if (!e.getClassName().startsWith("ru.catssoftware.gameserver.util.Debug"))
				if (e.getClassName().startsWith("ru.cats"))
					ret += e.getClassName().replace("ru.catssoftware", "") + ": " + e.getMethodName() + "\n";
				else
					ret += e.getClassName().replace("ru.ro0TT", "") + ": " + e.getMethodName() + "\n";
		return ret;
	}
}
