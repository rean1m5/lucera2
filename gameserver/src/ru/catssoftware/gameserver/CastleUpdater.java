package ru.catssoftware.gameserver;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.itemcontainer.ItemContainer;

/**
 * Thorgrim - 2005 Class managing periodical events with castle
 */
public class CastleUpdater implements Runnable
{
	private L2Clan				_clan;
	private int					_runCount	= 0;

	public CastleUpdater(L2Clan clan, int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}

	public void run()
	{
		try
		{
			// Move current castle treasury to clan warehouse every 2 hour
			ItemContainer warehouse = _clan.getWarehouse();
			if (warehouse != null && _clan.getHasCastle() > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(_clan.getHasCastle());
				if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					if (_runCount % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0)
					{
						castle.saveSeedData();
						castle.saveCropData();
					}
				}
				CastleUpdater cu = new CastleUpdater(_clan, ++_runCount);
				ThreadPoolManager.getInstance().scheduleGeneral(cu, 3600000);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}