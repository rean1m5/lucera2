package ru.catssoftware.gameserver.model.entity.events.DeathMatch;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.ExShowScreenMessage;
import ru.catssoftware.gameserver.taskmanager.TaskManager;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.ArrayUtils;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author m095
 * @version 1.0
 */

public class DeathMatch extends GameEvent
{
	private FastList<Integer> 	_players 		= new FastList<Integer>();
	private FastMap<Integer, Location> _playerLoc = new FastMap<Integer, Location>();
	private static DeathMatch	_instance		= null;
	public long					_eventDate 		= 0;
	private int					_minLvl 		= 0;
	private int					_maxLvl 		= 0;
	private int 				_maxPlayers 	= 60;
	private int 				_minPlayers 	= 0;
	private int 				_instanceId		= 0;
	private int 				_regTime		= 0;
	private int 				_eventTime		= 0;
	private int 				[]_rewardId		= null;
	private int 				[]_rewardAmount	= null;
	private int					_reviveDelay	= 0;
	private int 				_remaining;

	private boolean ON_START_REMOVE_ALL_EFFECTS;
	private boolean ON_START_UNSUMMON_PET;
	private Location EVENT_LOCATION;
	private boolean RESORE_HP_MP_CP;
	private boolean ALLOW_POTIONS;
	private boolean ALLOW_SUMMON;
	private boolean JOIN_CURSED;
	private boolean ALLOW_INTERFERENCE;
	private boolean RESET_SKILL_REUSE;
	private boolean DM_RETURNORIGINAL;
	

	public static final DeathMatch getInstance()
	{
		if(_instance==null)
			new DeathMatch();
		return _instance;
	}
	
	public String getStatus()
	{
		int free = (_maxPlayers - _players.size());
		if (free < 0)
			free = 0;

		return free + " из " + _maxPlayers;
	}

	public DeathMatch()
	{
		_instance = this;
	}

	@Override
	public boolean finish()
	{
		_eventTask.cancel();
		_registrationTask.cancel();
		L2PcInstance player;
		for (Integer playerId: _players)
		{
				player = L2World.getInstance().findPlayer(playerId);
				if(player != null)
				{
					remove(player);
				}
		}
		if(_eventScript!=null)
			_eventScript.onFinish(_instanceId);
		
		if (_instanceId != 0)
		{
			InstanceManager.getInstance().destroyInstance(_instanceId);
			_instanceId = 0;
		}
		_players.clear();
		setState(State.STATE_OFFLINE);
		return true;
	}

	@Override
	public String getName()
	{
		return "DeathMatch";
	}

	@Override
	public boolean isParticipant(L2PcInstance player)
	{
		return _players.contains(player.getObjectId());
	}

	@Override
	public boolean load()
	{
		try
		{ 
			/* ----- Файл с параметрами -----*/
			L2Properties Setting = new L2Properties("./config/mods/DeathMatch.properties");
			
			/* ----- Чтение параметров ------*/
			if(!Boolean.parseBoolean(Setting.getProperty("DMEnabled","true")))
			{
				_instance = null;
				return false;
			}

			ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Setting.getProperty("OnStartRemoveAllEffects", "true"));
			ON_START_UNSUMMON_PET = Boolean.parseBoolean(Setting.getProperty("OnStartUnsummonPet", "true"));
			DM_RETURNORIGINAL = Boolean.parseBoolean(Setting.getProperty("OriginalPosition","false"));
			RESORE_HP_MP_CP = Boolean.parseBoolean(Setting.getProperty("OnStartRestoreHpMpCp", "false"));
			ALLOW_POTIONS = Boolean.parseBoolean(Setting.getProperty("AllowPotion", "false"));
			ALLOW_SUMMON = Boolean.parseBoolean(Setting.getProperty("AllowSummon", "false"));
			JOIN_CURSED = Boolean.parseBoolean(Setting.getProperty("CursedWeapon", "false"));
			ALLOW_INTERFERENCE = Boolean.parseBoolean(Setting.getProperty("AllowInterference", "false"));
			RESET_SKILL_REUSE = Boolean.parseBoolean(Setting.getProperty("ResetAllSkill", "false"));
			EVENT_LOCATION = new Location(Setting.getProperty("EventLocation","149800 46800 -3412"));
			
			_reviveDelay = Integer.parseInt(Setting.getProperty("ReviveDelay", "10"));
			_regTime  = Integer.parseInt(Setting.getProperty("RegTime", "10"));
			_eventTime = Integer.parseInt(Setting.getProperty("EventTime", "10"));
			_rewardId  = null;
			_rewardAmount = null;
			
			for(String s : Setting.getProperty("RewardItem", "57").split(","))
				_rewardId = ArrayUtils.add(_rewardId, Integer.parseInt(s));
			for(String s : Setting.getProperty("RewardItemCount", "50000").split(","))
				_rewardAmount = ArrayUtils.add(_rewardAmount, Integer.parseInt(s));
			_minPlayers = Integer.parseInt(Setting.getProperty("MinPlayers", "2"));
			_maxPlayers = Integer.parseInt(Setting.getProperty("MaxPlayers", "60"));
			_minLvl = Integer.parseInt(Setting.getProperty("MinLevel", "1"));
			_maxLvl = Integer.parseInt(Setting.getProperty("MaxLevel", "90"));
		}
		catch(Exception e)
		{
			_log.warn("DeathMatch: Error reading config ", e);
			return false;
		}

		TaskManager.getInstance().registerTask(new TaskStartDM());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new VoiceDeathMatch());
		return true;
	}

	@Override
	public void onCommand(L2PcInstance actor, String command, String params)
	{
		if(isState(State.STATE_ACTIVE))
		{
			if(command.equals("join")) 
			{
				if(!register(actor))
					actor.sendMessage(Message.getMessage(actor, Message.MessageId.MSG_EVENT_CANT_REGISTERED));
			}
			else if(command.equals("leave"))
				remove(actor);
		}
	}

	@Override
	public void onKill(L2Character killer, L2Character victim)
	{
		if(killer==null || victim ==null)
			return;
		
		if (killer instanceof L2PcInstance && victim instanceof L2PcInstance)
		{
			L2PcInstance plk = (L2PcInstance)killer;
			L2PcInstance pld = (L2PcInstance)victim;
			
			if (plk != null && plk.getGameEvent() == this && pld != null && pld.getGameEvent() == this)
			{
				plk.setDmKills(plk.getDmKills() +1);
				plk.setTitle(String.format(Message.getMessage(null, MessageId.MSG_EVT_12), plk.getDmKills()));
				pld.sendMessage(Message.getMessage(pld, Message.MessageId.MSG_EVENT_WAIT_FOR_RES));
				ThreadPoolManager.getInstance().scheduleGeneral(new revivePlayer(victim), _reviveDelay * 1000);
			}
		}
		
	}

	@Override
	public boolean onNPCTalk(L2NpcInstance npc, L2PcInstance talker)
	{
		return false;
	}

	@Override
	public boolean register(L2PcInstance player)
	{
		if(!canRegister(player))
			return false;

		_players.add(player.getObjectId());
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
				if(player.isDead())
				{
					player.doRevive();
				}
				player.setInstanceId(0);
				player.setDmKills(0);
				if(!DM_RETURNORIGINAL)
					randomTeleport(player);
				else
					player.teleToLocation(_playerLoc.get(player.getObjectId()),false);
			}
			player.setGameEvent(null);
			_players.remove((Integer)player.getObjectId());
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
		if(!Config.Allow_Same_HWID_On_Events && player.getHWid()!=null && player.getHWid().length()!=0) {
			L2PcInstance pc = null;
			for(int charId : _players) {
				pc = L2World.getInstance().getPlayer(charId);
				if(pc !=null && player.getHWid().equals(pc.getHWid())) {
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_HWID_ALREADY_REGISTERED));
					return false;
				}
			}
		}
		if(!Config.Allow_Same_IP_On_Events) {
			L2PcInstance pc = null;
			for(int charId : _players) {
				pc = L2World.getInstance().getPlayer(charId);
				if(pc !=null && pc.getClient()!=null && player.getClient().getHostAddress().equals(pc.getClient().getHostAddress())) {
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_HWID_ALREADY_REGISTERED));
					return false;
				}
			}
		}
		
		if (_players.size() >= _maxPlayers)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_FULL));
			return false;
		}
		if (player.isCursedWeaponEquipped() && !JOIN_CURSED)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CURSED_WEAPON_NOT_ALLOW));
			return false;
		}
		if (player.getLevel() > _maxLvl || player.getLevel() < _minLvl)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_WRONG_LEVEL));
			return false;
		}
		if (!player.canRegisterToEvents())
			return false;
		return true;
	}

	@Override
	public boolean start()
	{
		_players.clear();

		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_1),getName()));
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_2),getName(),_minLvl,_maxLvl));
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_3),getName()));
		for(int i=0;i<_rewardId.length;i++) 
			AnnounceToPlayers(true, " - "+_rewardAmount[i]+" "+ItemTable.getInstance().getTemplate(_rewardId[i]).getName());
		AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_4), getName(),_regTime));

		setState(State.STATE_ACTIVE);
		_remaining = (_regTime * 60000) / 2; 
		_registrationTask.schedule(_remaining);
		return true;
	}
	
	@Override
	public boolean canInteract(L2Character actor, L2Character target)
	{
		return !isRunning() || (actor.getGameEvent() == target.getGameEvent() && actor.getGameEvent() == this) || ALLOW_INTERFERENCE;
	}

	@Override
	public boolean canAttack(L2Character attacker, L2Character target)
	{
		if(isRunning())
			return attacker.getGameEvent() == target.getGameEvent() && attacker.getGameEvent() == this;

		return false;
	}

	@Override
	public boolean canBeSkillTarget(L2Character caster, L2Character target, L2Skill skill)
	{
		if(isRunning())
			return false;

		return true;
	}

	@Override
	public boolean canUseItem(L2Character actor, L2ItemInstance item)
	{
		if(isRunning())
		{
			if(item.getItem().getItemType() == L2EtcItemType.POTION )
				return ALLOW_POTIONS;
			else
			{
				int itemId = item.getItemId();
				return !((itemId == 3936 || itemId == 3959 || itemId == 737 || itemId == 9157 || itemId == 10150 || itemId == 13259));
			}
			
		}
		return true;
	}

	@Override
	public boolean canUseSkill(L2Character caster, L2Skill skill)
	{
		if(isRunning())
		{
			if(skill.getTargetType() == SkillTargetType.TARGET_PET || skill.getTargetType() == SkillTargetType.TARGET_SELF)
				return true;
			else if(skill.getSkillType() == L2SkillType.SUMMON)
				return ALLOW_SUMMON;
			else if(skill.getSkillType() == L2SkillType.HEAL || skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.MANAHEAL)  
				return caster.getTarget() == caster;
		}
		return true;
	}

	@Override
	public void onRevive(L2Character actor)
	{
		if(RESORE_HP_MP_CP && isRunning())
		{
			actor.getStatus().setCurrentCp(actor.getMaxCp());
			actor.getStatus().setCurrentHp(actor.getMaxHp());
			actor.getStatus().setCurrentMp(actor.getMaxMp());
		}
	}
	
	@Override
	public void onLogin(L2PcInstance player)
	{
		if(isRunning())
			remove(player);
	}

	/* Приватные методы эвента */
	public void AnnounceToPlayers(Boolean toall, String announce)
	{
		if (toall)
			Announcements.getInstance().criticalAnnounceToAll(announce);
		else
		{
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Critical_Announce, "", announce);
			L2PcInstance player;
			if (_players != null && !_players.isEmpty())
			{
				for (Integer playerid : _players)
				{
					player = L2World.getInstance().findPlayer(playerid);
					if (player != null && player.isOnline() != 0)
						player.sendPacket(cs);
				}
			}
		}
	}

	private final ExclusiveTask _registrationTask = new ExclusiveTask()
	{
		private boolean showed; 
		@Override
		protected void onElapsed()
		{
			if(_remaining < 1000)
			{
				run();
			}
			else
			{
				if(_remaining >= 60000)
					AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_5), getName(),_remaining/60000));
				else if(!showed) {
					AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_6), getName()));
					showed = true;
				}
				_remaining /= 2;
				schedule(_remaining);
			}
		}
	};
	
	private Runnable TeleportTask = new Runnable()
	{
		@Override
		public void run()
		{
			L2PcInstance player;
			int[] par = {-1, 1};
			int Radius = 500;

			for(Integer playerId: _players)
			{
				player = L2World.getInstance().findPlayer(playerId);
				if(player!=null)
				{
					player.abortAttack();
					player.abortCast();
					player.setTarget(null);
					if (RESET_SKILL_REUSE)
						player.resetSkillTime(true);
					if(ON_START_REMOVE_ALL_EFFECTS)
						player.stopAllEffects();
					if(player.getPet()!=null)
					{
						player.getPet().abortAttack();
						player.getPet().abortCast();
						player.getPet().setTarget(null);
						if(ON_START_REMOVE_ALL_EFFECTS)
							player.getPet().stopAllEffects();
						if(ON_START_UNSUMMON_PET)
							player.getPet().unSummon(player);
					}
					if(player.getParty()!=null)
						player.getParty().removePartyMember(player);
					player.setInstanceId(_instanceId);
					
					player.teleToLocation(EVENT_LOCATION.getX()+(par[Rnd.get(2)]*Rnd.get(Radius)), EVENT_LOCATION.getY()+(par[Rnd.get(2)]*Rnd.get(Radius)), EVENT_LOCATION.getZ());
					player.setDmKills(0);
					player.setTitle(String.format(Message.getMessage(null, MessageId.MSG_EVT_12),0));
					SkillTable.getInstance().getInfo(4515, 1).getEffects(player, player);
					player.sendPacket(new ExShowScreenMessage("1 minutes until event start, wait", 10000));
				}
			}

			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					L2PcInstance player;
					for(Integer playerId: _players)
					{
						player = L2World.getInstance().findPlayer(playerId);
						if(player!=null) 
							player.stopAllEffects();
					}
					AnnounceToPlayers(false, String.format(Message.getMessage(null, MessageId.MSG_EVT_10), getName()));
					_remaining = _eventTime * 60000;
					_eventTask.schedule(10000);
				}
			}, 60000);
		}
	};

	private final ExclusiveTask _eventTask = new ExclusiveTask()
	{
		@Override
		protected void onElapsed()
		{
			_remaining -= 10000;
			if(_remaining <= 0)
			{
				rewardPlayers();
				return;
			}
			_eventTask.schedule(10000);
		}
	};

	
	private class revivePlayer implements Runnable
	{
		L2Character _player;
		public revivePlayer(L2Character player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			if(_player != null)
			{
				int[] par = {-1, 1};
				int Radius = 500;

				_player.teleToLocation(149800+(par[Rnd.get(2)]*Rnd.get(Radius)), 46800+(par[Rnd.get(2)]*Rnd.get(Radius)), -3412);
				_player.doRevive();
			}
		}
	}

	private void rewardPlayers()
	{
		L2PcInstance player = null;
		L2PcInstance winner	= null;
		int	top_score = 0;

		for(Integer playerId : _players)
		{
			player = L2World.getInstance().findPlayer(playerId);
			if(player != null)
			{
				player.abortAttack();
				player.abortCast();
				player.setTarget(null);
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				
				if (player.getDmKills() == top_score && top_score > 0)
					winner = null;
				
				if (player.getDmKills() > top_score)
				{
					winner = player;
					top_score = player.getDmKills();
				}
			}
		}

		if(winner != null && winner.getDmKills() > 0)
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_11), getName(),winner.getName()));
			for(int i=0;i<_rewardId.length;i++)
				winner.addItem("DM Reward", _rewardId[i], _rewardAmount[i], null, true);
		}
		else 
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_8), getName()));
		}
			
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			public void run()
			{
				finish();
			}
		}, 10000);
		
		player = null;
		winner = null;
	}

	private void run()
	{
		int realPlayers = 0;
		_playerLoc.clear();
		L2PcInstance player;
		for(Integer playerId: _players)
		{
			player = L2World.getInstance().findPlayer(playerId);
			if(player!=null && player.getLevel() >= _minLvl && player.getLevel() <= _maxLvl && player.getInstanceId()==0)
			{
				if(!DM_RETURNORIGINAL)
					player.setIsIn7sDungeon(false);
				else
					_playerLoc.put(playerId,new Location(player.getLoc()));
				realPlayers++;
			}
			else {
				if(player!=null)
					player.setGameEvent(null);
				_players.remove(playerId);
			}
		}
		if(realPlayers < _minPlayers)
		{
			AnnounceToPlayers(true, String.format(Message.getMessage(null, MessageId.MSG_EVT_9), getName()));
			finish();
			return;
		}

		_instanceId = InstanceManager.getInstance().createDynamicInstance(null);
		Instance eventInst = InstanceManager.getInstance().getInstance(_instanceId);
		eventInst.setReturnTeleport(146353, 46709, -3435);
		eventInst.addDoor(24190001, false);
		eventInst.addDoor(24190002, false);
		eventInst.addDoor(24190003, false);
		eventInst.addDoor(24190004, false);
		ThreadPoolManager.getInstance().scheduleGeneral(TeleportTask, 10000);
		setState(State.STATE_RUNNING);
		if(_eventScript!=null)
			_eventScript.onStart(_instanceId);
	
	}

	/**
	 * Метод рандомного возврата игроков в города
	 * Выбор состоит из 5 городов
	 */
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
	public int getRegistredPlayersCount() {
		return _players.size();
	}
}
