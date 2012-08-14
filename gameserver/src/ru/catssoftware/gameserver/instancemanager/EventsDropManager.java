package ru.catssoftware.gameserver.instancemanager;

import java.util.HashMap;
import java.util.Map;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

import javolution.util.FastList;


/*
*
* Author: MHard - L2CatsSoftware DevTeam
*
*/
public class EventsDropManager
{
	private static EventsDropManager		_instance;
	private boolean							_haveActiveEvent=true;
	private Map<Integer,rewardRule>			_rewardRules =new HashMap<Integer, rewardRule>();
	
	public static enum ruleType 
	{
		ALL_NPC,
		BY_NPCID,
		BY_ZONE
	}
	
	public static final EventsDropManager getInstance()
	{
		if (_instance == null)
			_instance = new EventsDropManager();
		return _instance;
	}

	public boolean haveActiveEvent()
	{
		return _haveActiveEvent;
	}

	public void addRule(String event,ruleType type,int itemId[],int itemCnt[],int itemChance[])
	{
		addRule(event,type,itemId,itemCnt,itemChance,true);
	}	
	
	public void addRule(String event,ruleType type,int itemId[],int itemCnt[],int itemChance[], boolean lvlControl)
	{
		rewardRule rule=new rewardRule();
		rule._eventName=event;
		rule._ruleType=type;
		rule._rewardCnt=itemId.length;
		rule._levDifferenceControl = lvlControl;
		for(int x=1;x<=itemId.length;x++)
			rule._itemId.add(itemId[x-1]);
		for(int x=1;x<=itemCnt.length;x++)
			rule._itemCnt.add(itemCnt[x-1]);			
		for(int x=1;x<=itemChance.length;x++)
			rule._itemChance.add(itemChance[x-1]);			
		_rewardRules.put(_rewardRules.size()+1, rule);
		_haveActiveEvent=true;
	}
	public void addRule(String event,ruleType type,int npcId[],int itemId[],int itemCnt[],int itemChance[])
	{
		addRule(event,type,npcId,itemId,itemCnt,itemChance,true);
	}
	
	public void addRule(String event,ruleType type,int npcId[],int itemId[],int itemCnt[],int itemChance[], boolean lvlControl)
	{
		rewardRule rule=new rewardRule();
		rule._eventName=event;
		rule._ruleType=type;
		rule._rewardCnt=itemId.length;
		rule._levDifferenceControl = lvlControl;		
		for(int x=0;x<npcId.length;x++)
			rule._mobId.add(npcId[x]);
		for(int x=0;x<itemId.length;x++)
			rule._itemId.add(itemId[x]);
		for(int x=0;x<itemCnt.length;x++)
			rule._itemCnt.add(itemCnt[x]);			
		for(int x=0;x<itemChance.length;x++)
			rule._itemChance.add(itemChance[x]);			
		_rewardRules.put(_rewardRules.size()+1, rule);
		_haveActiveEvent=true;
	}

	public void addRule(String event,ruleType type,L2Zone zone,int itemId[],int itemCnt[],int itemChance[])
	{
		addRule(event,type,zone,itemId,itemCnt,itemChance,true);
	}
	
	public void addRule(String event,ruleType type,L2Zone zone,int itemId[],int itemCnt[],int itemChance[], boolean lvlControl)
	{
		rewardRule rule=new rewardRule();
		rule._eventName=event;
		rule._ruleType=type;
		rule._zone=zone;
		rule._rewardCnt=itemId.length;
		rule._levDifferenceControl = lvlControl;		
		for(int x=0;x<itemId.length;x++)
			rule._itemId.add(itemId[x]);
		for(int x=0;x<itemCnt.length;x++)
			rule._itemCnt.add(itemCnt[x]);			
		for(int x=0;x<itemChance.length;x++)
			rule._itemChance.add(itemChance[x]);			
		_rewardRules.put(_rewardRules.size()+1, rule);
		_haveActiveEvent=true;
	}
	public void removeEventRules(String event)
	{
		for(rewardRule tmp: _rewardRules.values())
			if (tmp._eventName==event)
				_rewardRules.remove(tmp);
		if (_rewardRules.size()==0)
			_haveActiveEvent=true;
	}
	public int[] calculateRewardItem(L2NpcTemplate npcTemplate,L2Character lastAttacker)
	{
		int res[]={0,0};
		int lvlDif=lastAttacker.getLevel()-npcTemplate.getLevel();
		FastList<rewards> _rewards = new FastList<rewards>();
		if (_rewardRules.size()>0)
			for(rewardRule tmp: _rewardRules.values())
			{
				if ( tmp._levDifferenceControl && (lvlDif>7 || lvlDif<-7))
					continue;
				if (tmp._ruleType==ruleType.ALL_NPC)
				{
					int cnt=0;
					while (cnt<=tmp._rewardCnt-1)
					{
						try {
							if(tmp._itemChance.get(cnt)>=Rnd.get(0,1000))
								_rewards.add(new rewards(tmp._itemId.get(cnt),tmp._itemCnt.get(cnt)));
						} catch(Exception e) {
							return new int [] {0,0};
						}
						cnt++;
					}
				}
				if (tmp._ruleType==ruleType.BY_NPCID)
				{
					if (tmp._mobId.contains(npcTemplate.getIdTemplate()))
					{
						int cnt=0;
						while (cnt<=tmp._rewardCnt-1)
						{
							if(tmp._itemChance.get(cnt)>=Rnd.get(0,1000))
								_rewards.add(new rewards(tmp._itemId.get(cnt),tmp._itemCnt.get(cnt)));
							cnt++;
						}
					}
				}
				if (tmp._ruleType==ruleType.BY_ZONE)
				{
					if (tmp._zone.isCharacterInZone(lastAttacker))
					{
						int cnt=0;
						while (cnt<=tmp._rewardCnt-1)
						{
							if(tmp._itemChance.get(cnt)>=Rnd.get(0,1000))
								_rewards.add(new rewards(tmp._itemId.get(cnt),tmp._itemCnt.get(cnt)));
							cnt++;
						}
					}
				}
			}
		if (_rewards.size()>0)
		{
			int rndRew=Rnd.get(_rewards.size());
			res[0]=_rewards.get(rndRew)._rewardId;
			res[1]=_rewards.get(rndRew)._rewardCnt;			
		}
		return res;
	}
	
	private class rewardRule
	{
		public int 					_rewardCnt=0;
		public String				_eventName;
		public ruleType 			_ruleType=null;
		public boolean				_levDifferenceControl;
		public FastList<Integer>	_mobId = new FastList<Integer>();
		public L2Zone 				_zone = null;
		public FastList<Integer>	_itemId	= new FastList<Integer>();
		public FastList<Integer>	_itemCnt = new FastList<Integer>();
		public FastList<Integer>	_itemChance = new FastList<Integer>();
	}
	private class rewards
	{
		public int		 			_rewardId;
		public int 					_rewardCnt;
		public rewards(int Id ,int Cnt)
		{
			_rewardId=Id;
			_rewardCnt=Cnt;
		}
	}
}