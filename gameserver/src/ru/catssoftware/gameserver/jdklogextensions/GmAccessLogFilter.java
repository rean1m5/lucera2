package ru.catssoftware.gameserver.jdklogextensions;

public final class GmAccessLogFilter extends L2LogFilter
{
	@Override
	protected String getLoggerName()
	{
		return "GmAccess";
	}
}
