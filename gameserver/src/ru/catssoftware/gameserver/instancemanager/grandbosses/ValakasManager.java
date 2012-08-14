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

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import ru.catssoftware.Config;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.pack.ai.Valakas;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

import javolution.util.FastList;


/**
 *
 * This class ...
 * control for sequence of fight against Valakas.
 * @version $Revision: $ $Date: $
 * @author  L2J_JP SANDMAN
 */
public class ValakasManager extends BossLair
{
	private static ValakasManager	_instance;

	// location of teleport cube.
	private final int				_teleportCubeId				= 31759;
	private final int				_teleportCubeLocation[][]	=
																{
																{ 214880, -116144, -1644, 0 },
																{ 213696, -116592, -1644, 0 },
																{ 212112, -116688, -1644, 0 },
																{ 211184, -115472, -1664, 0 },
																{ 210336, -114592, -1644, 0 },
																{ 211360, -113904, -1644, 0 },
																{ 213152, -112352, -1644, 0 },
																{ 214032, -113232, -1644, 0 },
																{ 214752, -114592, -1644, 0 },
																{ 209824, -115568, -1421, 0 },
																{ 210528, -112192, -1403, 0 },
																{ 213120, -111136, -1408, 0 },
																{ 215184, -111504, -1392, 0 },
																{ 215456, -117328, -1392, 0 },
																{ 213200, -118160, -1424, 0 }

																};
	protected List<L2Spawn>			_teleportCubeSpawn			= new FastList<L2Spawn>();
	protected List<L2NpcInstance>	_teleportCube				= new FastList<L2NpcInstance>();


	// instance of monsters.
	protected List<L2NpcInstance>	_monsters					= new FastList<L2NpcInstance>();

	// tasks.
	protected ScheduledFuture<?>	_cubeSpawnTask				= null;
	protected ScheduledFuture<?>	_monsterSpawnTask			= null;
	protected ScheduledFuture<?>	_intervalEndTask			= null;
	protected ScheduledFuture<?>	_activityTimeEndTask		= null;
	protected ScheduledFuture<?>	_socialTask					= null;
	protected ScheduledFuture<?>	_mobiliseTask				= null;
	protected ScheduledFuture<?>	_moveAtRandomTask			= null;
	protected ScheduledFuture<?>	_respawnValakasTask			= null;
	private long MIN_RESPAWN;
	private long MAX_RESPAWN;
	private long ACTIVITY_TIME;
	private long ARRIVED_TIME;
	private int LAIR_CAPACITY;
	private long MIN_SLEEP;
	private long MAX_SLEEP;
	private L2GrandBossInstance	_valakas	= null;
	private boolean ENABLED;


	public ValakasManager()
	{
		
		super();
		_state = new GrandBossState(29028);
		try {

			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			ENABLED = Boolean.parseBoolean(p.getProperty("ValakasEnabled","true"));
			if(!ENABLED)
				return;
			
			MIN_RESPAWN = Integer.parseInt(p.getProperty("ValakasMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("ValakasMaxRespawn","2880"));
			ACTIVITY_TIME = Integer.parseInt(p.getProperty("ValakasActiveTime","50"));
			ARRIVED_TIME  = Integer.parseInt(p.getProperty("ValakasArrivedTime","5"));
			LAIR_CAPACITY = Integer.parseInt(p.getProperty("ValakasLairCapacity","200"));
			MIN_SLEEP = Integer.parseInt(p.getProperty("ValakasMinSleepTime","120"));
			MAX_SLEEP = Integer.parseInt(p.getProperty("ValakasMaxSleepTime","240"));
			
		} catch(Exception e) {
			_log.error("ValakasManager: Error while reading config",e);
			return;
		}
		
	}

	public static ValakasManager getInstance()
	{
		if (_instance == null)
			_instance = new ValakasManager();
		return _instance;
	}

	// initialize
	@Override
	public void init()
	{
		if(!ENABLED)
			return;
		
		new Valakas();
		_questName = Valakas.QUEST;
		// setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;

			// Valakas.
			template1 = NpcTable.getInstance().getTemplate(29028);
			_bossSpawn = new L2Spawn(template1);
			_bossSpawn.setLocx(212852);
			_bossSpawn.setLocy(-114842);
			_bossSpawn.setLocz(-1632);
			_bossSpawn.setHeading(833);
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}

		try
		{
			L2NpcTemplate Cube = NpcTable.getInstance().getTemplate(_teleportCubeId);
			L2Spawn spawnDat;
			for (int[] element : _teleportCubeLocation)
			{
				spawnDat = new L2Spawn(Cube);
				spawnDat.setAmount(1);
				spawnDat.setLocx(element[0]);
				spawnDat.setLocy(element[1]);
				spawnDat.setLocz(element[2]);
				spawnDat.setHeading(element[3]);
				_teleportCubeSpawn.add(spawnDat);
			}
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}

		_log.info("ValakasManager: State of Valakas is " + _state.getState() + ".");

		switch (_state.getState()) {
		case DEAD:
                        long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case SLEEP:	
		case INTERVAL:
			setIntervalEndTask();
			break;
		case UNKNOWN:
		case ALIVE:
			_state.setState(StateEnum.NOTSPAWN);
			break;
		}

		Date dt = new Date(_state.getRespawnDate());
		_log.info("ValakasManager: Next spawn date " + dt + ".");
	}

	@Override
	public boolean isEnableEnterToLair()
	{
		return getPlayersInside().size() < LAIR_CAPACITY && super.isEnableEnterToLair();
	}

	// do spawn teleport cube.
	public void spawnCube()
	{
		for (L2Spawn spawnDat : _teleportCubeSpawn)
		{
			_teleportCube.add(spawnDat.doSpawn());
		}
		_state.setState(StateEnum.DEAD);
		_state.update();
	}

	// setting Valakas spawn task.
	public void setValakasSpawnTask()
	{
		if (_monsterSpawnTask == null)
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(1), ARRIVED_TIME * 60000);
	}

	// do spawn Valakas.
	private class ValakasSpawn implements Runnable
	{
		private int					_distance	= 6502500;
		private int					_taskId;

		ValakasSpawn(int taskId)
		{
			_taskId = taskId;
		}

		public void run()
		{
			_monsterSpawnTask = null;
			SocialAction sa = null;

			switch (_taskId)
			{
			case 1:
				// do spawn.
				_valakas = (L2GrandBossInstance) _bossSpawn.doSpawn();
				_monsters.add(_valakas);
				_valakas.setIsImmobilized(true);
				_valakas.setCanReturnToSpawnPoint(false);
				_valakas.setIsInSocialAction(true);
				_state.setState(GrandBossState.StateEnum.ALIVE);
				_state.update();

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(2), 16);

				break;

			case 2:
				// do social.
				sa = new SocialAction(_valakas.getObjectId(), 1);
				_valakas.broadcastPacket(sa);

				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 1800, 180, -1, 1500, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(3), 1500);

				break;

			case 3:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 1300, 180, -5, 3000, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(4), 3300);

				break;

			case 4:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 500, 180, -8, 600, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(5), 1300);

				break;

			case 5:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 1200, 180, -5, 300, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(6), 1600);

				break;

			case 6:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 2800, 250, 70, 0, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(7), 200);

				break;

			case 7:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 2600, 30, 60, 3400, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(8), 5700);

				break;

			case 8:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 700, 150, -65, 0, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}
				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(9), 1400);

				break;

			case 9:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 1200, 150, -55, 2900, 15000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(10), 6700);

				break;

			case 10:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 750, 170, -10, 1700, 5700);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}

				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(11), 3700);

				break;

			case 11:
				// set camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					if (pc.getPlanDistanceSq(_valakas) <= _distance)
					{
						pc.enterMovieMode();
						pc.specialCamera(_valakas, 840, 170, -5, 1200, 2000);
					}
					else
					{
						pc.leaveMovieMode();
					}
				}
				_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(12), 2000);

				break;

			case 12:
				// reset camera.
				for (L2PcInstance pc : getPlayersInside())
				{
					pc.leaveMovieMode();
				}

				_state.setState(StateEnum.ALIVE);
				_state.update();
				_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_valakas), 16);
				L2CharPosition pos = new L2CharPosition(Rnd.get(211080, 214909), Rnd.get(-115841, -112822), -1662, 0);
				_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_valakas, pos), 32);
				_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityTimeEnd(), ACTIVITY_TIME * 60000);

				break;
			}
		}
	}

	// at end of activity time.
	private class ActivityTimeEnd implements Runnable
	{
		public void run()
		{
			setUnspawn();
		}
	}

	// clean Valakas's lair.
	@Override
	public void setUnspawn()
	{

		clearLair();
		for (L2NpcInstance mob : _monsters)
			mob.deleteMe();
		_monsters.clear();

		// delete teleport cube.
		for (L2NpcInstance cube : _teleportCube)
			cube.deleteMe();
		_teleportCube.clear();
		if(_state.getState()!=StateEnum.DEAD) {
			_state.setState(StateEnum.SLEEP);
			long interval = (MIN_SLEEP + Rnd.get((int)(MAX_SLEEP-MIN_SLEEP))) * 60000;
			_state.setRespawnDate(interval);
		} else {
			_state.setState(StateEnum.INTERVAL);
			long interval = (MIN_RESPAWN + Rnd.get((int)(MAX_RESPAWN-MIN_RESPAWN))) * 60000;
			_state.setRespawnDate(interval);
		}
		_state.update();
		setIntervalEndTask();
		_log.info("ValakasManager: State of Antharas is "+_state.getState());
	}

	public void setIntervalEndTask()
	{
		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	private class IntervalEnd implements Runnable
	{
		public void run()
		{
			_state.setState(GrandBossState.StateEnum.NOTSPAWN);
			_state.update();
			_log.info("ValakasManager: State of Valakas is "+_state.getState());
		}
	}

	public void setCubeSpawn()
	{
		_state.setState(StateEnum.DEAD);
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

	// Move at random on after Valakas appears.
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
		L2PcInstance player = cha.getActingPlayer();
		if(player!=null && !player.isGM() && Config.EPIC_REQUIRE_QUEST) {
			QuestState qs  = player.getQuestState(_questName);
			if(qs==null)
				player.teleToLocation(TeleportWhereType.Town);
		}
			
	}
}
