package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.instancemanager.RaidBossSpawnManager;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2RaidBossInstance extends L2Boss
{
	private static final class RaidBossReturnHomeManager extends AbstractIterativePeriodicTaskManager<L2RaidBossInstance>
	{
		private static final RaidBossReturnHomeManager _instance = new RaidBossReturnHomeManager();

		private static RaidBossReturnHomeManager getInstance()
		{
			return _instance;
		}

		private RaidBossReturnHomeManager()
		{
			super(5000);
		}

		@Override
		protected void callTask(L2RaidBossInstance task)
		{
			task.returnHome();
		}

		@Override
		protected String getCalledMethodName()
		{
			return "returnHome()";
		}
	}

	public L2RaidBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		RaidBossReturnHomeManager.getInstance().startTask(this);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		RaidBossSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}

	@Override
	public void onSpawn()
	{
		setIsRaid(true);
		setIsBoss(true);
		super.onSpawn();
	}

	private boolean _canReturnHome  = true;
	public void setCanReturnHome(boolean val) {
		_canReturnHome = val;
	}

	@Override
	public void returnHome()
	{
		if (_canReturnHome && getSpawn() != null)
		{
			int zoneSize = getSpawn().getSpawnZoneSize();
			if(zoneSize > 0)
			{
				if (!isDead())
				{
					if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 1000, false))
					{
						clearAggroList();
						healFull();
						teleToLocation(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz());
						if (hasMinions())
							callMinions(true);
					}
				}
			}
			else if (RaidBossReturnHomeManager.getInstance().hasTask(this))
				RaidBossReturnHomeManager.getInstance().stopTask(this);
		}
		else if (RaidBossReturnHomeManager.getInstance().hasTask(this))
			RaidBossReturnHomeManager.getInstance().stopTask(this);
	}

	@Override
	public L2RaidBossInstance getBoss()
	{
		return this;
	}

	@Override
	public boolean isBoss()
	{
		return true;
	}
}