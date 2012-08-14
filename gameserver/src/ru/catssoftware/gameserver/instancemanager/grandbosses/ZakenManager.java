package ru.catssoftware.gameserver.instancemanager.grandbosses;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

import java.util.List;
import java.util.concurrent.Future;


public class ZakenManager extends BossLair
{
	protected final static Logger _log	= Logger.getLogger(ZakenManager.class.getName());	
	public static long MIN_RESPAWN;
	public static long MAX_RESPAWN;
	public static int MAX_LVL;
	public static boolean isLoaded = false;
	public static List<Integer> OPEN_DOOR_TIME = new FastList<Integer>();;
	public static int DOOR_OPENED;
	private static ZakenManager	_instance;
	private GrandBossState	_state;	
	private L2GrandBossInstance _zaken;
	private boolean ENABLED;

	public static ZakenManager getInstance()
	{
		if (_instance == null)
			_instance = new ZakenManager();
		return _instance;
	}

	public ZakenManager()
	{
		super();
		_state = new GrandBossState(29022);
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			ENABLED = Boolean.parseBoolean(p.getProperty("ZakenEnabled","true"));
			if(!ENABLED)
				return;
			
			MIN_RESPAWN = Integer.parseInt(p.getProperty("ZakenMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("ZakenMaxRespawn","2880"));
			DOOR_OPENED =Integer.parseInt(p.getProperty("ZakenDoorOpenTime","5"));
			MAX_LVL = Integer.parseInt(p.getProperty("ZakenMaxLevelInZone","69"));
			GameExtensionManager.getInstance().registerExtension(new checkLevelUp());
					
			for(String s : p.getProperty("ZakenDoorOpenHour","0").split(" ")) try {
				OPEN_DOOR_TIME.add(Integer.parseInt(s));
			} catch(NumberFormatException e) {}
			isLoaded = true;
		} catch(Exception e) {
			MIN_RESPAWN  = 1440;
			MAX_RESPAWN = 2880;
		}
	}
	public void init()
	{
		
		if(!ENABLED)
			return;
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(29022);
		_bossSpawn = new L2Spawn(template);
		_bossSpawn.setLocx(55312);
		_bossSpawn.setLocy(219168);
		_bossSpawn.setLocz(-3223);
		switch (_state.getState()) {
		case DEAD:
                        long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case SLEEP:	
		case INTERVAL:
			ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
			break;
		case UNKNOWN:
			_state.setState(StateEnum.ALIVE);
		case ALIVE:
		case NOTSPAWN:
			_state.setState(StateEnum.NOTSPAWN);
			spawnZaken();
			break;
			
		}
		_log.info("ZakenManager: State of Zaken is " + _state.getState());
				
	}

	private class IntervalEnd implements Runnable
	{
		public void run()
		{
			_log.info("ZakenManager : State of Zaken is " + _state.getState() + ".");			
			spawnZaken();
		}
	}

	private class RunAway implements Runnable
	{
		public void run()
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					if (_zaken!=null && _zaken.getCurrentMp()>200)
						if(!_zaken.isDead()) 
							_zaken.doCast(SkillTable.getInstance().getInfo(4222, 1));
						else
							_runAway.cancel(false);
					else
						_runAway.cancel(false);
				}
			}, Rnd.get(5)*60000);
		}
	}	


	private Future<?> _runAway;
	private void spawnZaken()
	{
		_zaken = (L2GrandBossInstance)_bossSpawn.doSpawn();
		_zaken._lair = this;
		_zaken.getStatus().setCurrentHpMp(_zaken.getMaxHp(), _zaken.getMaxMp());
		_state.setState(GrandBossState.StateEnum.ALIVE);
		_state.update();
		_runAway  = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RunAway(), 20*60000, 20*60000);
		
	}


	@Override
	public void setUnspawn() {
		_state.setState(StateEnum.INTERVAL);
		long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN) * 60000;
		_state.setRespawnDate(interval);
		_state.update();
		_zaken=null;
		_log.info("ZakenManager : State of Zaken is " + _state.getState() + ".");		
		ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());		
	}

	@Override
	public void setRespawn() {
	}
	
	@Override
	public void onEnter(L2Character cha) {
		if(cha.getActingPlayer()!=null)
			if(MAX_LVL >0 && cha.getLevel()>MAX_LVL && !cha.getActingPlayer().isGM())
				cha.teleToLocation(TeleportWhereType.Town);
			
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
				if (player.isInsideZone(L2Zone.FLAG_ZAKEN))
					if (MAX_LVL > 0 && player.getLevel() > MAX_LVL)
						player.teleToLocation(TeleportWhereType.Town);
			}

			return null;
		}
	}
}