package ru.catssoftware.gameserver.model.entity.events.TvT;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.taskmanager.TaskManager;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.ArrayUtils;

import java.util.List;
import java.util.Map;


public class TvT extends GameEvent
{
	public class Team
	{
		public String name;
		public int color;
		public Location loc;
		public Map<Integer,Integer> players = new FastMap<Integer,Integer>();
		public int kills = 0;
		public int index;
	}

	private Map<Integer,Team> _participants = new FastMap<Integer, Team>();
	private Map<Integer, Location> _playerLocations = new FastMap<Integer, Location>();
	private Map<Integer, Integer> _kills;
	private List<Team> _teams = new FastList<Team>();

	/* -------- ПАРАМЕТРЫ ЭВЕНТА -------- */
	private Instance			_instanceObj;
	private int					_elapsed;
	private static TvT 			_instance;
	private boolean				_canStand				= true;
	public int					_instanceId				= 0;
	public int					[]_rewardId				= null;
	public int					[]_rewardAmount			= null;
	public int					_minlvl					= 0;
	public int					_maxlvl					= 0;
	public int					_regTime				= 0;
	public int					_eventTime				= 0;
	public int					_minPlayers				= 0;
	public int					_maxPlayers				= 60;

	/* ------ КОНФИГУРАЦИЯ ЭВЕНТА ------- */
	private boolean TVT_ALLOW_POTIONS;
	private long 	TVT_REVIVE_DELAY;
	private boolean TVT_ALLOW_SUMMON;
	private boolean TVT_ALLOW_ENEMY_HEALING;
	private boolean TVT_JOIN_CURSED;
	private boolean TVT_ON_START_REMOVE_ALL_EFFECTS;
	private boolean TVT_ON_START_UNSUMMON_PET;
	private boolean TVT_CLOSE_COLISEUM_DOORS;
	private boolean TVT_ALLOW_INTERFERENCE;
	private boolean TVT_REVIVE_RECOVERY;
	private boolean TVT_PRICE_NO_KILLS;
	private boolean TVT_ALLOW_TEAM_CASTING;
	private boolean TVT_ALLOW_TEAM_ATTACKING;
	private boolean TVT_AURA;
	private boolean	TVT_ININSTANCE;
	private boolean	TVT_ORIGINALRETURN;

	public TvT()
	{
		_instance = this;
	}

	public static TvT getInstance()
	{
		if(_instance==null)
			new TvT();
		return _instance;
	}
	
	public String getStatus()
	{
		int free = (_maxPlayers - _participants.size());
		if (free < 0)
			free = 0;

		return free + " из " + _maxPlayers;
	}

	@Override
	public boolean finish()
	{
		_startEventTask.cancel();
		_eventTask.cancel();

		L2PcInstance player;
		for(Integer playerId: _participants.keySet())
		{
			player = L2World.getInstance().findPlayer(playerId);
			if(player != null)
			{
				player.setGameEvent(null);
				remove(player);
			}
		}

		_participants.clear();

		if(TVT_CLOSE_COLISEUM_DOORS && !TVT_ININSTANCE)
		{
			DoorTable.getInstance().getDoor(24190001).openMe();
			DoorTable.getInstance().getDoor(24190002).openMe();
			DoorTable.getInstance().getDoor(24190003).openMe();
			DoorTable.getInstance().getDoor(24190004).openMe();
		}

		if(_eventScript!=null)
			_eventScript.onFinish(_instanceId);
		
		if(_instanceObj != null) {
			for(Integer charId : _instanceObj.getPlayers()) {
				L2PcInstance pc = L2World.getInstance().getPlayer(charId);
				if(pc!=null) {
					pc.setInstanceId(0);
					randomTeleport(pc);
				}
			}
			InstanceManager.getInstance().destroyInstance(_instanceId);
		}

		_instanceId = 0;
		setState(State.STATE_OFFLINE);
		
		return true;
	}

	@Override
	public String getName()
	{
		return "TeamVsTeam";
	}

	@Override
	public boolean isParticipant(L2PcInstance player)
	{
		return !isState(State.STATE_OFFLINE) && _participants.containsKey(player.getObjectId());
	}

	@Override
	public boolean load()
	{
		readConfig();
		if(_instance == null)
			return false;
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new VoiceTVTEngine());
		TaskManager.getInstance().registerTask(new TaskTvTStart());
		return true;
	}

	@Override
	public void onKill(L2Character killer, L2Character victim)
	{
		if(killer==null || victim ==null)
			return;

		Team killerTeam = getPlayerTeam(killer);
		Team victimTeam = getPlayerTeam(victim);
		L2PcInstance pk =killer.getActingPlayer();
		if(killerTeam != null && victimTeam != null)
		{
			if(killerTeam != victimTeam)
			{
				killerTeam.kills++;
				int kills = killerTeam.players.get(pk.getObjectId());
				kills++;
				killerTeam.players.put(pk.getObjectId(), kills);
				_kills.put(pk.getObjectId(), kills);
				killer.broadcastFullInfo();
				victim.sendMessage(Message.getMessage((L2PcInstance)victim, Message.MessageId.MSG_EVENT_WAIT_FOR_RES));
			}
			else
			{
				pk.sendMessage(Message.getMessage(pk, Message.MessageId.MSG_EVENT_YOU_KILL_TEAM_MEMBER));
				if (pk.getDeathPenaltyBuffLevel() < 10)
				{
					pk.setDeathPenaltyBuffLevel(pk.getDeathPenaltyBuffLevel() + 4);
					pk.increaseDeathPenaltyBuffLevel();
				}
				killerTeam.kills--;
			}
		}
		else
		{
			if (pk.getDeathPenaltyBuffLevel() < 10)
			{
				pk.sendMessage(Message.getMessage(pk, Message.MessageId.MSG_EVENT_YOU_KILL_REGULAR_PLAYER));
				pk.setDeathPenaltyBuffLevel(pk.getDeathPenaltyBuffLevel() + 5);
			}
		}
		if(victim!=null && victim.getGameEvent()==this)
			ThreadPoolManager.getInstance().scheduleGeneral(new TeleportVictim(victim),TVT_REVIVE_DELAY*1000);
	}

	@Override
	public boolean register(L2PcInstance player)
	{
		if(!canRegister(player))
			return false;
		_participants.put(player.getObjectId(), null);
		player.setGameEvent(this);
		return true;
	}

	@Override
	public void remove(L2PcInstance player)
	{
		if(isParticipant(player))
		{
			if(isRunning())
			{
				if(getPlayerTeam(player)!=null)
					getPlayerTeam(player).players.remove(player.getObjectId());
				player.setInstanceId(0);
				if(!TVT_ORIGINALRETURN)
					randomTeleport(player);
				else
					player.teleToLocation(_playerLocations.get(player.getObjectId()), false);
				player.setTeam(0);
				if(player.isDead())
					player.doRevive();
				player.setGameEvent(null);
				player.broadcastUserInfo();
			}
			_participants.remove(player.getObjectId());
			player.setGameEvent(null);
		}
		
	}

	@Override
	public boolean start()
	{
		if(!isState(State.STATE_OFFLINE))
			return false;
		
		_participants.clear();
		_teams.clear();
		_kills = new FastMap<Integer, Integer>();
		readConfig();

		_elapsed = (_regTime * 60000) / 2;
		
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_1),getName()));
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_2),getName(),_minlvl,_maxlvl));
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_3),getName()));
		for(int i=0;i<_rewardId.length;i++) 
			AnnounceToPlayers(true, " - "+_rewardAmount[i]+" "+ItemTable.getInstance().getTemplate(_rewardId[i]).getName());
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_4), getName(),_regTime));

		_startEventTask.schedule(_elapsed);
		setState(State.STATE_ACTIVE);
		return true;
	}
	

	@Override
	public boolean canInteract(L2Character actor, L2Character target)
	{
		return !isRunning() || (actor.getGameEvent() == target.getGameEvent() && actor.getGameEvent() == this) || TVT_ALLOW_INTERFERENCE;
	}

	@Override
	public boolean canAttack(L2Character attacker, L2Character target)
	{
		if(isRunning())
		{
			if(attacker.getGameEvent() == target.getGameEvent() && attacker.getGameEvent() == this)
				return (getPlayerTeam(attacker)!= getPlayerTeam(target)) || TVT_ALLOW_TEAM_ATTACKING;
			else 
				return false;
		}
		return false;
	}

	@Override
	public boolean canBeSkillTarget(L2Character caster, L2Character target, L2Skill skill)
	{
		if(isRunning())
		{
			if(skill.getTargetType() == SkillTargetType.TARGET_ALLY ||  skill.getTargetType() == SkillTargetType.TARGET_CLAN || skill.getTargetType() == SkillTargetType.TARGET_PARTY )
				return getPlayerTeam(caster) == getPlayerTeam(target);
		} 
		return true;
	}

	@Override
	public boolean canUseItem(L2Character actor, L2ItemInstance item)
	{
		boolean result = true;
		if(isRunning())
		{
			if(item.getItem().getItemType() == L2EtcItemType.POTION )
				result =  TVT_ALLOW_POTIONS;
			else if (item.getItem().getItemType() == L2EtcItemType.PET_COLLAR)
				result =  TVT_ALLOW_SUMMON;
			else {
				int itemId = item.getItemId();
				result = !((itemId == 3936 || itemId == 3959 || itemId == 737 || itemId == 9157 || itemId == 10150 || itemId == 13259));
			}
			
		}
		if(!result)
			actor.sendMessage(Message.getMessage((L2PcInstance)actor, Message.MessageId.MSG_EVENT_CANT_USE_ITEM));
		return result;
	}

	@Override
	public boolean canUseSkill(L2Character caster, L2Skill skill)
	{
		boolean result = true;
		if(isRunning())
		{
			if(skill.getSkillType() == L2SkillType.SUMMON)
				result =  TVT_ALLOW_SUMMON;
			else if( skill.getTargetType() == SkillTargetType.TARGET_SELF || skill.getTargetType() == SkillTargetType.TARGET_PET ||skill.getTargetType() == SkillTargetType.TARGET_AURA || skill.getTargetType() == SkillTargetType.TARGET_PARTY)
				return true;
			else if(skill.getSkillType() == L2SkillType.HEAL || skill.getSkillType() == L2SkillType.REFLECT || skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.MANAHEAL)  
				result = getPlayerTeam(caster) == getPlayerTeam((L2Character)caster.getTarget()) || TVT_ALLOW_ENEMY_HEALING;
			else if(getPlayerTeam(caster) == getPlayerTeam((L2Character)caster.getTarget()))
				result = TVT_ALLOW_TEAM_CASTING;
		}
		if(!result)
			caster.sendMessage(Message.getMessage(caster.getActingPlayer(), Message.MessageId.MSG_EVENT_SKILL_NOT_ALOWED));
		return result;
	}

	@Override
	public boolean canDoAction(L2PcInstance player, int action)
	{
		return !isRunning() || _canStand;
	}
	
	@Override
	public void onLogin(L2PcInstance player)
	{
		if(isRunning())
		{ 
			Team playerTeam = getPlayerTeam(player);
			if(playerTeam!=null)
			{
				player.setTitle("Kills: " + playerTeam.players.get(player.getObjectId()));
				if(TVT_AURA)
					player.setTeam(playerTeam.index);
				player.setInstanceId(_instanceId);
				player.teleToLocation(playerTeam.loc,false);
			}
		}
	}
	
	@Override
	public boolean onNPCTalk(L2NpcInstance npc, L2PcInstance talker)
	{
		return false;
	}

	@Override
	public void onLogout(L2PcInstance player)
	{
		if(isRunning())
		{
			Team playerTeam = getPlayerTeam(player);
			if(playerTeam!=null)  
				player.teleToLocation(playerTeam.loc, false);
		}
	}

	@Override
	public void onRevive(L2Character actor)
	{
	 	if(isRunning())
	 	{
	 		if(TVT_REVIVE_RECOVERY)
	 		{
	 		 	actor.getStatus().setCurrentCp(actor.getMaxCp());
	 		 	actor.getStatus().setCurrentMp(actor.getMaxMp());
	 		 	actor.getStatus().setCurrentHp(actor.getMaxHp());
	 		}
	 	}
	}

	@Override
	public boolean canRegister(L2PcInstance player)
	{
		if (!isState(State.STATE_ACTIVE))
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_NOT_ALLOWED));
			return false;
		}
		if (isParticipant(player))
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_ALREADY_REGISTERED));
			return false;
		}
		if(_participants==null)
			return false;
		if(!Config.Allow_Same_HWID_On_Events && player.getHWid()!=null && player.getHWid().length()!=0) {
			L2PcInstance pc = null;
			for(int charId : _participants.keySet()) {
				pc = L2World.getInstance().getPlayer(charId);
				if(pc !=null && player.getHWid().equals(pc.getHWid())) {
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_HWID_ALREADY_REGISTERED));
					return false;
				}
			}
		}
		if(!Config.Allow_Same_IP_On_Events) {
			L2PcInstance pc = null;
			for(int charId : _participants.keySet()) {
				pc = L2World.getInstance().getPlayer(charId);
				if(pc !=null && pc.getClient()!=null && player.getClient().getHostAddress().equals(pc.getClient().getHostAddress())) {
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_HWID_ALREADY_REGISTERED));
					return false;
				}
			}
		}
		
		if (_participants.size() >= _maxPlayers)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_FULL));
			return false;
		}
		if (player.isCursedWeaponEquipped() && !TVT_JOIN_CURSED)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CURSED_WEAPON_NOT_ALLOW));
			return false;
		}
		if (player.getLevel() > _maxlvl || player.getLevel() < _minlvl)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_WRONG_LEVEL));
			return false;
		}
		return player.canRegisterToEvents();
	}

	@Override
	public void onCommand(L2PcInstance actor, String command, String params)
	{
		if(actor==null)
			return;
	
		if(command.equals("join"))
		{
			if(register(actor))
				actor.sendMessage(String.format(Message.getMessage(actor, Message.MessageId.MSG_EVENT_REGISTERED),"TVT"));
			else
				actor.sendMessage(Message.getMessage(actor, Message.MessageId.MSG_EVENT_CANT_REGISTERED));
		}
		else if(command.equals("leave"))
		{
			if(isParticipant(actor) && isState(State.STATE_ACTIVE))
			{
				remove(actor);
				actor.sendMessage(String.format(Message.getMessage(actor, Message.MessageId.MSG_EVENT_CANCEL_REG),"TVT"));
			}
			else 
				actor.sendMessage(Message.getMessage(actor, Message.MessageId.MSG_EVENT_NOT_REGISTERED));
		}
	}
	
//==================================== Приватные методы и члены класса =================
	private class TeleportVictim implements Runnable
	{
		L2Character _victim;
		public TeleportVictim(L2Character victim)
		{
			_victim = victim;
		}

		@Override
		public void run()
		{
			if(getPlayerTeam(_victim)!=null)
			{
				_victim.teleToLocation(getPlayerTeam(_victim).loc, false);
				_victim.doRevive();
			}
			else
			{
				remove((L2PcInstance)_victim);
			}
		}
	}

	public void run()
	{
		_startEventTask.cancel();
		setState(State.STATE_RUNNING);

		if(TVT_ININSTANCE)
		{
			_instanceId = InstanceManager.getInstance().createDynamicInstance(null);
			_instanceObj = InstanceManager.getInstance().getInstance(_instanceId);
			_instanceObj.setReturnTeleport(81260, 148607, -3471);
			if(TVT_CLOSE_COLISEUM_DOORS)
			{
				_instanceObj.addDoor(24190001, false);
				_instanceObj.addDoor(24190002, false);
				_instanceObj.addDoor(24190003, false);
				_instanceObj.addDoor(24190004, false);
			}
		}
	
		_playerLocations.clear();
		L2PcInstance player;
		for(int charId : _participants.keySet())
		{
			player = L2World.getInstance().findPlayer(charId);
			if(player != null)
			{
				if(player.getInstanceId()!=0 || player.getLevel() < _minlvl || player.getLevel() > _maxlvl || player.inPrivateMode() || player.isDead())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_CANT_REGISTERED));
					player.setGameEvent(null);
					_participants.remove(charId);
				} 
				if(!TVT_ORIGINALRETURN)
					player.setIsIn7sDungeon(false);
				else
					_playerLocations.put(player.getObjectId(), new Location(player.getLoc()));
				
			}
			else
				_participants.remove(charId);
		}
		if(_participants.size() < _minPlayers)
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_9), getName()));
			finish();
			return;
		}
		int delta = _participants.size()%2==0?0:1; 
		for(;;)
		{
			boolean allShuffled = true;
			for(Integer playerId : _participants.keySet())
			{
				if(_participants.get(playerId)==null)
				{
					Team team;
					for(;;)
					{
						team = _teams.get(Rnd.get(_teams.size()));
						if(team.players.size()< _participants.size()/_teams.size()+delta)
						{
							break;
						}
					}
					team.players.put(playerId,0);
					_participants.put(playerId,team);
					allShuffled = false;
				}
			}
			if(allShuffled)
				break;
		}

		ThreadPoolManager.getInstance().schedule(_teleportTask, 10000);
		if(TVT_CLOSE_COLISEUM_DOORS && !TVT_ININSTANCE)
		{
			DoorTable.getInstance().getDoor(24190001).closeMe();
			DoorTable.getInstance().getDoor(24190002).closeMe();
			DoorTable.getInstance().getDoor(24190003).closeMe();
			DoorTable.getInstance().getDoor(24190004).closeMe();
		}
		if(_eventScript!=null)
			_eventScript.onStart(_instanceId);
	}

	private Runnable _teleportTask = new Runnable()
	{
		@Override
		public void run()
		{
			L2PcInstance player;
			_canStand = false;
			for(Team team: _teams)
			{
				for(Integer playerId: team.players.keySet())
				{
					player = L2World.getInstance().getPlayer(playerId);
					if(player!=null)
					{
						player.abortAttack();
						player.abortCast();
						player.setTarget(null);
						if(player.getPet()!=null )
						{
							player.getPet().abortAttack();
							player.getPet().abortCast();
							player.getPet().setTarget(null);
							if(TVT_ON_START_UNSUMMON_PET)
								player.getPet().unSummon(player);
						}
						if(TVT_ON_START_REMOVE_ALL_EFFECTS)
						{
							player.stopAllEffects();
							if(player.getPet()!=null)
								player.getPet().stopAllEffects();
						}
						if(player.getParty()!=null)
							player.getParty().removePartyMember(player);
						onLogin(player);
						player.sitDown(true);
					}
				}
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					_canStand = true;
					L2PcInstance player;
					for(Team team: _teams)
					{
						for(Integer playerId: team.players.keySet())
						{
							player = L2World.getInstance().getPlayer(playerId);
							if(player!=null) 
								player.standUp(true);
						}
					}
					_elapsed = ( _eventTime * 60000 ) / 2;
					AnnounceToPlayers(false, String.format(Message.getMessage(null, MessageId.MSG_EVT_10), getName()));
					_eventTask.schedule(_elapsed);
				}
			}, 10000);
		}
	};
	
	private void AnnounceToPlayers(Boolean toall, String announce)
	{
		if (toall)
			Announcements.getInstance().criticalAnnounceToAll(announce);
		else
		{
			L2PcInstance player;
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Critical_Announce, "", announce);
			for(Integer charId : _participants.keySet())
			{
				player = L2World.getInstance().getPlayer(charId);
				if(player!=null)
					player.sendPacket(cs);
			}
		}
	}

	private void doReward()
	{
		L2PcInstance player;
		for(Integer playerId: _participants.keySet())
		{
			player = L2World.getInstance().findPlayer(playerId);
			if(player!=null)
			{
				player.abortAttack();
				player.abortCast();
				player.setTarget(null);
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
			
		}

		Team 	winner = null;
		int		top_score = 0;

		for(Team team : _teams)
		{
			if (team.kills == top_score && top_score > 0)
				winner = null;
			if (team.kills > top_score)
			{
				winner = team;
				top_score = team.kills;
			}
		}

		if(winner!=null && winner.kills > 0)
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_7), getName(),winner.name));

			for(Integer playerId : _participants.keySet())
			{
				player = L2World.getInstance().findPlayer(playerId);
				if(player != null)
				{
					if (_participants.get(player.getObjectId()) == winner)
					{
						if(TVT_PRICE_NO_KILLS || winner.players.get(playerId) > 0)
							player.addItem("TvT reward", _rewardId,_rewardAmount,null,true);
					}	
				}
			}
		}
		else 
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_8), getName()));
		}
	}

	private ExclusiveTask _eventTask = new ExclusiveTask()
	{
		@Override
		protected void onElapsed()
		{
			if(_elapsed > 0)
			{
				schedule(_elapsed);
				_elapsed = 0;
			}
			else
			{
				doReward();
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					public void run()
					{
						finish();
					}
				}, 10000);
			}
		}
	};
	
	private ExclusiveTask _startEventTask = new ExclusiveTask()
	{
		private int announces = 0;
		private boolean showed;
		@Override
		protected void onElapsed()
		{
			if(_elapsed <= 1000)
			{
				run();
			}
			else
			{
				if(_elapsed > 60000) {
					if(announces==0) {
						AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_5), getName(),_elapsed/60000));
						announces++;
					}
				}
				else 
					if(announces==1 && _elapsed<=30000 && !showed) {
						AnnounceToPlayers(true,String.format(Message.getMessage(null, MessageId.MSG_EVT_6), getName()));
						showed = true;
						announces++;
					}
				_elapsed /= 2;
				schedule(_elapsed);
			}	
		}
	};

	private Team getPlayerTeam(L2Character player)
	{
		if(player==null)
			return null;
		return _participants.get(player.getObjectId());
	}
	
	private void randomTeleport(L2PcInstance player)
	{
		int _locX, _locY, _locZ;
		int _Rnd = Rnd.get(100);

		if (_Rnd < 20) // Giran
		{
			_locX = 81260;
			_locY = 148607;
			_locZ = -3471;
		}
		else if (_Rnd < 40) // Goddart
		{
			_locX = 147709;
			_locY = -53231;
			_locZ = -2732;	
		}
		else if (_Rnd < 60) // Rune
		{
			_locX = 43429;
			_locY = -50913;
			_locZ = -796;
		}
		else if (_Rnd < 80) // Oren
		{
			_locX = 80523;
			_locY = 54741;
			_locZ = -1563;
		}
		else // Hein
		{
			_locX = 110745;
			_locY = 220618;
			_locZ = -3671;
		}
		player.teleToLocation(_locX, _locY, _locZ, false);
	}
	
	private void readConfig()
	{
		try
		{
			/* ----- Файл с параметрами -----*/
			L2Properties Setting = new L2Properties("./config/mods/TeamVsTeam.properties");
			
			/* ----- Чтение параметров ------*/
			if(!Boolean.parseBoolean(Setting.getProperty("TvTEnabled","true")))
			{
				return;
			}
			
			TVT_AURA = Boolean.parseBoolean(Setting.getProperty("TvTAura", "true"));
			TVT_ININSTANCE = Boolean.parseBoolean(Setting.getProperty("TvTInInstance", "true"));
			TVT_JOIN_CURSED = Boolean.parseBoolean(Setting.getProperty("TvTJoinWithCursedWeapon", "false"));
			TVT_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Setting.getProperty("TvTOnStartRemoveAllEffects", "true"));
			TVT_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Setting.getProperty("TvTOnStartUnsummonPet", "true"));
			TVT_CLOSE_COLISEUM_DOORS = Boolean.parseBoolean(Setting.getProperty("TvTCloseColiseumDoors", "true"));
			TVT_ALLOW_INTERFERENCE = Boolean.parseBoolean(Setting.getProperty("TvTAllowInterference", "false"));
			TVT_ALLOW_POTIONS = Boolean.parseBoolean(Setting.getProperty("TvTAllowPotions", "false"));
			TVT_ALLOW_SUMMON = Boolean.parseBoolean(Setting.getProperty("TvTAllowSummon", "true"));
			TVT_REVIVE_RECOVERY = Boolean.parseBoolean(Setting.getProperty("TvTReviveRecovery", "true"));
			TVT_REVIVE_DELAY = Long.parseLong(Setting.getProperty("TvTReviveDelay", "10"));		
			TVT_ALLOW_ENEMY_HEALING = Boolean.parseBoolean(Setting.getProperty("TvTAllowEnemyHealing", "false"));
			TVT_ALLOW_TEAM_CASTING = Boolean.parseBoolean(Setting.getProperty("TvTAllowTeamCasting", "false"));
			TVT_ALLOW_TEAM_ATTACKING = Boolean.parseBoolean(Setting.getProperty("TvTAllowTeamAttacking", "false"));
			TVT_PRICE_NO_KILLS = Boolean.parseBoolean(Setting.getProperty("TvTPriceNoKills", "false"));
			TVT_ORIGINALRETURN = Boolean.parseBoolean(Setting.getProperty("TvTOriginalPosition","false"));
			
			_minlvl = Integer.parseInt(Setting.getProperty("TvTMinLevel", "1"));
			_maxlvl = Integer.parseInt(Setting.getProperty("TvTMaxLevel", "85"));
			_rewardId  = null;
			_rewardAmount = null;
			for(String s : Setting.getProperty("TvTRewardId", "57").split(",")) 
				_rewardId = ArrayUtils.add(_rewardId, Integer.parseInt(s));
			for(String s : Setting.getProperty("TvTRewardAmount", "100000").split(",")) 
				_rewardAmount = ArrayUtils.add(_rewardAmount, Integer.parseInt(s));

			_regTime = Integer.parseInt(Setting.getProperty("TvTJoinTime", "5"));
			_eventTime = Integer.parseInt(Setting.getProperty("TvTEventTime", "15"));
			_minPlayers = Integer.parseInt(Setting.getProperty("TvTMinPlayers", "8"));
			_maxPlayers = Integer.parseInt(Setting.getProperty("TvTMaxPlayers", "60"));
			
			/* ---- Blue Team ------ */
			Team teamBlue = new Team();
			String[] teamLocBlue = Setting.getProperty("BlueTeamLoc", "150545,46734,-3415").split(",");

			teamBlue.name = "Blue";
			teamBlue.index = 1;
			teamBlue.loc = new Location(Integer.parseInt(teamLocBlue[0]), Integer.parseInt(teamLocBlue[1]), Integer.parseInt(teamLocBlue[2]));
			teamBlue.color = 16711680;
			_teams.add(teamBlue);
			
			/* ---- Red Team ------ */
			Team teamRed = new Team();
			String[] teamLocRed = Setting.getProperty("RedTeamLoc", "148386,46747,-3415").split(",");

			teamRed.name = "Red";
			teamRed.index = 2;
			teamRed.loc = new Location(Integer.parseInt(teamLocRed[0]), Integer.parseInt(teamLocRed[1]), Integer.parseInt(teamLocRed[2]));
			teamRed.color = 255;
			_teams.add(teamRed);
		}
		catch(Exception e)
		{
			_log.warn("TvT: Error reading config ", e);
			_instance = null;
		}
	}
	private int getTeam(L2PcInstance pc) {
		for(int i=0;i<_teams.size();i++)
			if(_teams.get(i).players.containsKey(pc.getObjectId()))
					return i;
		return -1;
	}
	public int getRegistredPlayersCount() { 
		return _participants.size();
	}
	
	public String getTitle(L2PcInstance cha, L2PcInstance other) {
		int kills = 0;
		if(_kills.containsKey(cha.getObjectId()))
			kills = _kills.get(cha.getObjectId());
		return "Kills : "+kills;
		
	}
	
	public String getName(L2PcInstance cha, L2PcInstance other) {
		if(cha.getGameEvent() == other.getGameEvent()) {
			int myTeam = getTeam(cha);
			int otherTeam = getTeam(other);
			if(myTeam!=otherTeam)
				return _teams.get(myTeam).name;
		}

		return cha.getName();
	}
	public int getCharTitleColor(L2PcInstance cha, L2PcInstance other) {
		int myTeam = getTeam(cha);
		return _teams.get(myTeam).color;
	}
		
}