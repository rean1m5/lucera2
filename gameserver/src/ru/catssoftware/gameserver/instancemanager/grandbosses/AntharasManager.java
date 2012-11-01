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
import ru.catssoftware.Config;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.pack.ai.Antharas;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

import java.util.List;
import java.util.concurrent.ScheduledFuture;



/**
 *
 * This class ...
 * control for sequence of fight against Antharas.
 * @version $Revision: $ $Date: $
 * @author  L2J_JP SANDMAN
 */
public class AntharasManager extends BossLair
{
	private static AntharasManager	_instance;

	// location of teleport cube.
	private L2Spawn					_weakAntharas;
	private L2Spawn					_middleAntharas;
	private L2Spawn					_strongAntharas;
	private L2Spawn					_cubeSpawn;
	private L2NpcInstance			_cube;
	protected ScheduledFuture<?>	_cubeSpawnTask				= null;
	protected ScheduledFuture<?>	_monsterSpawnTask			= null;
	protected ScheduledFuture<?>	_intervalEndTask			= null;
	protected ScheduledFuture<?>	_activityTimeEndTask		= null;
	protected ScheduledFuture<?>	_socialTask					= null;
	protected ScheduledFuture<?>	_mobiliseTask				= null;
	protected ScheduledFuture<?>	_behemothSpawnTask			= null;
	protected ScheduledFuture<?>	_selfDestructionTask		= null;
	protected ScheduledFuture<?>	_moveAtRandomTask			= null;
	protected ScheduledFuture<?>	_movieTask					= null;

	private long MIN_RESPAWN;
	private long MAX_RESPAWN;
	private long ACTIVITY_TIME;
	private long ARRIVED_TIME;
	private int WEAK_ANTHARAS;
	private int MIDDLE_ANTHARAS;
	private long MIN_SLEEP;
	private long MAX_SLEEP;
	private long BEHEMOTH;
	private int BEHEMOTH_QTTY;
	private boolean ENABLED;
	
	private L2GrandBossInstance	_antharas	= null;
	private List<L2NpcInstance> _monsters = new FastList<L2NpcInstance>();
	public AntharasManager()
	{
		_state = new GrandBossState(29019);
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			ENABLED = Boolean.parseBoolean(p.getProperty("AntharasEnabled","true"));
			if(!ENABLED)
				return;
			MIN_RESPAWN = Integer.parseInt(p.getProperty("AntharasMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("AntharasMaxRespawn","2880"));
			ACTIVITY_TIME = Integer.parseInt(p.getProperty("AntharasActiveTime","50"));
			ARRIVED_TIME  = Integer.parseInt(p.getProperty("AntharasArrivedTime","5"));
			WEAK_ANTHARAS = Integer.parseInt(p.getProperty("AntharasWeakPlayers","20"));
			MIDDLE_ANTHARAS = Integer.parseInt(p.getProperty("AntharasMiddlePlayers","50"));
			MIN_SLEEP = Integer.parseInt(p.getProperty("AntharasMinSleepTime","120"));
			MAX_SLEEP = Integer.parseInt(p.getProperty("AntharasMaxSleepTime","240"));
			BEHEMOTH = Integer.parseInt(p.getProperty("AntharasIntervalOfBehemoth","5"));
			
		} catch(Exception e) {
			_log.error("AntharasManager: Error while reading config",e);
			return;
		}
		
	}

	public static AntharasManager getInstance()
	{
		if (_instance == null)
			_instance = new AntharasManager();
		return _instance;
	}

	// initialize
	@Override
	public void init()
	{
		if(!ENABLED)
			return;
		// setting spawn data of monsters.
		new Antharas();
		_questName = Antharas.QUEST;
		try
		{
			L2NpcTemplate template1;

			template1 = NpcTable.getInstance().getTemplate(29066);
			_weakAntharas = new L2Spawn(template1);
			_weakAntharas.setLocx(181323);
			_weakAntharas.setLocy(114850);
			_weakAntharas.setLocz(-7623);
			_weakAntharas.setHeading(32542);

			// normal Antharas.
			template1 = NpcTable.getInstance().getTemplate(29067);
			_middleAntharas = new L2Spawn(template1);
			_middleAntharas.setLocx(181323);
			_middleAntharas.setLocy(114850);
			_middleAntharas.setLocz(-7623);
			_middleAntharas.setHeading(32542);

			// strong Antharas.
			template1 = NpcTable.getInstance().getTemplate(29068);
			_strongAntharas = new L2Spawn(template1);
			_strongAntharas.setLocx(181323);
			_strongAntharas.setLocy(114850);
			_strongAntharas.setLocz(-7623);
			_strongAntharas.setHeading(32542);
			_bossSpawn = _strongAntharas;
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}

		try
		{
			L2NpcTemplate Cube = NpcTable.getInstance().getTemplate(31859);
			_cubeSpawn = new L2Spawn(Cube);
			_cubeSpawn.setLocx(177615);
			_cubeSpawn.setLocy(114941);
			_cubeSpawn.setLocz(-7709);
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}
		if (_state.getState().equals(GrandBossState.StateEnum.ALIVE))
		{
			_state.setState(GrandBossState.StateEnum.NOTSPAWN);
			_state.update();
		}

		switch (_state.getState()) {
		case DEAD:
        	long inter = Rnd.get(MIN_RESPAWN, MAX_RESPAWN) * 60000;
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case SLEEP:	
		case INTERVAL:
			setIntervalEndTask();
			break;
		default:
			_state.setState(StateEnum.NOTSPAWN);
			break;
		}
		_log.info("AntharasManager: State of Antharas is " + _state.getState() + ".");

	}

	// do spawn teleport cube.
	public void spawnCube()
	{
		_antharas = null;
		_state.setState(StateEnum.DEAD);
		_state.update();
		_cube = _cubeSpawn.doSpawn();
	}

	public void onKill(L2NpcInstance mob) {
		_monsters.remove(mob);
	}
	// setting Antharas spawn task.
	public void setAntharasSpawnTask()
	{
		if (_monsterSpawnTask == null)
		{
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(1), ARRIVED_TIME*60000);
		}
	}

	// do spawn Antharas.
	private class AntharasSpawn implements Runnable
	{
		private int					_distance	= 6502500;
		private int					_taskId		= 0;

		AntharasSpawn(int taskId)
		{
			_taskId = taskId;
		}

		public void run()
		{
			SocialAction sa = null;
			_socialTask = null;
			_monsterSpawnTask = null;
			switch (_taskId)
			{
			case 1: // spawn.
				_state.setState(StateEnum.ALIVE);
				_state.update();
				if (getPlayersInside().size() <= WEAK_ANTHARAS) {
					_antharas = (L2GrandBossInstance)_weakAntharas.doSpawn();
					BEHEMOTH_QTTY = 2;
				}
				else if (getPlayersInside().size() <= MIDDLE_ANTHARAS) {
					_antharas = (L2GrandBossInstance)_middleAntharas.doSpawn();
					BEHEMOTH_QTTY = 4;
				}
				else {
					_antharas = (L2GrandBossInstance)_strongAntharas.doSpawn();
					BEHEMOTH_QTTY = 6;
				}
				_antharas.setIsImmobilized(true);
				_antharas.setIsInSocialAction(true);
				_antharas.setCanReturnToSpawnPoint(false);
				_monsters.add(_antharas);

				// setting 1st time of minions spawn task.
					// spawn Behemoth.
				if(BEHEMOTH>0)
					_behemothSpawnTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BehemothSpawn(), BEHEMOTH * 60000, BEHEMOTH * 60000);

					// spawn Bomber.

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(2), 16);

				break;

			case 2:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_antharas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_antharas, 700, 13, -19, 0, 10000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(3), 3000);

				break;

			case 3:
				// do social.
				sa = new SocialAction(_antharas.getObjectId(), 1);
				_antharas.broadcastPacket(sa);

				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_antharas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_antharas, 700, 13, 0, 6000, 10000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(4), 10000);

				break;

			case 4:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_antharas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_antharas, 3800, 0, -3, 0, 10000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(5), 200);

				break;

			case 5:
				// do social.
				sa = new SocialAction(_antharas.getObjectId(), 2);
				_antharas.broadcastPacket(sa);

				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_antharas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_antharas, 1200, 0, -3, 22000, 11000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(6), 10800);

				break;

			case 6:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_antharas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_antharas, 1200, 0, -3, 300, 2000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(7), 1900);

				break;

			case 7:
				_antharas.abortCast();
				// reset camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					pc.leaveMovieMode();
				}

				_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_antharas), 16);

				// move at random.
				L2CharPosition pos = new L2CharPosition(Rnd.get(175000, 178500), Rnd.get(112400, 116000), -7707, 0);
				_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_antharas, pos), 32);
				_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(), ACTIVITY_TIME * 60000);

				break;
			}
			
		}
	}

	// do spawn Behemoth.
	private class BehemothSpawn implements Runnable
	{

		public void run()
		{
			if(_antharas.isDead()) {
				_behemothSpawnTask.cancel(false);
				_behemothSpawnTask = null;
				return;
			}
			L2NpcTemplate template1;
			
			template1 = NpcTable.getInstance().getTemplate(29069);
			for(int i=0;i<BEHEMOTH_QTTY;i++) {
					L2MonsterInstance  behemot  = new L2MonsterInstance(IdFactory.getInstance().getNextId(),template1);
					behemot.spawnMe(Rnd.get(175000, 179900), Rnd.get(112400, 116000), -7709);
					behemot.getStatus().setCurrentHpMp(behemot.getMaxHp(), behemot.getMaxMp());
					L2PcInstance player = getRandomPlayer();
					_monsters.add(behemot);
					if(player!=null && !player.isAlikeDead()) 
							behemot.addDamageHate(player, 100, 100);
			}
			
		}
	}


	// at end of activitiy time.
	private class ActivityTimeEnd implements Runnable
	{
		public void run()
		{
			setUnspawn();
		}
	}

	// clean Antharas's lair.
	@Override
	public void setUnspawn()
	{
		if(_behemothSpawnTask!=null) {
			_behemothSpawnTask.cancel(true);
			_behemothSpawnTask = null;
		}

		clearLair();
		for(L2NpcInstance npc : _monsters) 
			npc.deleteMe();
		if(_cube!=null) {
			_cube.deleteMe();
			_cube = null;
		}
		_monsters.clear();
		
		if(_antharas!=null && !_antharas.isDead()) {
			_state.setState(StateEnum.SLEEP);
			long interval = (MIN_SLEEP + Rnd.get((int)(MAX_SLEEP-MIN_SLEEP))) * 60000;
			_state.setRespawnDate(interval);
		} else {
			_state.setState(StateEnum.INTERVAL);
			long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN) * 60000;
			_state.setRespawnDate(interval);
		}
		_state.update();
		setIntervalEndTask();
		_log.info("AntharasManager: State of Antharas is "+_state.getState());
	}

	// start interval.
	public void setIntervalEndTask()
	{
		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	// at end of interval.
	private class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(GrandBossState.StateEnum.NOTSPAWN);
			_state.update();
			_log.info("AntharasManager: State of Antharas is "+_state.getState());
		}
	}

	// setting teleport cube spawn task.
	public void setCubeSpawn()
	{
		long interval = Rnd.get(MIN_RESPAWN, MAX_RESPAWN) * 60000;
		_state.setRespawnDate(interval);
		_state.setState(GrandBossState.StateEnum.INTERVAL);
		_activityTimeEndTask.cancel(true);
		setIntervalEndTask();
		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 10000);
	}

	// do spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		public void run()
		{
			spawnCube();
		}
	}

	// action is enabled the boss.
	private class SetMobilised implements Runnable
	{
		private L2GrandBossInstance	_boss;

		public SetMobilised(L2GrandBossInstance boss)
		{
			_boss = boss;
		}

		public void run()
		{
			_boss.setIsImmobilized(false);
			_boss.setIsInSocialAction(false);

		}
	}

	// Move at random on after Antharas appears.
	private class MoveAtRandom implements Runnable
	{
		private L2NpcInstance	_npc;
		private L2CharPosition	_pos;

		public MoveAtRandom(L2NpcInstance npc, L2CharPosition pos)
		{
			_npc = npc;
			_pos = pos;
		}

		public void run()
		{
			_npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _pos);
		}
	}

	@Override
	public void setRespawn() {
	}
	
	@Override
	public void onEnter(L2Character cha) {
		L2PcInstance pc = cha.getPlayer();
		if(pc!=null && !pc.isGM() && Config.EPIC_REQUIRE_QUEST) 
			if(pc.getQuestState(_questName)==null)
				pc.teleToLocation(TeleportWhereType.Town);
	}
	
}
