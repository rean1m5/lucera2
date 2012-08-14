package ru.catssoftware.gameserver.instancemanager.clanhallsiege;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;


import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallSiege;
import ru.catssoftware.gameserver.instancemanager.EventsDropManager;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.EventsDropManager.ruleType;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2ChestInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2HotSpringSquashInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;
import javolution.util.FastList;


public class RainbowSpringSiege extends ClanHallSiege
{
	protected static Logger 			_log				= Logger.getLogger(RainbowSpringSiege.class.getName());
	
	private static RainbowSpringSiege		_instance;
	private boolean							_registrationPeriod	= false;
	public ClanHall _clanhall = ClanHallManager.getInstance().getClanHallById(62);
	private Map<Integer, clanPlayersInfo>		_clansInfo		= new HashMap<Integer, clanPlayersInfo>();
	private L2NpcInstance[]	eti = new L2NpcInstance[]{null,null,null,null};
	private int[] potionsApply = {0,0,0,0};
	private FastList<Integer> potionsDefault= new FastList<Integer>();
	private L2HotSpringSquashInstance[]	squash = new L2HotSpringSquashInstance[]{null,null,null,null};
	private int[] arenaChestsCnt = {0,0,0,0};
	private int currArena;
	private FastList<Integer> _playersOnArena = new FastList<Integer>();
	private L2NpcInstance teleporter;
	private ScheduledFuture<?> _chestsSpawnTask;
	private int teamWiner = -1;
	
	private FastList<L2ChestInstance> arena1chests = new FastList<L2ChestInstance>();
	private FastList<L2ChestInstance> arena2chests = new FastList<L2ChestInstance>();
	private FastList<L2ChestInstance> arena3chests = new FastList<L2ChestInstance>();
	private FastList<L2ChestInstance> arena4chests = new FastList<L2ChestInstance>();
	
	private int _skillsId[] =		{1086,1204,1059,1085,1078,1068,1240,1077,1242,1062}; 
	private int _skillsLvl[] =		{2,2,3,3,6,3,3,3,3,2};
	
	public static final RainbowSpringSiege load()
	{
		_log.info("SiegeManager of Rainbow Springs Chateau");
		if (_instance == null)
			_instance = new RainbowSpringSiege();
		return _instance;
	}

	public static final RainbowSpringSiege getInstance()
	{
		if (_instance == null)
			_instance = new RainbowSpringSiege();
		return _instance;
	}	
	
	private RainbowSpringSiege()
	{
		long siegeDate=restoreSiegeDate(62);
		Calendar tmpDate=Calendar.getInstance();
		tmpDate.setTimeInMillis(siegeDate);
		setSiegeDate(tmpDate);
		setNewSiegeDate(siegeDate,62,22);
		// Schedule siege auto start
		_startSiegeTask.schedule(1000);
	}
	public void startSiege()
	{
		if(_startSiegeTask.isScheduled())
			_startSiegeTask.cancel();
		if(_clansInfo.size()==0)
		{
			endSiege(false);
			return;
		}
		if (_clansInfo.size()>4)
		{
			for (int x = 1;x < _clansInfo.size()-4; x++)
			{
				clanPlayersInfo minClan=null;
				int minVal = Integer.MAX_VALUE;
				for (clanPlayersInfo cl: _clansInfo.values())
				{
					if(cl._decreeCnt<minVal)
					{
						minVal=cl._decreeCnt;
						minClan=cl;
					}
				}
				_clansInfo.remove(minClan);
			}
		}
		else if (_clansInfo.size()<2)
		{
			shutdown();
			anonce("Внимание !!! Холл Клана, Дворец Радужных Источников не получил нового владельца");			
			endSiege(false);
			return;
		}
		for (L2Spawn sp : SpawnTable.getInstance().getAllTemplates().values())
		{
			if (sp.getTemplate().getNpcId() == 35603)
				teleporter = sp.getLastSpawn();
		}
		teamWiner = -1;
		currArena=0;
		setIsInProgress(true);		
		anonce("Внимание !!! соревнование за Холл Клана, Дворец Радужных Источников начнется через 5 минут.");
		anonce("Внимание !!! представителям кланов необходимо войти на арену.");
		for (clanPlayersInfo cl: _clansInfo.values())
		{
			L2Clan clan = ClanTable.getInstance().getClanByName(cl._clanName);
			L2PcInstance clanLeader = clan.getLeader().getPlayerInstance();
			if (clanLeader!=null)
				clanLeader.sendMessage(Message.getMessage(clanLeader, Message.MessageId.MSG_RAINBOW_GO_TO_ARENA));
		}
		
		_firstStepSiegeTask.schedule(60000*5);		
		_siegeEndDate = Calendar.getInstance();
		_siegeEndDate.add(Calendar.MINUTE, 65);
		_endSiegeTask.schedule(1000);
	}
	
	public void startFirstStep()
	{
		potionsDefault= new FastList<Integer>();
		L2NpcTemplate template;
		template = NpcTable.getInstance().getTemplate(35596);
		
		for (int x=0;x<=3;x++)
		{
			eti[x] = new L2NpcInstance(IdFactory.getInstance().getNextId(),template);
			eti[x].getStatus().setCurrentHpMp(eti[x].getMaxHp(), eti[x].getMaxMp());
			potionsDefault.add(x+1);
		}
		eti[0].spawnMe(153129, -125337, -2221);
		eti[1].spawnMe(153884, -127534, -2221);
		eti[2].spawnMe(151560, -127075, -2221);
		eti[3].spawnMe(156657, -125753, -2221);
		template = NpcTable.getInstance().getTemplate(35588);
		for (int x=3;x>=0;x--)
		{
			squash[x] = new L2HotSpringSquashInstance(IdFactory.getInstance().getNextId(),template);
			squash[x].getStatus().setCurrentHpMp(squash[x].getMaxHp(), squash[x].getMaxMp());
			potionsApply[x] = potionsDefault.remove(Rnd.get(0,x));
		}
		squash[0].spawnMe(153129+50, -125337+50, -2221);
		squash[1].spawnMe(153884+50, -127534+50, -2221);
		squash[2].spawnMe(151560+50, -127075+50, -2221);
		squash[3].spawnMe(156657+50, -125753+50, -2221);
		
		int mobs[]={35593};
		int item[]={8035,8037,8039,8040,8046,8047,8050,8051,8052,8053,8054};
		int cnt[]={1,1,1,1,1,1,1,1,1,1,1};
		int chance[]={400,400,400,400,400,400,400,400,400,400,400};
		EventsDropManager.getInstance().addRule("RainbowSpring", ruleType.BY_NPCID,mobs,item,cnt,chance,false);		
		_chestsSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ChestsSpawn(), 5000, 5000);
	}
	
	public void chestDie(L2Character killer, L2ChestInstance chest)
	{
		if (arena1chests.contains(chest))
		{
			arenaChestsCnt[0]--;
			arena1chests.remove(chest);
		}
		if (arena2chests.contains(chest))
		{
			arenaChestsCnt[1]--;
			arena2chests.remove(chest);
		}
		if (arena3chests.contains(chest))
		{
			arenaChestsCnt[2]--;
			arena3chests.remove(chest);
		}
		if (arena4chests.contains(chest))
		{
			arenaChestsCnt[3]--;
			arena4chests.remove(chest);
		}
	}

	public void exchangeItem(L2PcInstance player,int val)
	{
		if (val==1) //WATERS
		{
			if (player.destroyItemByItemId("Quest", 8054, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8035, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8052, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8039, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8050, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8051, 1, player, true))
			{
				L2ItemInstance item = player.getInventory().addItem("Quest", 8032, 1, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_QUEST_ITEMS));
				return;
			}
		}
		if (val==2) //WATERS
		{
			if (player.destroyItemByItemId("Quest", 8054, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8035, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8052, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8039, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8050, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8051, 1, player, true))
			{
				L2ItemInstance item = player.getInventory().addItem("Quest", 8031, 1, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_QUEST_ITEMS));
				return;
			}
		}

		if (val==3) //NECTAR
		{
			if (player.destroyItemByItemId("Quest", 8047, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8039, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8037, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8052, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8035, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8050, 1, player, true))
			{
				L2ItemInstance item = player.getInventory().addItem("Quest", 8030, 1, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_QUEST_ITEMS));
				return;
			}
		}
		if (val==4) //SULFUR
		{
			if (player.destroyItemByItemId("Quest", 8051, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8053, 2, player, true)&&
					player.destroyItemByItemId("Quest", 8046, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8040, 1, player, true)&&
					player.destroyItemByItemId("Quest", 8050, 1, player, true))
			{
				L2ItemInstance item = player.getInventory().addItem("Quest", 8033, 1, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_QUEST_ITEMS));
				return;
			}
		}
	}	

	public synchronized void onDieSquash(L2HotSpringSquashInstance par)
	{
		if (!getIsInProgress())
			return;
		for (int x=0 ; x<squash.length ; x++)
		{
			if (squash[x]==par)
				teamWiner = x;				
		}
		if (teamWiner>=0)
		{
			anonce("Внимание !!! Один из участников соревнований, успешно справился с испытанием.");
			anonce("О результатах соревнований за обладание Холл Кланом Горячих источников будет сообщено через 2 минуты.");
			setIsInProgress(false);
			unspawnQusetNPC();
			_endSiegeTask.cancel();
			ThreadPoolManager.getInstance().scheduleGeneral(new EndSiegeTaks(), 1000*60*2);
		}
	}
	
	private void unspawnQusetNPC()
	{
		if(_chestsSpawnTask!=null)
			_chestsSpawnTask.cancel(true);
		for (L2ChestInstance ch: arena1chests)
			ch.deleteMe();
		for (L2ChestInstance ch: arena2chests)
			ch.deleteMe();
		for (L2ChestInstance ch: arena3chests)
			ch.deleteMe();
		for (L2ChestInstance ch: arena4chests)
			ch.deleteMe();
		for(int x=0;x<4;x++)
		{
			if (squash[x] != null)
				squash[x].deleteMe();
		}
	}
	
	public boolean usePotion(L2PlayableInstance activeChar,int potionId)
	{
		if (activeChar instanceof L2PcInstance
				&& isPlayerInArena((L2PcInstance)activeChar)
				&& (activeChar.getTarget() instanceof L2NpcInstance)
				&& (((L2NpcInstance)activeChar.getTarget()).getTemplate().getIdTemplate()==35596))
		{
			int action = 0;
			switch(potionId)
			{
			case 8030:
				action = potionsApply[0];
				break;
			case 8031:
				action = potionsApply[1];
				break;
			case 8032:
				action = potionsApply[2];
				break;
			case 8033:
				action = potionsApply[3];
				break;
			}
			if (action == 0)
				return false;
			L2Clan plClan = ((L2PcInstance)activeChar).getClan();
			if (plClan == null)
				return false;
			int playerArena = -1;			
			for(clanPlayersInfo cl:_clansInfo.values())
			{
				if (plClan.getName().equalsIgnoreCase(cl._clanName))
					playerArena = cl._arenaNumber;
			}
			if (playerArena == -1)
				return false;
			L2PcInstance player = (L2PcInstance)activeChar;
			if (action == 1) // Урон тыкве
			{
				// уменьшаем HP тыквы на 5 - 15 %
				double damage = squash[playerArena].getMaxHp()/100*Rnd.get(5, 15);
				squash[playerArena].reduceCurrentHp(damage, activeChar);
				activeChar.sendMessage(Message.getMessage(player, Message.MessageId.MSG_RAINBOW_GIVE_DAMMAGE));
			}
			else if (action == 2) // Хилл тыкву
			{
				// восстанавливаем HP тыквы на 5 - 15 %				
				double hp = squash[playerArena].getMaxHp()/100*Rnd.get(5, 15);
				squash[playerArena].getStatus().increaseHp(hp);
				activeChar.sendMessage(Message.getMessage(player, Message.MessageId.MSG_RAINBOW_HEAl));
			}
			else if (action == 3) // Дебаф противнику 
			{
				int rndArena = Rnd.get(0, _clansInfo.size()-1);
				String clName="";
				if (rndArena == playerArena)
					rndArena++;
				if (rndArena>_clansInfo.size()-1)
					rndArena=0;
				for(clanPlayersInfo cl:_clansInfo.values())
				{
					if (cl._arenaNumber == rndArena)
						clName = cl._clanName;
				}
				for (int id : _playersOnArena)
				{
					L2PcInstance pl = L2World.getInstance().findPlayer(id);
					if(pl!=null && pl.getClan().getName().equalsIgnoreCase(clName))
						skillsControl(pl);
				}
				activeChar.sendMessage(Message.getMessage(player, Message.MessageId.MSG_RAINBOW_DEBAFF));
			}
			else if (action == 3) // Хилл чужую тыкву
			{
				// восстанавливаем HP тыквы на 5 - 15 %				
				int rndArena = Rnd.get(0, _clansInfo.size()-1);
				if (rndArena == playerArena)
					rndArena++;
				if (rndArena>_clansInfo.size()-1)
					rndArena=0;
				double hp = squash[rndArena].getMaxHp()/100*Rnd.get(5, 15);				
				squash[rndArena].getStatus().increaseHp(hp);
				activeChar.sendMessage(Message.getMessage(player, Message.MessageId.MSG_RAINBOW_HEAL_OTHER));
			}
			return true;
		}
		return false;
	}
	
	private void skillsControl(L2PcInstance pl)
	{
		if (pl==null)
			return ;
		int x = Rnd.get(0, _skillsId.length-1);
		L2Skill skill = SkillTable.getInstance().getInfo(_skillsId[x],_skillsLvl[x]);
		if (skill != null)
			skill.getEffects(pl, pl);
	}	

	private final class EndSiegeTaks implements Runnable
	{
		public void run()
		{
			endSiege(true);
		}
	}
	
	private final class ChestsSpawn implements Runnable
	{
		public void run()
		{
			if (arenaChestsCnt[0]<4)
			{
				L2NpcTemplate template;
				template = NpcTable.getInstance().getTemplate(35593);
				L2ChestInstance newChest = new L2ChestInstance(IdFactory.getInstance().getNextId(),template);
				newChest.getStatus().setCurrentHpMp(newChest.getMaxHp(), newChest.getMaxMp());
				newChest.spawnMe(153129+Rnd.get(-400, 400), -125337+Rnd.get(-400, 400), -2221);
				newChest.setSpecialDrop();				
				arena1chests.add(newChest);
				arenaChestsCnt[0]++;
			}
			if (arenaChestsCnt[1]<4)
			{
				L2NpcTemplate template;
				template = NpcTable.getInstance().getTemplate(35593);
				L2ChestInstance newChest = new L2ChestInstance(IdFactory.getInstance().getNextId(),template);
				newChest.getStatus().setCurrentHpMp(newChest.getMaxHp(), newChest.getMaxMp());
				newChest.spawnMe(153884+Rnd.get(-400, 400), -127534+Rnd.get(-400, 400), -2221);
				newChest.setSpecialDrop();				
				arena2chests.add(newChest);
				arenaChestsCnt[1]++;
			}
			if (arenaChestsCnt[2]<4)
			{
				L2NpcTemplate template;
				template = NpcTable.getInstance().getTemplate(35593);
				L2ChestInstance newChest = new L2ChestInstance(IdFactory.getInstance().getNextId(),template);
				newChest.getStatus().setCurrentHpMp(newChest.getMaxHp(), newChest.getMaxMp());
				newChest.spawnMe(151560+Rnd.get(-400, 400), -127075+Rnd.get(-400, 400), -2221);
				newChest.setSpecialDrop();				
				arena3chests.add(newChest);
				arenaChestsCnt[2]++;
			}
			if (arenaChestsCnt[3]<4)
			{
				L2NpcTemplate template;
				template = NpcTable.getInstance().getTemplate(35593);
				L2ChestInstance newChest = new L2ChestInstance(IdFactory.getInstance().getNextId(),template);
				newChest.getStatus().setCurrentHpMp(newChest.getMaxHp(), newChest.getMaxMp());
				newChest.spawnMe(155657+Rnd.get(-400, 400), -125753+Rnd.get(-400, 400), -2221);
				newChest.setSpecialDrop();				
				arena4chests.add(newChest);
				arenaChestsCnt[3]++;
			}
			
		}		
	}	
	
	public void endSiege(boolean par)
	{
		if (!par)
		{
			setIsInProgress(false);
			unspawnQusetNPC();
			anonce("Осада Холл Клана: " + _clanhall.getName() + " окончена.");
			anonce("Владелец Холл Клана остался прежний");
		}
		else
		{
			for (clanPlayersInfo ci : _clansInfo.values())
			{
				if(ci!=null)
				{
					if(ci._arenaNumber==teamWiner)
					{
						L2Clan clan = ClanTable.getInstance().getClanByName(ci._clanName);
						if(clan!=null)
						{
							ClanHallManager.getInstance().setOwner(_clanhall.getId(), clan);
							anonce("Осада Холл Клана: " + _clanhall.getName() + " окончена.");
							anonce("Владельцем Холл Клана стал "+clan.getName());
						}
					}
				}
			}
		}
		_clansInfo.clear();
		for (int id : _playersOnArena)
		{
			L2PcInstance pl = L2World.getInstance().findPlayer(id);
			if (pl!=null)
				pl.teleToLocation(150717, -124818, -2355);
		}
		_playersOnArena = new FastList<Integer>();
		setNewSiegeDate(getSiegeDate().getTimeInMillis(),62,22);
		_startSiegeTask.schedule(1000);
	}	
	public boolean isRegistrationPeriod()
	{
		return _registrationPeriod;
	}
	public void setRegistrationPeriod(boolean par)
	{
		_registrationPeriod = par;
	}
	public boolean isClanOnSiege(L2Clan playerClan)
	{
		if (playerClan==_clanhall.getOwnerClan())
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
		L2ItemInstance item=player.getInventory().getItemByItemId(8034);
		int itemCnt=0;
		if (item!=null)
		{
			itemCnt = item.getCount();
			if (player.destroyItem("RegOnSiege", item.getObjectId(), itemCnt, player, true))
			{
				_log.info("Rainbow Springs Chateau: registered clan "+playerClan.getName()+" get: "+itemCnt+" decree.");
				clanPlayersInfo regPlayers = _clansInfo.get(playerClan.getClanId());
				if (regPlayers == null)
				{
					regPlayers = new clanPlayersInfo();
					regPlayers._clanName=playerClan.getName();
					regPlayers._decreeCnt=itemCnt;
					_clansInfo.put(playerClan.getClanId(), regPlayers);
				}
			}
		}
		else
			return 0;
		return itemCnt;
	}
	
	public boolean isPlayerInArena(L2PcInstance pl)
	{
		if (_playersOnArena.contains(pl.getObjectId()))
			return true;
		return false;
	}
	
	public void removeFromArena(L2PcInstance pl)
	{
		if (_playersOnArena.contains(pl.getObjectId()))
			pl.teleToLocation(150717, -124818, -2355);
	}
	
	public synchronized boolean enterOnArena(L2PcInstance pl)
	{
		L2Clan clan = pl.getClan();
		L2Party party = pl.getParty(); 
		if (clan==null || party==null)
			return false;
		if (!isClanOnSiege(clan)
				|| !getIsInProgress()
				|| currArena>3
				|| !pl.isClanLeader()
				|| party.getMemberCount()<5)
			return false;
		
		clanPlayersInfo ci = _clansInfo.get(clan.getClanId());
		if (ci==null)
			return false;
		for (L2PcInstance pm : party.getPartyMembers())
		{
			if (pm==null || pm.getRangeToTarget(teleporter)>500)
				return false;
		}
			
		ci._arenaNumber = currArena;
		currArena++;
		
		for (L2PcInstance pm : party.getPartyMembers())
		{
			if(pm.getPet()!=null)
				pm.getPet().unSummon(pm);
			_playersOnArena.add(pm.getObjectId());
			
			switch (ci._arenaNumber)
			{
			case 0:
				pm.teleToLocation(153129+Rnd.get(-400, 400), -125337+Rnd.get(-400, 400), -2221);
				break;
			case 1:
				pm.teleToLocation(153884+Rnd.get(-400, 400), -127534+Rnd.get(-400, 400), -2221);				
				break;
			case 2:
				pm.teleToLocation(151560+Rnd.get(-400, 400), -127075+Rnd.get(-400, 400), -2221);				
				break;
			case 3:
				pm.teleToLocation(155657+Rnd.get(-400, 400), -125753+Rnd.get(-400, 400), -2221);				
				break;
			}
		}
		return true;
	}
	
	public synchronized boolean unRegisterClan(L2PcInstance player)
	{
		L2Clan playerClan = player.getClan();
		if(_clansInfo.containsKey(playerClan.getClanId()))
		{
			int decreeCnt = _clansInfo.get(playerClan.getClanId())._decreeCnt/2;
			if (decreeCnt>0)
			{
				L2ItemInstance item = player.getInventory().addItem("UnRegOnSiege", 8034, decreeCnt, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}	
			return true;
		}
		return false;		
	}
	public void anonce(String text)
	{
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Shout, "Вестник", text);
			L2MapRegion region = MapRegionManager.getInstance().getRegion(143944, -119196, -2136);
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (region == MapRegionManager.getInstance().getRegion(player.getX(), player.getY(), player.getZ())
						&& (player.getInstanceId() == 0))
				{
					player.sendPacket(cs);
				}
			}
	}

	public void shutdown()
	{
		if (isRegistrationPeriod())
		{
			for(clanPlayersInfo cl:_clansInfo.values())
			{
				L2Clan clan = ClanTable.getInstance().getClanByName(cl._clanName);
				if (clan!=null && cl._decreeCnt>0)
				{
					L2PcInstance pl = L2World.getInstance().getPlayer(clan.getLeaderName());
					if (pl!=null)
						pl.sendMessage("В хранилище Клана возвращены Свидетельства Участия в Войне за Холл Клана Горячего Источника");
					clan.getWarehouse().addItem("revert", 8034, cl._decreeCnt, null, null);
				}
			}
		}
		for (int id : _playersOnArena)
		{
			L2PcInstance pl = L2World.getInstance().findPlayer(id);
			if (pl!=null)
				pl.teleToLocation(150717, -124818, -2355);
		}
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
					setRegistrationPeriod(true);
					if (_clanhall.getOwnerId()!=0)
					{
						clanPlayersInfo regPlayers = _clansInfo.get(_clanhall.getOwnerId());
						if (regPlayers == null)
						{
							regPlayers = new clanPlayersInfo();
							regPlayers._clanName=_clanhall.getOwnerClan().getName();
							regPlayers._decreeCnt=0;
							_clansInfo.put(_clanhall.getOwnerId(), regPlayers);
						}
					}
					anonce("Внимание !!! Начался период регистрации на осаду Холл Клана , Дворец Радужных Источников.");
					anonce("Внимание !!! Битва за Холл Клана , Дворец Радужных Источников начнется через час.");
					remaining=siegeTimeRemaining;
				}
			}
			if (siegeTimeRemaining <= 0)
			{
				setRegistrationPeriod(false);
				startSiege();
				cancel();
				return;
			}
			schedule(remaining);			
		}
	};
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
				endSiege(false);
				cancel();
				return;
			}
			schedule(timeRemaining);
		}
	};
	private final ExclusiveTask _firstStepSiegeTask = new ExclusiveTask() {
		@Override
		protected void onElapsed()
		{
			startFirstStep();
		}
	};
	private class clanPlayersInfo
	{
		public String _clanName;
		public int _decreeCnt;
		public int _arenaNumber;
	}	
}