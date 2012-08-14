package ru.catssoftware.gameserver.instancemanager.grandbosses;


import ru.catssoftware.config.L2Properties;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.pack.ai.QueenAnt;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class QueenAntManager extends BossLair {

	public static long MIN_RESPAWN;
	public static long MAX_RESPAWN;
	public static int SAFE_LEVEL;
	public static int MAX_NURSES;
	public static int MAX_GUARDS;
	public static boolean AK_ENABLE;
	private static QueenAntManager _instance;
	private boolean _loaded = false;
	public static QueenAntManager getInstance() {
		if(_instance==null)
			_instance = new QueenAntManager();
		return _instance;
	}
	
	private QueenAntManager() {
		super();
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			MIN_RESPAWN = Integer.parseInt(p.getProperty("QueenAntMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("QueenAntMaxRespawn","2880"));
			SAFE_LEVEL = Integer.parseInt(p.getProperty("QueenAntMaxSafeLevel","48"));
			MAX_NURSES = Integer.parseInt(p.getProperty("QueenAntNumberOfNurses","6"));
			MAX_GUARDS = Integer.parseInt(p.getProperty("QueenAntNumberOfGuards","8"));
			AK_ENABLE = Boolean.parseBoolean(p.getProperty("QueenAntEnabled","true"));
			
			if(AK_ENABLE) {
				_loaded = true;
				new QueenAnt();
			}
			GameExtensionManager.getInstance().registerExtension(new checkLevelUp());
			
		} catch(Exception e) {
			_log.error("QuuenAntManager: Error while reading config",e);
			_loaded = false;
			return;
		}
		
	}
	@Override
	public void init() {
		if(!_loaded)
			return;
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(29001);
		_bossSpawn =  new L2Spawn(template);
		_bossSpawn.setLocx(-21610);
		_bossSpawn.setLocy(181594);
		_bossSpawn.setLocz(-5734);
		_bossSpawn.stopRespawn();
		_state = new GrandBossState(29001);
		switch (_state.getState()) {
		case DEAD:
                        long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case INTERVAL:
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				public void run() {
					doSpawn();
				}
			}, _state.getInterval());
			break;
		case UNKNOWN:
			_state.setState(StateEnum.ALIVE);
		case ALIVE:
		case NOTSPAWN:
			doSpawn();
			break;
		}
		_log.info("QueenAntManager: State of Queen Ant is "+_state.getState());
			
	}
	
	private void doSpawn() {
		L2GrandBossInstance ak = (L2GrandBossInstance)_bossSpawn.doSpawn();
		ak._lair = this;
		_state.setState(StateEnum.ALIVE);
		_state.update();
	}

	@Override
	public void setRespawn() {
	}

	@Override
	public void setUnspawn() {
		_state.setState(StateEnum.INTERVAL);
		long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN) * 60000;
		_state.setRespawnDate(interval);
		_state.update();
		_log.info("QueenAntManager: State of Queen Ant is "+_state.getState());
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				doSpawn();
			}
		}, _state.getInterval());
	}

	@Override
	public void onExit(L2Character cha) {
		if(cha instanceof L2Attackable) {
			int npcId = ((L2Attackable)cha).getNpcId();
			if(npcId == QueenAnt.GUARD || npcId == QueenAnt.NURSE || npcId == QueenAnt.ROYAL || npcId==QueenAnt.QUEEN)
				cha.teleToLocation(_bossSpawn.getLocx(), _bossSpawn.getLocy(), _bossSpawn.getLocz());
		}
	}

	private class checkLevelUp extends ObjectExtension
	{

		@Override
		public Class<?>[] forClasses() {
			return new Class<?>[]{ L2PcInstance.class };
		}

		@Override
		public Object hanlde(Object object, Action action, Object... params)
		{
			if (action.equals(Action.PC_LEVEL_UP) && object instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) object;
				if (player.isInsideZone(L2Zone.FLAG_QUEEN))
					if (SAFE_LEVEL > 0 && player.getLevel() > SAFE_LEVEL)
						player.teleToLocation(TeleportWhereType.Town);
			}

			return null;
		}
	}

}
