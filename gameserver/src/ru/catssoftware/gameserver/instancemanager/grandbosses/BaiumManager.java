/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.instancemanager.grandbosses;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.pack.ai.Baium;
import ru.catssoftware.gameserver.network.serverpackets.Earthquake;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * This class ...
 * control for sequence of fight against Baium.
 * @version $Revision: $ $Date: $
 * @author  L2J_JP SANDMAN
 */
public class BaiumManager extends BossLair
{
	private static BaiumManager		_instance;

	public final static int			BAIUM_NPC				= 29025;
	public final static int			BAIUM					= 29020;
	public final static int			ARCHANGEL				= 29021;
	public final static int			TELEPORT_CUBE			= 29055;
	public final static int			STATUE_LOCATION[]		= { 116025, 17455, 10109, 40233};
	private L2Spawn					_statueSpawn			= null;
	public final static int			ANGEL_LOCATION[][]		=
	{
		{ 113004, 16209, 10076, 60242 },
		{ 114053, 16642, 10076, 4411 },
		{ 114563, 17184, 10076, 49241 },
		{ 116356, 16402, 10076, 31109 },
		{ 115015, 16393, 10076, 32760 },
		{ 115481, 15335, 10076, 16241 },
		{ 114680, 15407, 10051, 32485 },
		{ 114886, 14437, 10076, 16868 },
		{ 115391, 17593, 10076, 55346 },
		{ 115245, 17558, 10076, 35536 }
	};
	
	protected List<L2Spawn>			_angelSpawns			= new FastList<L2Spawn>();
	private L2NpcInstance			_statue;
	private L2GrandBossInstance		_baium;
	
	
	public final static int			CUBE_LOCATION[]			= { 115203, 16620, 10078, 0 };
	protected L2Spawn				_teleportCubeSpawn		= null;
	protected L2NpcInstance			_teleportCube			= null;
	protected L2NpcInstance			_npcBaium;
	protected Map<Integer, L2Spawn>	_monsterSpawn			= new FastMap<Integer, L2Spawn>();
	protected List<L2NpcInstance>	_monsters				= new FastList<L2NpcInstance>();

	protected ScheduledFuture<?>	_activityTimeEndTask	= null;
	protected String				_words					= "Don't obstruct my sleep! Die!";

	private long MIN_RESPAWN;
	private long MAX_RESPAWN;
	private long ACTIVITY_TIME;
	private long NO_ATTACK_TIME;
	public long _lastAttackTime;
	private boolean ENABLED;
	public BaiumManager()
	{
		super();
		_state = new GrandBossState(BAIUM);
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			ENABLED = Boolean.parseBoolean(p.getProperty("BaiumEnabled","true"));
			if(!ENABLED)
				return;
			
			MIN_RESPAWN = Integer.parseInt(p.getProperty("BaiumMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("BaiumMaxRespawn","2880"));
			ACTIVITY_TIME = Integer.parseInt(p.getProperty("BaiumActiveTime","50"));
			NO_ATTACK_TIME = Integer.parseInt(p.getProperty("BaiumNoAttackTime","20"))*60000;
		} catch(Exception e) {
			_log.error("BaiumManager: Error while reading config",e);
		}
	}

	private Future<?> _activityTask;
	private ExclusiveTask _baumCheskTask = new ExclusiveTask() {
		@Override
		protected void onElapsed() {
			if(_baium!=null && !_baium.isDead()) {
				if(System.currentTimeMillis()-_lastAttackTime > NO_ATTACK_TIME) {
					setUnspawn();
					return;
				}
				schedule(60000);
			}
		}
		
	};
	public static BaiumManager getInstance()
	{
		if (_instance == null)
			_instance = new BaiumManager();
		return _instance;
	}

	// initialize
	@Override
	public void init()
	{
		if(!ENABLED)
			return;

		// setting spawn data of monsters.
		new Baium();
		_questName = Baium.QUEST;
		try
		{
			L2NpcTemplate template1;

			//Statue of Baium  
			template1 = NpcTable.getInstance().getTemplate(BAIUM_NPC);
			_statueSpawn = new L2Spawn(template1);
			_statueSpawn.setAmount(1);
			_statueSpawn.setLocx(STATUE_LOCATION[0]);
			_statueSpawn.setLocy(STATUE_LOCATION[1]);
			_statueSpawn.setLocz(STATUE_LOCATION[2]);
			_statueSpawn.setHeading(STATUE_LOCATION[3]);

			// Baium.
			template1 = NpcTable.getInstance().getTemplate(BAIUM);
			_bossSpawn = new L2Spawn(template1);
			_bossSpawn.setLocx(STATUE_LOCATION[0]);
			_bossSpawn.setLocy(STATUE_LOCATION[1]);
			_bossSpawn.setLocz(STATUE_LOCATION[2]);
			_bossSpawn.setHeading(STATUE_LOCATION[3]);
			
			
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}

		// setting spawn data of teleport cube.
		try
		{
			L2NpcTemplate Cube = NpcTable.getInstance().getTemplate(TELEPORT_CUBE);
			_teleportCubeSpawn = new L2Spawn(Cube);
			_teleportCubeSpawn.setAmount(1);
			_teleportCubeSpawn.setLocx(CUBE_LOCATION[0]);
			_teleportCubeSpawn.setLocy(CUBE_LOCATION[1]);
			_teleportCubeSpawn.setLocz(CUBE_LOCATION[2]);
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}

		// setting spawn data of archangels.
		try
		{
			L2NpcTemplate angel = NpcTable.getInstance().getTemplate(ARCHANGEL);
			L2Spawn spawnDat;
			FastList<Integer> random = new FastList<Integer>();
			for (int i = 0; i < 5; i++)
			{
				int r = -1;
				while (r == -1 || random.contains(r))
					r = Rnd.get(10);
				random.add(r);
			}

			for (int i : random)
			{
				spawnDat = new L2Spawn(angel);
				spawnDat.setAmount(1);
				spawnDat.setLocx(ANGEL_LOCATION[i][0]);
				spawnDat.setLocy(ANGEL_LOCATION[i][1]);
				spawnDat.setLocz(ANGEL_LOCATION[i][2]);
				spawnDat.setHeading(ANGEL_LOCATION[i][3]);
				_angelSpawns.add(spawnDat);
			}
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}
		switch (_state.getState()) {
		case DEAD:
        	long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
			break;
		case SLEEP:	
		case ALIVE:
		case NOTSPAWN:
		case UNKNOWN:
			_state.setState(StateEnum.NOTSPAWN);
			_statue = _statueSpawn.doSpawn();
			break;
		}
		if(_state.getState()==StateEnum.INTERVAL)
			setIntervalEndTask();
		_log.info("BaiumManager : State of Baium is " + _state.getState() + ".");
	}
	
	private L2PcInstance _waker;
	public void wakeBaium(L2PcInstance waker) {
		_waker = waker;
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				spawnBaium();
			}
		},2000);
	}

	protected void spawnArchangels()
	{
		for (L2Spawn spawn : _angelSpawns) {
			L2Attackable angel = (L2Attackable)spawn.doSpawn();
			angel.addDamageHate(_baium, 10, 2000);
			_monsters.add(angel);
		}
	}

	// Archangel ascension.
	public void deleteArchangels()
	{
		for(L2NpcInstance npc : _monsters)
			if(!npc.isDead())
				npc.deleteMe();
		_monsters.clear();
	}

	
	// do spawn Baium.
	private void spawnBaium()
	{

		if(_statue==null)
			return;
		_statue.deleteMe();
		_statue = null;
		_baium = (L2GrandBossInstance)_bossSpawn.doSpawn();
		_state.setState(StateEnum.ALIVE);
		_state.update();
		_baium.setIsImmobilized(true);
		_baium.setIsInSocialAction(true);
		_baium.setCanReturnToSpawnPoint(false);
		ThreadPoolManager.getInstance().scheduleGeneral(new Social( 2), 100);

		ThreadPoolManager.getInstance().scheduleGeneral(new Social(3), 15000);

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				Earthquake eq = new Earthquake(_baium.getX(), _baium.getY(), _baium.getZ(), 40, 5);
				_baium.broadcastPacket(eq);
			}
		}, 25000);

		ThreadPoolManager.getInstance().scheduleGeneral(new Social(1), 25000);

		ThreadPoolManager.getInstance().scheduleGeneral(new KillPc(),26000);

		ThreadPoolManager.getInstance().scheduleGeneral(new CallArchAngel(), 35000);

		ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(), 35500);

		L2CharPosition pos = new L2CharPosition(Rnd.get(112826, 116241), Rnd.get(15575, 16375), 10078, 0);
		ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(pos), 36000);

		// set delete task.
		_activityTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(), ACTIVITY_TIME * 60000);
		_lastAttackTime = System.currentTimeMillis();
		_baumCheskTask.schedule(60000);
		
	}

	// at end of activity time.
	private class ActivityTimeEnd implements Runnable
	{
		public void run()
		{
			setUnspawn();
		}
	}

	// clean Baium's lair.
	@Override
	public void setUnspawn()
	{
		if(_activityTask!=null) {
			_activityTask.cancel(true);
			_activityTask = null;
		}
		clearLair();
		deleteArchangels();
		if(_state.getState()!=StateEnum.DEAD) {
			if(_baium!=null) {
				_baium.deleteMe();
				_statue = _statueSpawn.doSpawn();
			}
			_state.setState(StateEnum.NOTSPAWN);
		} else {
			long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN)*60000;
			_state.setRespawnDate(interval);
			_state.setState(StateEnum.INTERVAL);
			setIntervalEndTask();
		}
		if(_teleportCube!=null) {
			_teleportCube.deleteMe();
			_teleportCube = null;
		}
		_baium = null;
		_state.update();
		_log.info("BaiumManager: State of Baium is "+_state.getState());
	}

	// do spawn teleport cube.
	public void spawnCube()
	{
		_state.setState(StateEnum.DEAD);
		_teleportCube = _teleportCubeSpawn.doSpawn();
	}

	public void setIntervalEndTask()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	// at end of interval.
	private class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(GrandBossState.StateEnum.NOTSPAWN);
			_state.update();
			_statue = _statueSpawn.doSpawn();
			_log.info("BaiumManager: State of Baium is "+_state.getState());
		}
	}

	// setting teleport cube spawn task.
	public void setCubeSpawn()
	{
		_state.setState(GrandBossState.StateEnum.DEAD);
		_state.update();

		deleteArchangels();

		ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 10000);
	}

	// do spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		public void run()
		{
			spawnCube();
		}
	}

	// do social.
	private class Social implements Runnable
	{
		private int				_action;

		public Social(int actionId)
		{
			_action = actionId;
		}

		public void run()
		{
			SocialAction sa = new SocialAction(_baium.getObjectId(), _action);
			_baium.broadcastPacket(sa);
		}
	}

	// action is enabled the boss.
	private class SetMobilised implements Runnable
	{
		public void run()
		{
			_baium.setIsImmobilized(false);
			_baium.setIsInSocialAction(false);

		}
	}

	// Move at random on after Baium appears.
	private class MoveAtRandom implements Runnable
	{
		private L2CharPosition	_pos;

		public MoveAtRandom(L2CharPosition pos)
		{
			_pos = pos;
		}

		public void run()
		{
			_baium.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _pos);
		}
	}

	private class CallArchAngel implements Runnable
	{
		public void run()
		{
			spawnArchangels();
		}
	}

	private class KillPc implements Runnable
	{
		public void run()
		{
			L2Skill skill = SkillTable.getInstance().getInfo(4136, 1);
			if (_waker != null && skill != null)
			{
				NpcSay say = new NpcSay(_baium.getObjectId(),1,_baium.getNpcId(),_words);
				_baium.broadcastPacket(say);
				_baium.setTarget(_waker);
				_baium.doCast(skill);
				ThreadPoolManager.getInstance().schedule(new Runnable() {
					public void run() {
						_waker.reduceCurrentHp(_waker.getCurrentHp()+1, _baium);
					}
				}, 2000);
				
			}
		}
	}



	@Override
	public void setRespawn() {
	}
	
	@Override
	public void onEnter(L2Character cha) {
		L2PcInstance pc = cha.getActingPlayer();
		if(pc!=null && !pc.isGM() && Config.EPIC_REQUIRE_QUEST)
			if(pc.getQuestState(_questName)==null)
				pc.teleToLocation(TeleportWhereType.Town);
	}
	
}