package ru.catssoftware.gameserver.instancemanager.grandbosses;


import java.util.List;


import javolution.util.FastList;

import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Entity;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.QuestState;

public abstract class BossLair extends Entity
{
	protected final static Logger _log	= Logger.getLogger(BossLair.class.getName());
	private  static List<BossLair> _lairs = new FastList<BossLair>();
	protected GrandBossState	_state;
	protected String			_questName;
	
	public L2Spawn _bossSpawn;
	public abstract void init();

	public abstract void setUnspawn();

	public BossLair() {
		_lairs.add(this);
	}
	
	public static List<BossLair> getLairs() {
		return _lairs;
	}
	public GrandBossState.StateEnum getState()
	{
		if(_state==null)
			return StateEnum.UNKNOWN;
		return _state.getState();
	}

	public abstract void setRespawn();
	
	public boolean isEnableEnterToLair()
	{
		return _state.getState() == GrandBossState.StateEnum.NOTSPAWN;
	}

	public void onEnter(L2Character cha) {
		
	}

	public void onExit(L2Character cha) {
		
	}
	
	public synchronized boolean isPlayersAnnihilated()
	{
		for (L2PcInstance pc : getPlayersInside())
		{
			if (!pc.isDead())
				return false;
		}
		return true;
	}

	public void checkAnnihilated()
	{
		if (isPlayersAnnihilated())
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					setUnspawn();
				}
			}, 5000);
		}
	}

	public void clearLair() {
		for(L2PcInstance pc : getPlayersInside()) {
			if(_questName!=null) {
				QuestState qs  = pc.getQuestState(_questName);
				if(qs!=null)
					qs.exitQuest(true);
			}
			pc.teleToLocation(TeleportWhereType.Town);
		}
	}
}
