package ru.catssoftware.gameserver.model;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.instancemanager.BossSpawnManager;
import ru.catssoftware.gameserver.instancemanager.RaidPointsManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BossLair;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public abstract class L2Boss extends L2MonsterInstance
{
	private static final int	BOSS_MAINTENANCE_INTERVAL	= 10000;

	public static final int		BOSS_INTERACTION_DISTANCE	= 500;

	public L2Boss(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	private BossSpawnManager.StatusEnum	_raidStatus;

	public BossLair _lair;
	@Override
	protected int getMaintenanceInterval()
	{
		return BOSS_MAINTENANCE_INTERVAL;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		L2PcInstance player = killer.getPlayer();
		if (player != null)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
			if (player.getParty() != null)
			{
				for (L2PcInstance member : player.getParty().getPartyMembers())
					rewardRaidPoints(member);
			}
			else
				rewardRaidPoints(player);
		}
		if(_lair!=null)
			_lair.setUnspawn();
		return true;
	}
	
	private void rewardRaidPoints(L2PcInstance player)
	{
		int points = (getLevel() / 2) + Rnd.get(-5, 5);
		RaidPointsManager.addPoints(player, getNpcId(), points);
		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S1_RAID_POINTS);
		sm.addNumber(points);
		player.sendPacket(sm);
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	protected boolean canInteract(L2PcInstance player)
	{
		return isInsideRadius(player, BOSS_INTERACTION_DISTANCE, false, false);
	}

	public void setRaidStatus(BossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	public BossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	/**
	 * Спавн всех миньенов босса
	 * Если миньен не около босса, то возвращаем его к боссу
	 */
	@Override
	protected void manageMinions()
	{
		_minionList.spawnMinions();
		_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			public void run()
			{
				L2Spawn bossSpawn = getSpawn();
				if (!isInsideRadius(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), 5000, true, false))
				{
					teleToLocation(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), true);
					healFull();
				}
				_minionList.maintainMinions();
			}
		}, 60000, getMaintenanceInterval() + Rnd.get(5000));
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}

	public void healFull()
	{
		super.getStatus().setCurrentHp(super.getMaxHp());
		super.getStatus().setCurrentMp(super.getMaxMp());
	}

	@Override
	public boolean isBoss()
	{
		return true;
	}

	@Override
	public L2Boss getBoss()
	{
		return this;
	}
}
