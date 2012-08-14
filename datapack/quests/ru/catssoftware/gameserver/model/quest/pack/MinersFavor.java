package ru.catssoftware.gameserver.model.quest.pack;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;

public class MinersFavor extends Quest
{
	private static String qn="5_MinersFavor";
	
	private int BOLTER = 30554;
	private int SHARI  = 30517;
	private int GARITA = 30518;
	private int REED   = 30520;
	private int BRUNON = 30526;

	private int BOLTERS_LIST         = 1547;
	private int MINING_BOOTS         = 1548;
	private int MINERS_PICK          = 1549;
	private int BOOMBOOM_POWDER      = 1550;
	private int REDSTONE_BEER        = 1551;
	private int BOLTERS_SMELLY_SOCKS = 1552;
	
	private int NECKLACE = 906;
	
	private int[] questItems = {BOLTERS_LIST, MINING_BOOTS, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER, BOLTERS_SMELLY_SOCKS};  
	
	public MinersFavor()
	{
		super(5,qn,"Поручение шахтера");
		addStartNpc(BOLTER);
		addTalkId(BOLTER);
		addTalkId(SHARI);
		addTalkId(GARITA);
		addTalkId(REED);
		addTalkId(BRUNON);
		questItemIds = questItems;		
	}

	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = "<html><body>Вы не взяли квест для этого NPC или просто не соответствуете его минимальным требованиям!</body></html>";
		QuestState st=player.getQuestState(qn);
		if (st==null)
			return htmltext;
		int id = st.getState();
		if (id == State.COMPLETED)
			return "<html><body>Данный квест уже выполнен.</body></html>";		
		int npcId = npc.getNpcId();
		int cond = st.getInt("cond");
		if (id == State.CREATED && npcId == BOLTER)
		{
			if (player.getLevel() >= 2)
				htmltext = "bolter-1.htm";
			else
			{
				htmltext = "nocondition.htm";
				st.exitQuest(true);
			}
		}
		if (id == State.STARTED)
		{
			if (npcId == BOLTER)
			{
				if (cond==1)
					htmltext = "bolter-3.htm";
				else if (cond==2)
				{
					htmltext = "bolter-5.htm";
					st.rewardItems(57,2466);
					st.giveItems(NECKLACE,1);
					st.addExpAndSp(5672,446);
					st.unset("cond");
					st.exitQuest(false);
					st.playSound("ItemSound.quest_finish");
				}
			}
			else if (npcId == SHARI)
			{
				if (st.getQuestItemsCount(BOOMBOOM_POWDER)>0)
					htmltext = "shary-2.htm";
				else
				{
					htmltext = "shary-1.htm";
			        st.giveItems(BOOMBOOM_POWDER,1);
			        st.playSound("ItemSound.quest_itemget");
			        checkItems(st);
				}
			}
			else if (npcId == REED)
			{
				if (st.getQuestItemsCount(REDSTONE_BEER)>0)
					htmltext = "rid-2.htm";
				else
				{
					htmltext = "rid-1.htm";
			        st.giveItems(REDSTONE_BEER,1);
			        st.playSound("ItemSound.quest_itemget");
			        checkItems(st);
				}
			}
			else if (npcId == GARITA)
			{
				if (st.getQuestItemsCount(MINING_BOOTS)>0)
					htmltext = "garit-2.htm";
				else
				{
					htmltext = "garit-1.htm";
			        st.giveItems(MINING_BOOTS,1);
			        st.playSound("ItemSound.quest_itemget");
			        checkItems(st);
				}
			}
			else if (npcId == BRUNON)
			{
				if (st.getQuestItemsCount(MINERS_PICK)>0)
					htmltext = "brunon-4.htm";
				else
					htmltext = "brunon-1.htm";
			}			
		}
		return htmltext;
	}
	@Override
	public String onEvent(String event, QuestState st)
	{
		String htmltext = event;
		if (event.equalsIgnoreCase("bolter-2.htm"))
		{
		     st.giveItems(BOLTERS_LIST,1);
		     st.giveItems(BOLTERS_SMELLY_SOCKS,1);
		     st.set("cond","1");
		     st.setState(State.STARTED);
		     st.playSound("ItemSound.quest_accept");
		}
		else if (event.equalsIgnoreCase("BrunonEvent"))
		{
			if (st.getQuestItemsCount(BOLTERS_SMELLY_SOCKS)>0)
			{
			     st.takeItems(BOLTERS_SMELLY_SOCKS,-1);
			     st.giveItems(MINERS_PICK,1);
			     htmltext = "brunon-3.htm";
			     checkItems(st);
			}
			else
				htmltext = "brunon-2.htm";
		}
		return htmltext;		     
	}
	private void checkItems(QuestState st)
	{
		if ((st.getQuestItemsCount(MINERS_PICK)>0)&&(st.getQuestItemsCount(MINING_BOOTS)>0)&&(st.getQuestItemsCount(REDSTONE_BEER)>0)&&(st.getQuestItemsCount(BOOMBOOM_POWDER)>0))
		{
		       st.set("cond","2");
		       st.playSound("ItemSound.quest_middle");			
		}
	}
}