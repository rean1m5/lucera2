package ru.catssoftware.gameserver.jdklogextensions;

public final class BanLogFilter extends L2LogFilter
{
	@Override
	protected String getLoggerName()
	{
		return "ban";
	}
}
