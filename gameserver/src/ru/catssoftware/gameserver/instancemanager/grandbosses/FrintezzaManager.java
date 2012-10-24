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
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2FrintezzaBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.GrandBossState;
import ru.catssoftware.gameserver.model.entity.GrandBossState.StateEnum;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.Earthquake;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.skills.AbnormalEffect;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.ArrayUtils;

import java.util.List;
import java.util.concurrent.Future;



/** *************************- main class + global class values -*********************************** */

/**
 * control for sequence of Frintezza and Scarlet Van Halisha and their minions.
 *
 * @version 1.00
 * @author Darki699
 */

public class FrintezzaManager extends BossLair
{
	private static FrintezzaManager	_instance;

	private int _intervalOfFrintezzaSongs = 10000;
	private long MIN_RESPAWN;
	private long MAX_RESPAWN;
	private long ACTIVITY_TIME;
	private boolean ENABLED;
	
	public FrintezzaManager()
	{
		super();
		_questName = "frintezza";
		_state = new GrandBossState(29045);
		try {
			L2Properties p = new L2Properties("./config/main/events/bosses.properties");
			MIN_RESPAWN = Integer.parseInt(p.getProperty("FrintezzaMinRespawn","1440"));
			MAX_RESPAWN = Integer.parseInt(p.getProperty("FrintezzaMaxRespawn","2880"));
			ACTIVITY_TIME = Integer.parseInt(p.getProperty("FrintezzaActiveTime","60"));
			ENABLED = Boolean.parseBoolean(p.getProperty("FrintezzaEnabled","true"));
			
		} catch(Exception e) {
			_log.error("FrintezzaManager: Error while reading config",e);
			return;
		}
		
	}

	public static FrintezzaManager getInstance()
	{
		if (_instance == null)
			_instance = new FrintezzaManager();

		return _instance;
	}

	/**
	 * initialize <b>this</b> Frintezza Manager
	 */
	private L2Spawn _weakScarlet;
	private L2FrintezzaBossInstance _frintezza;
	private L2FrintezzaBossInstance _scarlet;
	private L2Spawn _strongScarlet;
	private L2Spawn _cubeSpawn;
	private List<L2Spawn> _portraits = new FastList<L2Spawn>();
	private List<L2Spawn> _deamons = new FastList<L2Spawn>();
	private boolean _isWeakScarlet;
	private boolean _canPlayMusic;
	private boolean _skipMusic;
	private Future<?> _startTask;
	@Override
	public void init()
	{
		if(!ENABLED)
			return;
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(29045);
		_bossSpawn = new L2Spawn(template);
		_bossSpawn.setLocx(174240);
		_bossSpawn.setLocy(-89805);
		_bossSpawn.setLocz(-5022);
		_bossSpawn.setHeading(16048);
		
		template = NpcTable.getInstance().getTemplate(29046);
		_weakScarlet = new L2Spawn(template);
		_weakScarlet.setLocx(174234);
		_weakScarlet.setLocy(-88015);
		_weakScarlet.setLocz(-5116);
		_weakScarlet.setHeading(48028);
		template = NpcTable.getInstance().getTemplate(29047);
		_strongScarlet = new L2Spawn(template);
		
		template = NpcTable.getInstance().getTemplate(31759);
		_cubeSpawn = new L2Spawn(template);
		_cubeSpawn.setLocx(174234);
		_cubeSpawn.setLocy(-88015);
		_cubeSpawn.setLocz(-5116);
		
		_portraits.add(createNewSpawn(29048, 175833, -87165, -4972, 35048, 0));
		_portraits.add(createNewSpawn(29049, 175876, -88713, -4972, 28205, 0));
		_portraits.add(createNewSpawn(29048, 172608, -88702, -4972, 64817, 0));
		_portraits.add(createNewSpawn(29049, 172634, -87165, -4972, 57730, 0));
		
		_deamons.add(createNewSpawn(29050, 175833, -87165, -4972, 35048, 180));
		_deamons.add(createNewSpawn(29051, 175876, -88713, -4972, 28205, 180));
		_deamons.add(createNewSpawn(29051, 172608, -88702, -4972, 64817, 180));
		_deamons.add(createNewSpawn(29050, 172634, -87165, -4972, 57730, 180));
		
		switch (_state.getState()) {
		case DEAD:
                        long inter = Rnd.get((int)(MIN_RESPAWN*60000), (int)(MAX_RESPAWN*60000));
			_state.setRespawnDate(inter);
			_state.setState(StateEnum.INTERVAL);
		case SLEEP:	
		case INTERVAL:
			setIntervalTask();
			break;
		case UNKNOWN:
		case ALIVE:
			_state.setState(StateEnum.NOTSPAWN);
			break;
		}
		_log.info("FrintezzaManager: State of Frintezza "+_state.getState());
		
	}

	private void setIntervalTask() {
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				_state.setState(StateEnum.NOTSPAWN);
				_state.update();
				_log.info("FrintezzaManager: State of Frintezza "+_state.getState());
			}
		}, _state.getInterval());
	}

	private L2Spawn createNewSpawn(int templateId, int x, int y, int z, int heading, int respawnDelay) {
		L2NpcTemplate template1;
		template1 = NpcTable.getInstance().getTemplate(templateId);
		L2Spawn tempSpawn = new L2Spawn(template1);
		tempSpawn.setLocx(x);
		tempSpawn.setLocy(y);
		tempSpawn.setLocz(z);
		tempSpawn.setHeading(heading);
		tempSpawn.setAmount(1);
		tempSpawn.setRespawnDelay(respawnDelay);
		return tempSpawn;
	}
	@Override
	public void setRespawn() {
		
	}

	public void stopActivity() {
		if(!ENABLED)
			return;

		clearLair();
		finish();
		if(_cubeSpawn.getLastSpawn()!=null)
			_cubeSpawn.getLastSpawn().deleteMe();
		long interval = Rnd.get(MIN_RESPAWN,MAX_RESPAWN) * 60000;
		_state.setRespawnDate(interval);
		_state.setState(StateEnum.INTERVAL);
		setIntervalTask();
	}
	
	public void finish() {
		if(!ENABLED)
			return;

		if(_scarlet!=null) {
			_scarlet.deleteMe();
			_scarlet = null;
		}
		if(_frintezza!=null) {
			_frintezza.deleteMe();
			_frintezza = null;
		}
		for(L2Spawn spawn : _portraits) {
			spawn.stopRespawn();
			if(spawn.getLastSpawn()!=null)
				spawn.getLastSpawn().deleteMe();
		}
		for(L2Spawn spawn : _deamons ) {
			spawn.stopRespawn();
			if(spawn.getLastSpawn()!=null)
				spawn.getLastSpawn().deleteMe();
		}
		if(_state.getState()==StateEnum.DEAD) 
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				public void run() {
					_cubeSpawn.doSpawn();
				}
			},2000);
	}
	
	private class ActionTask implements Runnable {
		
		private int _taskId;
		public ActionTask(int taskId) {
			_taskId = taskId;
		}
		private  ActionTask setTask(int taskId) {
			_taskId = taskId;
			return this;
		}
		@Override
		public void run() {
			switch(_taskId) {
			case 2:
				showSocialActionMovie(_frintezza, 1000, 90, 30, 0, 5000, 0);
				ThreadPoolManager.getInstance().schedule(setTask(200), 3000);
				break;
			case 200:
				showSocialActionMovie(_frintezza, 1000, 90, 30, 0, 5000, 0);
				ThreadPoolManager.getInstance().schedule(setTask(3), 3000);
				break;
			case 3:
				showSocialActionMovie(_frintezza, 140, 90, 0, 6000, 6000, 2);
				ThreadPoolManager.getInstance().schedule(setTask(5),5990);
				break;
			case 5:
				showSocialActionMovie(_frintezza, 240, 90, 3, 22000, 6000, 3);
				ThreadPoolManager.getInstance().schedule(setTask(6),5800);
				break;
			case 6:
				showSocialActionMovie(_frintezza, 240, 90, 3, 300, 6000, 0);
				MagicSkillUse msu = new MagicSkillUse(_frintezza, _frintezza, 5006, 1, _intervalOfFrintezzaSongs, 0, false);
				for(L2PcInstance pc : getPlayersInside())
					pc.sendPacket(msu);
				_scarlet = (L2FrintezzaBossInstance)_weakScarlet.doSpawn();
				for(L2Spawn spawn : _portraits) {
					L2NpcInstance npc = spawn.doSpawn();
					npc.setIsImmobilized(true);
				}
				for(L2Spawn spawn : _deamons) {
					spawn.startRespawn();
					spawn.doSpawn();
				}
				Earthquake eq = new Earthquake(_scarlet.getX(), _scarlet.getY(), _scarlet.getZ(), 50, 6);
				for(L2PcInstance pc : getPlayersInside())
					pc.sendPacket(eq);
				ThreadPoolManager.getInstance().scheduleGeneral(setTask(7), 5800);
				break;
			case 7:
				for (L2PcInstance pc : getPlayersInside())
				{
					pc.leaveMovieMode();
					pc.enableAllSkills();
					pc.setIsImmobilized(false);
				}
				_frintezza.setIsImmobilized(false);
				ThreadPoolManager.getInstance().schedule(new MusicTask(Rnd.get(1,4)), Rnd.get(_intervalOfFrintezzaSongs,_intervalOfFrintezzaSongs*2));
				ThreadPoolManager.getInstance().schedule(new Runnable() {
					public void run() {
						stopActivity();
					}
				},ACTIVITY_TIME * 60000);
				
				break;
			}
			
		}
		
	}

	public void start() {
		if(!ENABLED)
			return;

		_frintezza = (L2FrintezzaBossInstance)_bossSpawn.doSpawn();
		_frintezza.disableAllSkills();
		_frintezza.setIsImmobilized(true);
		_frintezza.setIsInvul(true);
		_state.setState(StateEnum.ALIVE);
		_canPlayMusic = true;
		_skipMusic = false;
		_isWeakScarlet = true;
		ThreadPoolManager.getInstance().schedule(new ActionTask(2), 1000);
	}
	
	public synchronized void skipMusic() {
		_skipMusic = true;
	}
	public synchronized void onKill(L2NpcInstance npc) {
		if(npc.getNpcId() == 29048 || npc.getNpcId() == 29049) {
			int index = _portraits.indexOf(npc.getSpawn());
			_deamons.get(index).stopRespawn();
		}
		else if(npc == _scarlet) {
			_scarlet = null;
			_state.setState(StateEnum.DEAD);
			finish();
		}
	}
	
	public class MusicTask implements Runnable {

		private int _musicId;
		public MusicTask(int musicId) {
			_musicId = musicId;
		}
		public L2Character[] getMusicTarget() {
			L2Character [] result = new L2Character[] {};
			switch (_musicId) {
				case 1: 
				case 5:
						for (L2PcInstance pc : getPlayersInside()) 
							if(!pc.isDead() && pc.isInsideRadius(_frintezza, 5000, false, false))
								result = ArrayUtils.add(result, pc);
						break;
				case 2: 
					if (_scarlet!=null && !_scarlet.isDead())
						result = ArrayUtils.add(result, _scarlet);
					break;
				case 3:
				case 4:
					if (_scarlet!=null && !_scarlet.isDead())
						result = ArrayUtils.add(result, _scarlet);
					for(L2Spawn s : _deamons)
						if(!s.getLastSpawn().isDead())
							result = ArrayUtils.add(result, s.getLastSpawn());
					break;
			}
			return result;
		}
		public MusicTask setMusic(int musicId) {
			_musicId = musicId;
			return this;
		}
		
		@Override
		public void run() {
			if(!_canPlayMusic || _frintezza==null)
				return;
			if(!_skipMusic) {
				for(L2PcInstance pc : getPlayersInside())
					pc.sendPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 1, _intervalOfFrintezzaSongs, 0, false));
				
				L2Skill skill = null;
				switch(_musicId) {
					case 2:
						skill = SkillTable.getInstance().getInfo(1217, 33);
						break;
					case 3:
						skill = SkillTable.getInstance().getInfo(1204, 2);
						break;
					case 4:
						skill = SkillTable.getInstance().getInfo(1086, 2);
						break;
					case 5:
						skill = SkillTable.getInstance().getInfo(5008, 5);
						break;
				}
				if(skill!=null)
					_frintezza.callSkill(skill, getMusicTarget());
				if(_musicId==5) {
					for(L2Character ch : getMusicTarget()) {
						setIdle(ch);
						ch.startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
					}
						
					ThreadPoolManager.getInstance().schedule(new Runnable() {
						public void run() {
							for(L2PcInstance pc : getPlayersInside()) {
								pc.setIsImmobilized(false);
								pc.enableAllSkills();
								pc.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
							}
								
						}
					}, Rnd.get(_intervalOfFrintezzaSongs/2, _intervalOfFrintezzaSongs));
				}
			} 
			_skipMusic = false;
			ThreadPoolManager.getInstance().schedule(setMusic(Rnd.get(1,_isWeakScarlet?4:5)), Rnd.get(_intervalOfFrintezzaSongs, _intervalOfFrintezzaSongs*2));
		}
		
	}
	public synchronized  void respawnScarlet() {
		if(!ENABLED)
			return;

		if(!_isWeakScarlet)
			return;
		Location loc = _scarlet.getLoc();
		L2Character victim = _scarlet.getMostHated();
		_scarlet.deleteMe();
		_isWeakScarlet = false;
		_strongScarlet.setLocx(loc.getX());
		_strongScarlet.setLocy(loc.getY());
		_strongScarlet.setLocz(loc.getZ());
		_strongScarlet.setHeading(loc.getHeading());
		
		_scarlet = (L2FrintezzaBossInstance)_strongScarlet.doSpawn();
		Earthquake eq = new Earthquake(_scarlet.getX(), _scarlet.getY(), _scarlet.getZ(), 50, 6);
		for(L2PcInstance pc : getPlayersInside())
			pc.sendPacket(eq);
		if(victim!=null)
			_scarlet.addDamageHate(victim, 1, 100);
	}
	
	@Override
	public void setUnspawn() {
	}

	private void setIdle(L2Character target)
	{
		target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		target.abortAttack();
		target.abortCast();
		target.setIsImmobilized(true);
		target.disableAllSkills();
	}

	public synchronized void callAssist(L2Character attacker ) {
		if(!ENABLED)
			return;

		for(L2Spawn s : _deamons) {
			L2Attackable m = (L2Attackable)s.getLastSpawn();
			if(m!=null && !m.isDead() && !m.isInCombat())
				m.addDamageHate(attacker, 1,100);
		}
	}
	
	private void showSocialActionMovie(L2NpcInstance target, int dist, int yaw, int pitch, int time, int duration, int socialAction)
	{
		if (target == null)
			return;
		for (L2PcInstance pc : getPlayersInside())
		{
			setIdle(pc);
			pc.setTarget(null);
			if (pc.getPlanDistanceSq(target) <= 6502500)
			{
				pc.enterMovieMode();
				pc.specialCamera(target, dist, yaw, pitch, time, duration);
			}
			else
				pc.leaveMovieMode();
		}
		// do social.
		if (socialAction > 0 && socialAction < 5)
			target.broadcastPacket(new SocialAction(target.getObjectId(), socialAction));
	}
	
	@Override
	public void onEnter(L2Character cha)
	{
		if(!ENABLED || !cha.isPlayer())
			return;
		L2PcInstance player = cha.getPlayer();
		QuestState qs = player.getQuestState("654_JourneytoaSettlement");

	 	if(qs != null && qs.isCompleted() && _state.getState()==StateEnum.NOTSPAWN && _startTask==null)
			 _startTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
			 public void run() {
				 start();
				 _startTask = null;
			 }
		 }, 60000);
	}
}