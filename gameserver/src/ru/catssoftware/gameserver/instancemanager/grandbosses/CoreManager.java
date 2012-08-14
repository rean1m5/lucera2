package ru.catssoftware.gameserver.instancemanager.grandbosses;

import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.quest.pack.ai.Core;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class CoreManager extends BossLair {

	public static long MIN_RESPAWN;
	public static long MAX_RESPAWN;
	public static int MAX_GUARDS;
	private boolean _loaded;
	private boolean ENABLED;
	private static CoreManager _instance;
	public static CoreManager getInstance() {
		if(_instance == null)
			_instance = new CoreManager();
		return _instance;
	}
	private CoreManager() {
		_state = new GrandBossState(29006);
		_state.load();
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			ENABLED = Boolean.parseBoolean(p.getProperty("CoreEnabled","true"));
			if(!ENABLED)
				return;
			MIN_RESPAWN = Integer.parseInt(p.getProperty("CoreMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("CoreMaxRespawn","2880"));
			MAX_GUARDS = Integer.parseInt(p.getProperty("CoreNumberOfGuards","4"));
			
			if (MAX_GUARDS<2)
				MAX_GUARDS=2;
			_loaded = true;
		} catch(Exception e) {
			_log.error("CoreManager: Error while reading config",e);
			_loaded = false;
			return;
		}
		
	}
	@Override
	public void init() {
		if(!ENABLED)
			return;
		if(!_loaded)
			return;
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(29006);
		_bossSpawn = new L2Spawn(template);
		_bossSpawn.setLocx(17726);
		_bossSpawn.setLocy(108915);
		_bossSpawn.setLocz(-6480);
		new Core();
		switch (_state.getState()) {
		case DEAD:
                        long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case SLEEP:	
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
		_log.info("CoreManager: State of Core is "+_state.getState());
	}

	private void doSpawn() {
		L2Boss core = (L2Boss)_bossSpawn.doSpawn();
		core._lair = this;
		_state.setState(StateEnum.ALIVE);
		_state.update();
	}
	@Override
	public void setRespawn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setUnspawn() {
		_state.setState(StateEnum.INTERVAL);
		long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN) * 60000;
		_state.setRespawnDate(interval);
		_state.update();
		_log.info("CoreManager: State of Core is "+_state.getState());
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				doSpawn();
			}
		}, _state.getInterval());
	}

}
