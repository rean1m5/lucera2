package ru.catssoftware.gameserver;

import ru.catssoftware.gameserver.model.L2Clan;

public class FortUpdater implements Runnable
{
	@SuppressWarnings("unused")
	private L2Clan			_clan;
	@SuppressWarnings("unused")
	private int				_runCount	= 0;

	public FortUpdater(L2Clan clan, int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}

	public void run()
	{
		try
		{
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}