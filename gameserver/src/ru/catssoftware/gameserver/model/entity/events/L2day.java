package ru.catssoftware.gameserver.model.entity.events;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.instancemanager.EventsDropManager;
import ru.catssoftware.gameserver.instancemanager.EventsDropManager.ruleType;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.ArrayUtils;


public class L2day 
{
	private static Logger _log = Logger.getLogger("Event");
	private static int ScrollChance = Config.L2DAY_SCROLLCHANCE; // Шанс выпадения скрола
	private static int EnchScrollChance = Config.L2DAY_ENCHSCROLLCHANCE; // Шанс выпадения точки
	private static int AccessoryChance = Config.L2DAY_ACCCHANCE; // Шанс выпадения Бижи
	private static int rewardL2Day[] = {};// ={3931,3927,3928,3929,3926,3930,3933,3932,3935,3934};
	private static int rewardAcc[] = {};//={6662,6660};
	private static int rewardScroll[] = {};//={3958,3959};
	public static void addDrop()
	{
		int item[]=
		{
			3887,3880,3885,3884,3883,3882,3881,3877,3876,3875,3879,3888
		};
		int cnt[]=
		{
			1,1,1,1,1,1,1,1,1,1,1,1
		};
		int chance[]=
		{
			Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,
			Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,Config.L2DAY_CHANCE,
			Config.L2DAY_CHANCE,Config.L2DAY_CHANCE
		};
		EventsDropManager.getInstance().addRule("L2Day", ruleType.ALL_NPC,item,cnt,chance);
	}
	public static void spawnEventManager()
	{
	}
	public static void startEvent()
	{
		boolean started = false;

		if (Config.L2DAY_DROP)
		{
			for(String s : Config.L2DAY_REWARD.split(",")) 
				rewardL2Day = ArrayUtils.add(rewardL2Day, Integer.parseInt(s.trim()));
			for(String s : Config.L2DAY_ACCESSORIE.split(",")) 
				rewardAcc = ArrayUtils.add(rewardAcc, Integer.parseInt(s.trim()));
			for(String s : Config.L2DAY_SCROLL.split(",")) 
				rewardScroll= ArrayUtils.add(rewardScroll, Integer.parseInt(s.trim()));
			
			addDrop();
			started = true;
		}
		if (Config.L2DAY_SPAWN)
		{
			spawnEventManager();
			started = true;
		}
		// log info
		if (started)
			_log.info("L2Day event status: On.");
		else
			_log.info("L2Day event status: Off.");
	}
	public static void exchangeItem(L2PcInstance player,int val)
	{
		if (val==1) // Lineage II
		{
			L2ItemInstance item1 = player.getInventory().getItemByItemId(3882);//L
			L2ItemInstance item2 = player.getInventory().getItemByItemId(3881);//i
			L2ItemInstance item3 = player.getInventory().getItemByItemId(3883);//n
			L2ItemInstance item4 = player.getInventory().getItemByItemId(3877);//e
			L2ItemInstance item5 = player.getInventory().getItemByItemId(3875);//a
			L2ItemInstance item6 = player.getInventory().getItemByItemId(3879);//g
			L2ItemInstance item7 = player.getInventory().getItemByItemId(3888);//II
			if (item1==null || item2==null || item3==null || item4==null || item5==null || item6==null || item7==null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			if (item1.getCount()>=1 && item2.getCount()>=1 && item3.getCount()>=1 && item4.getCount()>=2 && item5.getCount()>=1 && item6.getCount()>=1 && item7.getCount()>=1)
			{
				player.destroyItemByItemId("Quest", 3882, 1, player, true);
				player.destroyItemByItemId("Quest", 3881, 1, player, true);
				player.destroyItemByItemId("Quest", 3883, 1, player, true);
				player.destroyItemByItemId("Quest", 3877, 2, player, true);
				player.destroyItemByItemId("Quest", 3875, 1, player, true);
				player.destroyItemByItemId("Quest", 3879, 1, player, true);
				player.destroyItemByItemId("Quest", 3888, 1, player, true);

				int rand= Rnd.get(0, rewardL2Day.length-1);
				L2ItemInstance item = player.getInventory().addItem("Quest", rewardL2Day[rand], 3, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addNumber(3);
				player.sendPacket(smsg);
				if (ScrollChance>=Rnd.get(1,1000) && rewardScroll.length>0)
				{
					item = player.getInventory().addItem("Quest", rewardScroll[Rnd.get(0,rewardScroll.length-1)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				if (EnchScrollChance>=Rnd.get(1,1000))
				{
					int EnchScrollId=0;
					int EnchScrollCnt=0;
					if (player.getLevel()<53)
					{
						EnchScrollId=951;
						EnchScrollCnt=3;
					}
					else if (player.getLevel()<63)
					{
						EnchScrollId=947;
						EnchScrollCnt=2;
					}
					else
					{
						EnchScrollId=729;
						EnchScrollCnt=1;
					}
					item = player.getInventory().addItem("Quest",EnchScrollId , EnchScrollCnt, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					smsg.addItemName(item);
					smsg.addNumber(EnchScrollCnt);
					player.sendPacket(smsg);
				}
				if (AccessoryChance>=Rnd.get(1,1000) && rewardAcc.length>0)
				{
					item = player.getInventory().addItem("Quest", rewardAcc[Rnd.get(0,rewardAcc.length)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
		}
		if (val==2) // CHRONICLE
		{
			L2ItemInstance item1 = player.getInventory().getItemByItemId(3882);//L
			L2ItemInstance item2 = player.getInventory().getItemByItemId(3881);//i
			L2ItemInstance item3 = player.getInventory().getItemByItemId(3883);//n
			L2ItemInstance item4 = player.getInventory().getItemByItemId(3877);//e
			L2ItemInstance item5 = player.getInventory().getItemByItemId(3876);//c
			L2ItemInstance item6 = player.getInventory().getItemByItemId(3880);//h
			L2ItemInstance item7 = player.getInventory().getItemByItemId(3885);//r
			L2ItemInstance item8 = player.getInventory().getItemByItemId(3884);//o
			if (item1==null || item2==null || item3==null || item4==null || item5==null || item6==null || item7==null || item8==null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			if (item1.getCount()>=1 && item2.getCount()>=1 && item3.getCount()>=1 && item4.getCount()>=1 && item5.getCount()>=2 && item6.getCount()>=1 && item7.getCount()>=1)
			{
				player.destroyItemByItemId("Quest", 3882, 1, player, true);
				player.destroyItemByItemId("Quest", 3881, 1, player, true);
				player.destroyItemByItemId("Quest", 3883, 1, player, true);
				player.destroyItemByItemId("Quest", 3877, 1, player, true);
				player.destroyItemByItemId("Quest", 3876, 2, player, true);
				player.destroyItemByItemId("Quest", 3880, 1, player, true);
				player.destroyItemByItemId("Quest", 3885, 1, player, true);
				player.destroyItemByItemId("Quest", 3884, 1, player, true);
				int rand= Rnd.get(0, 9);
				L2ItemInstance item = player.getInventory().addItem("Quest", rewardL2Day[rand], 3, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addNumber(3);
				player.sendPacket(smsg);
				if (ScrollChance>=Rnd.get(1,1000))
				{
					item = player.getInventory().addItem("Quest", rewardScroll[Rnd.get(0,1)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				if (EnchScrollChance>=Rnd.get(1,1000))
				{
					int EnchScrollId=0;
					int EnchScrollCnt=0;
					if (player.getLevel()<53)
					{
						EnchScrollId=951;
						EnchScrollCnt=3;
					}
					else if (player.getLevel()<63)
					{
						EnchScrollId=947;
						EnchScrollCnt=2;
					}
					else
					{
						EnchScrollId=729;
						EnchScrollCnt=1;
					}
					item = player.getInventory().addItem("Quest",EnchScrollId , EnchScrollCnt, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					smsg.addItemName(item);
					smsg.addNumber(EnchScrollCnt);
					player.sendPacket(smsg);
				}
				if (AccessoryChance>=Rnd.get(1,1000))
				{
					item = player.getInventory().addItem("Quest", rewardAcc[Rnd.get(0,1)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
		}		
		if (val==3) // THRONE
		{
			L2ItemInstance item1 = player.getInventory().getItemByItemId(3883);//n
			L2ItemInstance item2 = player.getInventory().getItemByItemId(3877);//e
			L2ItemInstance item3 = player.getInventory().getItemByItemId(3885);//r
			L2ItemInstance item4 = player.getInventory().getItemByItemId(3884);//o
			L2ItemInstance item5 = player.getInventory().getItemByItemId(3880);//h
			L2ItemInstance item6 = player.getInventory().getItemByItemId(3887);//t
			if (item1==null || item2==null || item3==null || item4==null || item5==null || item6==null)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			if (item1.getCount()>=1 && item2.getCount()>=1 && item3.getCount()>=1 && item4.getCount()>=1 && item5.getCount()>=1 && item6.getCount()>=1)
			{
				player.destroyItemByItemId("Quest", 3883, 1, player, true);
				player.destroyItemByItemId("Quest", 3877, 1, player, true);
				player.destroyItemByItemId("Quest", 3885, 1, player, true);
				player.destroyItemByItemId("Quest", 3884, 1, player, true);
				player.destroyItemByItemId("Quest", 3880, 1, player, true);
				player.destroyItemByItemId("Quest", 3887, 1, player, true);
				int rand= Rnd.get(0, 9);
				L2ItemInstance item = player.getInventory().addItem("Quest", rewardL2Day[rand], 2, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addNumber(3);
				player.sendPacket(smsg);
				if (ScrollChance>=Rnd.get(1,1000))
				{
					item = player.getInventory().addItem("Quest", rewardScroll[Rnd.get(0,1)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				if (EnchScrollChance>=Rnd.get(1,1000))
				{
					int EnchScrollId=0;
					int EnchScrollCnt=0;
					if (player.getLevel()<41)
					{
						EnchScrollId=955;
						EnchScrollCnt=4;
					}
					else if (player.getLevel()<53)
					{
						EnchScrollId=951;
						EnchScrollCnt=3;
					}
					else
					{
						EnchScrollId=947;
						EnchScrollCnt=2;
					}
					item = player.getInventory().addItem("Quest",EnchScrollId , EnchScrollCnt, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					smsg.addItemName(item);
					smsg.addNumber(EnchScrollCnt);
					player.sendPacket(smsg);
				}
				if (AccessoryChance>=Rnd.get(1,1000))
				{
					item = player.getInventory().addItem("Quest", rewardAcc[Rnd.get(0,1)], 1, player, player.getTarget());
					smsg = new SystemMessage(SystemMessageId.EARNED_S1);
					smsg.addItemName(item);
					player.sendPacket(smsg);
				}
				player.sendPacket(new ItemList(player, false));
			}
			else
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
		}
	}
}