package ru.catssoftware.gameserver.instancemanager.clanhallsiege;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallSiege;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.ZoneManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeFlagInstance;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.model.zone.L2ClanhallZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;

/*
 * Author: MHard - L2CatsSoftware DevTeam
 */

public class BanditStrongholdSiege extends ClanHallSiege
{
	protected static Logger 			_log				= Logger.getLogger(BanditStrongholdSiege.class.getName());
	private static BanditStrongholdSiege	_instance;
	private boolean							_registrationPeriod	= false;
	private int								_clanCounter		= 0;
	private Map<Integer, clanPlayersInfo>	_clansInfo			= new FastMap<Integer, clanPlayersInfo>().setShared(true);
	private L2Zone zone											= ZoneManager.getInstance().getZone(L2Zone.ZoneType.Clanhall, "Bandits Stronghold");
	public ClanHall clanhall									= ClanHallManager.getInstance().getClanHallById(35);
	private clanPlayersInfo					_ownerClanInfo		= new clanPlayersInfo();
	private boolean							_finalStage			= false;
	private ScheduledFuture<?>				_midTimer;
	
	public static final BanditStrongholdSiege load()
	{
		_log.info("SiegeManager of Bandits Stronghold");
		if (_instance == null)
			_instance = new BanditStrongholdSiege();
		return _instance;
	}

	public static final BanditStrongholdSiege getInstance()
	{
		if (_instance == null)
			_instance = new BanditStrongholdSiege();
		return _instance;
	}
	private BanditStrongholdSiege()
	{
		long siegeDate=restoreSiegeDate(35);
		Calendar tmpDate=Calendar.getInstance();
		tmpDate.setTimeInMillis(siegeDate);
		setSiegeDate(tmpDate);
		setNewSiegeDate(siegeDate,35,22);
		// Schedule siege auto start
		_startSiegeTask.schedule(1000);
	}
	public void startSiege()
	{
		setRegistrationPeriod(false);
		if(_clansInfo.size()==0)
		{
			endSiege(false);
			return;
		}
		if((_clansInfo.size()==1)&&(clanhall.getOwnerClan()==null))
		{
			endSiege(false);
			return;
		}
		if((_clansInfo.size()==1)&&(clanhall.getOwnerClan()!=null))
		{
			L2Clan clan=null;
			for (clanPlayersInfo a : _clansInfo.values())
				clan=ClanTable.getInstance().getClanByName(a._clanName);
			setIsInProgress(true);
			((L2ClanhallZone)zone).updateSiegeStatus();
			startSecondStep(clan);
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, 30);
			_endSiegeTask.schedule(1000);
			return;
		}
		setIsInProgress(true);
		clanhall.setUnderSiege(true);
		((L2ClanhallZone)zone).updateSiegeStatus();
		spawnFlags();
		gateControl(1);
		anonce("Займите места у своих осадных штабов.",1);
		ThreadPoolManager.getInstance().scheduleGeneral(new startFirstStep(), 5*60000);
		_midTimer=ThreadPoolManager.getInstance().scheduleGeneral(new midSiegeStep(), 25*60000);

		_siegeEndDate = Calendar.getInstance();
		_siegeEndDate.add(Calendar.MINUTE, 60);
		_endSiegeTask.schedule(1000);
	}
	public void startSecondStep(L2Clan winner)
	{
		FastList<String> winPlayers = BanditStrongholdSiege.getInstance().getRegisteredPlayers(winner);
		unSpawnAll();
		_clansInfo.clear();
		clanPlayersInfo regPlayers = new clanPlayersInfo();
		regPlayers._clanName=winner.getName();
		regPlayers._players=winPlayers;
		_clansInfo.put(winner.getClanId(), regPlayers);
		_clansInfo.put(clanhall.getOwnerClan().getClanId(),_ownerClanInfo);
		spawnFlags();
		gateControl(1);
		_finalStage = true;
		anonce("Займите места у своих осадных штабов.",1);
		ThreadPoolManager.getInstance().scheduleGeneral(new startFirstStep(), 5*60000);
	}
	public void endSiege(boolean par)
	{
		_mobControlTask.cancel();
		_finalStage = false;
		if (par)
		{
			L2Clan winner=checkHaveWinner();
			if(winner!=null)
			{
				ClanHallManager.getInstance().setOwner(clanhall.getId(),winner);
				anonce("Внимание !!! Холл Клана, Крепость разбойников был завоеван кланом "+winner.getName(),2);
			}
			else
				anonce("Внимание !!! Холл Клана, Крепость разбойников не получил нового владельца",2);
		}
		setIsInProgress(false);
		clanhall.setUnderSiege(false);
		((L2ClanhallZone)zone).updateSiegeStatus();
		unSpawnAll();
		_clansInfo.clear();
		_clanCounter = 0;
		teleportPlayers();
		setNewSiegeDate(getSiegeDate().getTimeInMillis(),35,22);
		_startSiegeTask.schedule(1000);
	}
	public void unSpawnAll()
	{
		for(String clanName:getRegisteredClans())
		{
			L2Clan clan=ClanTable.getInstance().getClanByName(clanName);
			L2MonsterInstance mob=getQuestMob(clan);
			L2SiegeFlagInstance flag=getSiegeFlag(clan);
			if (mob!=null)
				mob.deleteMe();
			if (flag!=null)
				flag.deleteMe();
		}
	}
	public void gateControl(int val)
	{
		if (val==1)
		{
			DoorTable.getInstance().getDoor(22170001).openMe();
			DoorTable.getInstance().getDoor(22170002).openMe();
			DoorTable.getInstance().getDoor(22170003).closeMe();
			DoorTable.getInstance().getDoor(22170004).closeMe();
		}
		else if (val==2)
		{
			DoorTable.getInstance().getDoor(22170001).closeMe();
			DoorTable.getInstance().getDoor(22170002).closeMe();
			DoorTable.getInstance().getDoor(22170003).closeMe();
			DoorTable.getInstance().getDoor(22170004).closeMe();
		}
	}
	public void teleportPlayers()
	{
		for(L2Character cha : zone.getCharactersInside().values())
			if (cha instanceof L2PcInstance)
			{
				L2Clan clan=((L2PcInstance)cha).getClan();
				if (!isPlayerRegister(clan,cha.getName()))
					cha.teleToLocation(88404, -21821, -2276);
			}
	}
	public L2Clan checkHaveWinner()
	{
		L2Clan res=null;
		int questMobCount=0;
		for(String clanName:getRegisteredClans())
		{
			L2Clan clan=ClanTable.getInstance().getClanByName(clanName);
			if (getQuestMob(clan)!=null)
			{
				res=clan;
				questMobCount++;
			}
		}
		if (questMobCount>1)
			return null;
		return res;
	}
	private class midSiegeStep implements Runnable 
	{
		public void run()
		{
			_mobControlTask.cancel();
			L2Clan winner=checkHaveWinner();
			if(winner!=null)
			{
				if (clanhall.getOwnerClan()==null)
				{
					ClanHallManager.getInstance().setOwner(clanhall.getId(),winner);
					anonce("Внимание !!! Холл Клана, Крепость разбойников был завоеван кланом "+winner.getName(),2);
					endSiege(false);
				}
				else
					startSecondStep(winner);
			}
			else
			{
				endSiege(true);
			}
		}
	}
	private class startFirstStep implements Runnable 
	{
		public void run()
		{
			teleportPlayers();
			gateControl(2);
			int mobCounter=1;
			for(String clanName:getRegisteredClans())
			{
				L2NpcTemplate template;
				L2Clan clan=ClanTable.getInstance().getClanByName(clanName);
				template = NpcTable.getInstance().getTemplate(35427+mobCounter);
				template.setServerSideTitle(true);
				template.setTitle(clan.getName());
				L2MonsterInstance questMob = new L2MonsterInstance(IdFactory.getInstance().getNextId(), template);
				questMob.setHeading(100);
				questMob.getStatus().setCurrentHpMp(questMob.getMaxHp(), questMob.getMaxMp());
				if (mobCounter==1)
					questMob.spawnMe(83752,-17354,-1828);
				else if (mobCounter==2)
					questMob.spawnMe(82018,-15126,-1829);
				else if (mobCounter==3)
					questMob.spawnMe(85320,-16191,-1823);
				else if (mobCounter==4)
					questMob.spawnMe(81522,-16503,-1829);
				else if (mobCounter==5)
					questMob.spawnMe(83786,-15369,-1828);
				clanPlayersInfo regPlayers = _clansInfo.get(clan.getClanId());
				for(String playerName : regPlayers._players) {
					L2PcInstance pc = L2World.getInstance().getPlayer(playerName);
					if(pc==null)
						regPlayers._players.remove(playerName);
					else
						if(!pc.isInsideRadius(regPlayers._flag, 300, false, false)) {
							pc.sendMessage(Message.getMessage(pc, Message.MessageId.MSG_NOT_REAR_FLAG));							
							pc.teleToLocation(88404, -21821, -2276);
							regPlayers._players.remove(playerName);
						}
				}
				regPlayers._mob=questMob;
				mobCounter++;
			}
			_mobControlTask.schedule(3000);
			anonce("Бой начался. Убейте НПЦ противника",1);
		}
	}
	public void spawnFlags()
	{
		int flagCounter=1;
		for(String clanName:getRegisteredClans())
		{
			L2NpcTemplate template;
			L2Clan clan=ClanTable.getInstance().getClanByName(clanName);
			if (clan==clanhall.getOwnerClan())
				template = NpcTable.getInstance().getTemplate(35422);
			else
				template = NpcTable.getInstance().getTemplate(35422+flagCounter);
			L2SiegeFlagInstance flag = new L2SiegeFlagInstance(null, IdFactory.getInstance().getNextId(), template, false,true,clan);
			flag.setTitle(clan.getName());
			flag.setHeading(100);
			flag.getStatus().setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			if (clan==clanhall.getOwnerClan())
				flag.spawnMe(81700,-16300,-1828);
			else
			{
				if (flagCounter==1)
					flag.spawnMe(83452,-17654,-1828);
				else if (flagCounter==2)
					flag.spawnMe(81718,-14826,-1829);
				else if (flagCounter==3)
					flag.spawnMe(85020,-15891,-1823);
				else if (flagCounter==4)
					flag.spawnMe(81222,-16803,-1829);
				else if (flagCounter==5)
					flag.spawnMe(83486,-15069,-1828);
			}
			clanPlayersInfo regPlayers = _clansInfo.get(clan.getClanId());
			regPlayers._flag=flag;
			flagCounter++;
		}
	}
	public void setRegistrationPeriod(boolean par)
	{
		_registrationPeriod = par;
	}
	public boolean isRegistrationPeriod()
	{
		return _registrationPeriod;
	}
	public boolean isPlayerRegister(L2Clan playerClan,String playerName)
	{
		if (playerClan==null)
			return false;
		clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
		if (regPlayers != null)
			if (regPlayers._players.contains(playerName))
				return true;
		return false;
	}
	public boolean isClanOnSiege(L2Clan playerClan)
	{
		if (playerClan==clanhall.getOwnerClan())
			return true;
		clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
		if (regPlayers == null)
		{
			return false;
		}
		return true;
	}
	public synchronized int registerClanOnSiege(L2PcInstance player,L2Clan playerClan)
	{
		if (_clanCounter==5)
			return 2;
		L2ItemInstance item=player.getInventory().getItemByItemId(5009);
		if ((item!=null)&&(player.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false)))
		{
			_clanCounter++;
			clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
			if (regPlayers == null)
			{
				regPlayers = new clanPlayersInfo();
				regPlayers._clanName=playerClan.getName();
				_clansInfo.put(playerClan.getClanId(), regPlayers);
			}
		}
		else
			return 1;
		return 0;
	}
	public boolean unRegisterClan(L2Clan playerClan)
	{
		if(_clansInfo.remove(playerClan.getClanId())!=null)
		{
			_clanCounter--;
			return true;
		}
		return false;
	}
	public FastList<String> getRegisteredClans()
	{
		FastList<String> clans=new FastList<String>();
		for (clanPlayersInfo a : _clansInfo.values())
		{
			clans.add(a._clanName);
		}
		return clans;
	}
	public FastList<String> getRegisteredPlayers(L2Clan playerClan)
	{
		if (playerClan==clanhall.getOwnerClan())
			return _ownerClanInfo._players;
		clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
		if (regPlayers != null)
			return regPlayers._players;
		return null;
	}
	public L2SiegeFlagInstance getSiegeFlag(L2Clan playerClan)
	{
		clanPlayersInfo clanInfo=_clansInfo.get(playerClan.getClanId());
		if(clanInfo!=null)
			return clanInfo._flag;
		return null;
	}
	public L2MonsterInstance getQuestMob(L2Clan clan)
	{
		clanPlayersInfo clanInfo=_clansInfo.get(clan.getClanId());
		if(clanInfo!=null)
			return clanInfo._mob;
		return null;
	}
	public int getPlayersCount(String playerClan)
	{
		for (clanPlayersInfo a : _clansInfo.values())
			if(a._clanName==playerClan)
				return a._players.size();
		return 0;
	}
	public void addPlayer(L2Clan playerClan,String playerName)
	{
		if (playerClan==clanhall.getOwnerClan())
			if (_ownerClanInfo._players.size()<18)
				if (!_ownerClanInfo._players.contains(playerName))
				{
					_ownerClanInfo._players.add(playerName);
					return;
				}
		clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
		if (regPlayers != null)
			if (regPlayers._players.size()<18)
				if (!regPlayers._players.contains(playerName))
					regPlayers._players.add(playerName);
	}
	public void removePlayer(L2Clan playerClan,String playerName)
	{
		if (playerClan==clanhall.getOwnerClan())
			if (_ownerClanInfo._players.contains(playerName))
			{
				_ownerClanInfo._players.remove(playerName);
				return;
			}
		clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
		if (regPlayers != null)
			if (regPlayers._players.contains(playerName))
				regPlayers._players.remove(playerName);
	}
	private final ExclusiveTask _startSiegeTask = new ExclusiveTask(){
		@Override
		protected void onElapsed()
		{
			if (getIsInProgress())
			{
				cancel();
				return;
			}
			Calendar siegeStart=Calendar.getInstance();
			siegeStart.setTimeInMillis(getSiegeDate().getTimeInMillis());
			final long registerTimeRemaining = siegeStart.getTimeInMillis() - System.currentTimeMillis();
			siegeStart.add(Calendar.HOUR, 1);
			final long siegeTimeRemaining = siegeStart.getTimeInMillis() - System.currentTimeMillis();
			long remaining=registerTimeRemaining;
			if (registerTimeRemaining <= 0)
			{
				if (!isRegistrationPeriod())
				{
					if (clanhall.getOwnerClan()!=null)
						_ownerClanInfo._clanName=clanhall.getOwnerClan().getName();
					else
						_ownerClanInfo._clanName="";
					setRegistrationPeriod(true);
					anonce("Внимание !!! Начался период регистрации на осаду Холл Клана , Крепость Разбойников.",2);
					remaining=siegeTimeRemaining;
				}
			}
			if (siegeTimeRemaining <= 0)
			{
				startSiege();
				cancel();
				return;
			}
			schedule(remaining);
		}
	};
	public void anonce(String text,int type)
	{
		if (type==1)
		{
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Shout, "Вестник", text);
			for(String clanName:getRegisteredClans())
			{
				L2Clan clan=ClanTable.getInstance().getClanByName(clanName);
				for(String playerName:getRegisteredPlayers(clan))
				{
					L2PcInstance cha = L2World.getInstance().getPlayer(playerName);
					if (cha!=null)
						cha.sendPacket(cs);
				}
			}
		}
		else
		{
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Shout, "Вестник", text);
			L2MapRegion region = MapRegionManager.getInstance().getRegion(88404, -21821, -2276);
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (region == MapRegionManager.getInstance().getRegion(player.getX(), player.getY(), player.getZ())
						&& (player.getInstanceId() == 0))
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	private final ExclusiveTask _endSiegeTask = new ExclusiveTask() {
		@Override
		protected void onElapsed()
		{
			if (!getIsInProgress())
			{
				cancel();
				return;
			}
			final long timeRemaining = _siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
			if (timeRemaining <= 0)
			{
				endSiege(true);
				cancel();
				return;
			}
			schedule(timeRemaining);
		}
	};
	private final ExclusiveTask _mobControlTask = new ExclusiveTask() {
		@Override
		protected void onElapsed()
		{
			int mobCount=0;
			synchronized(_clansInfo) {
				for(Integer key : _clansInfo.keySet()) {
					clanPlayersInfo cl = _clansInfo.get(key);
					if (cl._mob.isDead())
					{
						L2Clan clan = ClanTable.getInstance().getClanByName(cl._clanName);
						unRegisterClan(clan);
					}
					else
						mobCount++;
				}
			}
			teleportPlayers();
			if (mobCount<2)
				if (_finalStage)
				{
					_siegeEndDate = Calendar.getInstance();
					_endSiegeTask.cancel();
					_endSiegeTask.schedule(5000);
				}
				else
				{
					_midTimer.cancel(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new midSiegeStep(), 5000);
				}
			else
				schedule(3000);
		}
	};

	private class clanPlayersInfo
	{
		public String _clanName;
		public L2SiegeFlagInstance _flag = null;
		public L2MonsterInstance _mob = null;
		public FastList<String> _players	= new FastList<String>();
	}
}