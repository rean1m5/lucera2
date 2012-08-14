package ru.catssoftware.gameserver.model.quest.pack;

import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.State;

public class TheNameOfEvil1 extends Quest
{
	private static String qn="125_TheNameOfEvil-1";
	
	private int MUSHIKA = 32114;
	private int KARAKAWEI = 32117;
	private int ULUKAIMU = 32119;
	private int BALUKAIMU = 32120;
	private int CHUTAKAIMU = 32121;
	private int[] NPCLIST = {32114,32117,32119,32120,32121};
	
	private int[] MOBS1 = {22200, 22201, 22202, 22219, 22224};
	private int[] MOBS2 = {22203, 22204, 22205, 22220, 22225};
	
	private int ORCLAW = 8779;
	private int DIENBONE = 8780;
	private int EPITAPH = 8781;
	private int GAZKHFRAG = 8782;
	
	private int[] questItems = {ORCLAW, DIENBONE};
	
	public TheNameOfEvil1()
	{
		super(125,qn,"Имя Зла - Часть 1");
		addStartNpc(MUSHIKA);
		for (int i : NPCLIST)
			addTalkId(i);
		for (int i : MOBS1)
			addKillId(i);
		for (int i : MOBS2)
			addKillId(i);
		questItemIds = questItems;
	}
	
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st=player.getQuestState(qn);
		int ulu = st.getInt("ulu");
		int balu = st.getInt("balu");
		int chu = st.getInt("chu");
		if (event.equalsIgnoreCase("32114_08.htm"))
		{
			if (player.getLevel() >= 76)
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "32114_08.htm";
			}
			else
				htmltext = "32114_02.htm";
		}
		else if (event.equalsIgnoreCase("32114_12.htm"))
		{
			st.set("cond", "2");
			st.giveItems(GAZKHFRAG, 1);
			st.playSound("ItemSound.quest_middle");
			htmltext = "32114_12.htm";
		}
		else if (event.equalsIgnoreCase("32117_08.htm"))
		{
			st.set("cond", "3");
			st.playSound("ItemSound.quest_middle");
			htmltext = "32117_08.htm";
		}
		else if (event.equalsIgnoreCase("32117_16.htm"))
		{
			st.set("cond", "5");
			st.playSound("ItemSound.quest_middle");
			htmltext = "32117_16.htm";
		}
		else if (event.equalsIgnoreCase("ulu_tok"))
		{
			st.set("ulu", Integer.toString(ulu + 1));
			htmltext = "32119_05.htm";
		}
		else if (event.equalsIgnoreCase("ulu_eok"))
		{
			st.set("ulu", Integer.toString(ulu + 1));
			htmltext = "32119_06.htm";
		}
		else if (event.equalsIgnoreCase("ulu_pok"))
		{
			st.set("ulu", Integer.toString(ulu + 1));
			htmltext = "32119_07.htm";
		}
		else if (event.equalsIgnoreCase("ulu_uok"))
		{
			st.set("ulu", Integer.toString(ulu + 1));
			if (st.getInt("ulu") == 4)
				htmltext = "32119_09.htm";
			else
			{
				st.set("ulu", "0");
				htmltext = "32119_08.htm";
			}
		}
		else if (event.equalsIgnoreCase("32119_20.htm"))
		{
			st.set("cond", "6");
			st.playSound("ItemSound.quest_middle");
			htmltext = "32119_20.htm";
		}
		else if (event.equalsIgnoreCase("balu_tok"))
		{
			st.set("balu", Integer.toString(balu + 1));
			htmltext = "32120_05.htm";
		}
		else if (event.equalsIgnoreCase("balu_ook"))
		{
			st.set("balu", Integer.toString(balu + 1));
			htmltext = "32120_06.htm";
		}
		else if (event.equalsIgnoreCase("balu_o1ok"))
		{
			st.set("balu", Integer.toString(balu + 1));
			htmltext = "32120_07.htm";
		}
		else if (event.equalsIgnoreCase("balu_nok"))
		{
			st.set("balu", Integer.toString(balu + 1));
			if (st.getInt("balu") == 4)
				htmltext = "32120_09.htm";
			else
			{
				st.set("balu", "0");
				htmltext = "32120_08.htm";
			}
		}
		else if (event.equalsIgnoreCase("32120_19.htm"))
		{
			st.set("cond", "7");
			st.playSound("ItemSound.quest_middle");
			htmltext = "32120_19.htm";
		}
		else if (event.equalsIgnoreCase("chu_wok"))
		{
			st.set("chu", Integer.toString(chu + 1));
			htmltext = "32121_05.htm";
		}
		else if (event.equalsIgnoreCase("chu_aok"))
		{
			st.set("chu", Integer.toString(chu + 1));
			htmltext = "32121_06.htm";
		}
		else if (event.equalsIgnoreCase("chu_gok"))
		{
			st.set("chu", Integer.toString(chu + 1));
			htmltext = "32121_07.htm";
		}
		else if (event.equalsIgnoreCase("chu_uok"))
		{
			st.set("chu", Integer.toString(chu + 1));
			if (st.getInt("chu") == 4)
				htmltext = "32121_09.htm";
			else
			{
				st.set("chu", "0");
				htmltext = "32121_08.htm";
			}
		}
		else if (event.equalsIgnoreCase("32121_23.htm"))
		{
			st.set("cond", "8");
			st.giveItems(EPITAPH,1);
			st.playSound("ItemSound.quest_middle");
			htmltext = "32121_23.htm";
		}
		else
			htmltext = event;
		return htmltext;
	}
	
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		String htmltext = "<html><body><br>Вы не взяли квест для этого NPC или просто не соответствуете его минимальным требованиям!</body></html>";
		int npcId = npc.getNpcId();
		QuestState st = player.getQuestState(qn);
		QuestState st1 = player.getQuestState("124_MeetingTheElroki");

		if (st1 != null)
		{
			int id1 = st1.getState();
			if (id1 != State.COMPLETED)
				return "32114_04.htm";
			else if (id1 == State.COMPLETED && player.getLevel() < 76)
				return "32114_02.htm";
		}
		else
			return "32114_04.htm";
		
		if (st != null)
		{
			int id = st.getState();
			if (id == State.COMPLETED)
				return "<html><body>Данный квест уже Вами пройден.</body></html>";
			if (id == State.CREATED)
				st.set("cond", "0");
			int cond = st.getInt("cond");
			if (npcId == MUSHIKA)
			{
				if (cond == 0)
					htmltext = "32114_01.htm";
				else if (cond == 1)
					htmltext = "32114_10.htm";
				else if (cond > 1 && cond < 8)
					htmltext = "32114_14.htm";
				else if (cond == 8)
				{
					st.unset("cond");
					st.unset("ulu");
					st.unset("balu");
					st.unset("chu");
					htmltext = "32114_15.htm";
					st.addExpAndSp(859195, 86603);
					st.exitQuest(false);
					st.playSound("ItemSound.quest_finish");
				}
			}
			else if (npcId == KARAKAWEI)
			{
				if (cond < 2)
					htmltext = "32117_02.htm";
				else if (cond == 2)
					htmltext = "32117_01.htm";
				else if (cond == 3)
					htmltext = "32117_12.htm";
				else if (cond == 4)
				{
					st.takeItems(ORCLAW, 2);
					st.takeItems(DIENBONE, 2);
					htmltext = "32117_11.htm";
				}
				else if (cond > 4 && cond < 8)
					htmltext = "32117_19.htm";
				else if (cond == 8)
					htmltext = "32117_20.htm";
			}
			else if (npcId == ULUKAIMU)
			{
				if (cond == 5)
				{
					st.set("ulu", "0");
					htmltext = "32119_01.htm";
				}
				else if (cond < 5)
					htmltext = "32119_02.htm";
				else if (cond > 5)
					htmltext = "32119_03.htm";
			}
			else if (npcId == BALUKAIMU)
			{
				if (cond == 6)
				{
					st.set("balu", "0");
					htmltext = "32120_01.htm";
				}
				else if (cond < 6)
					htmltext = "32120_02.htm";
				else if (cond > 6)
					htmltext = "32120_03.htm";
			}
			else if (npcId == CHUTAKAIMU)
			{
				if (cond == 7)
				{
					st.set("chu", "0");
					htmltext = "32121_01.htm";
				}
				else if (cond < 7)
					htmltext = "32121_02.htm";
				else if (cond > 7)
					htmltext = "32121_03.htm";
			}
		}
		return htmltext;
	}
	
	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		QuestState st = player.getQuestState(qn);
		if (st != null)
		{
			int cond = st.getInt("cond");
			if (cond == 3)
			{
				if (contains(MOBS1,npcId) && (st.getQuestItemsCount(ORCLAW) < 2) && (Rnd.get(100) < (10 * Config.RATE_DROP_QUEST)))
				{
					st.giveItems(ORCLAW, 1);
					st.playSound("ItemSound.quest_itemget");
				}
				else if (contains(MOBS2,npcId) && (st.getQuestItemsCount(DIENBONE) < 2) && (Rnd.get(100) < (10 * Config.RATE_DROP_QUEST)))
				{
					st.giveItems(DIENBONE, 1);
					st.playSound("ItemSound.quest_itemget");
				}
				
				if ((st.getQuestItemsCount(ORCLAW) == 2) && (st.getQuestItemsCount(DIENBONE) == 2))
				{
					st.set("cond", "4");
					st.playSound("ItemSound.quest_middle");
				}
			}
		}
		return null;
	}
}