package ru.catssoftware.gameserver.model.quest.pack;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.State;

public class TheNameOfEvil2 extends Quest
{
	private static String qn="126_TheNameOfEvil-2";

	private final int MUSHIKA = 32114;
	private final int ASAMAH = 32115;
	private final int ULUKAIMU = 32119;
	private final int BALUKAIMU = 32120;
	private final int CHUTAKAIMU = 32121;
	private final int WARRIORGRAVE = 32122;
	private final int STATUE = 32109;
	private int[] NPCLIST = {MUSHIKA, ASAMAH, ULUKAIMU, BALUKAIMU, CHUTAKAIMU, WARRIORGRAVE, STATUE};
	
	private final int BONEPOWDER = 8783;
	private final int EPITAPH = 8781;
	private final int EWA = 729;
	private final int ADENA = 57;
	
	public TheNameOfEvil2()
	{
		super(126,qn,"Имя Зла - Часть 2");
		addStartNpc(ASAMAH);
		for (int i : NPCLIST)
			addTalkId(i);
	}
	
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st=player.getQuestState(qn);
		if (event.equalsIgnoreCase("32115-05.htm"))
		{
			if (player.getLevel() >= 77)
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "32115-05.htm";
			}
			else
				htmltext = "32115-02.htm";
		}
		else if (event.equalsIgnoreCase("32115-10.htm"))
		{
			st.set("cond", "2");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32119-02.htm"))
		{
			st.set("cond", "3");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32119-09.htm"))
		{
			st.set("cond", "4");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32119-11.htm"))
		{
			st.set("cond", "5");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32120-07.htm"))
		{
			st.set("cond", "6");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32120-09.htm"))
		{
			st.set("cond", "7");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32120-11.htm"))
		{
			st.set("cond", "8");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32121-07.htm"))
		{
			st.set("cond", "9");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32121-10.htm"))
		{
			st.set("cond", "10");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32121-15.htm"))
		{
			st.set("cond", "11");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32122-03.htm"))
		{
			st.set("cond", "12");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32122-15.htm"))
		{
			st.set("cond", "13");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32122-18.htm"))
		{
			st.set("cond", "14");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32122-87.htm"))
		{
			htmltext = "32122-87.htm";
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32122-90.htm"))
		{
			st.set("cond", "18");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32109-02.htm"))
		{
			st.set("cond", "19");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32109-19.htm"))
		{
			st.set("cond", "20");
			st.takeItems(BONEPOWDER, 1);
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32115-21.htm"))
		{
			st.set("cond", "21");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32115-28.htm"))
		{
			st.set("cond", "22");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32114-08.htm"))
		{
			st.set("cond", "23");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("32114-09.htm"))
		{
			st.playSound("ItemSound.quest_finish");
			st.giveItems(EWA, 1);
			st.giveItems(ADENA, 460683);
			st.addExpAndSp(1015973, 102802);
			st.unset("cond");
			st.exitQuest(false);
		}

		else if (event.equalsIgnoreCase("DOOne"))
		{
			htmltext = "32122-26.htm";
			if(st.getInt("DO") < 1)
				st.set("DO", "1");
		}
		else if (event.equalsIgnoreCase("MIOne"))
		{
			htmltext = "32122-30.htm";
			if (st.getInt("MI") < 1)
				st.set("MI", "1");
		}
		else if (event.equalsIgnoreCase("FAOne"))
		{
			htmltext = "32122-34.htm";
			if (st.getInt("FA") < 1)
				st.set("FA", "1");
		}
		else if (event.equalsIgnoreCase("SOLOne"))
		{
			htmltext = "32122-38.htm";
			if (st.getInt("SOL") < 1)
				st.set("SOL", "1");
		}
		else if (event.equalsIgnoreCase("FA_2One"))
		{
			if (st.getInt("FA_2") < 1)
				st.set("FA_2", "1");
			htmltext = getSongOne(st);
		}
		else if (event.equalsIgnoreCase("FATwo"))
		{
			htmltext = "32122-47.htm";
			if (st.getInt("FA") < 1)
				st.set("FA", "1");
		}
		else if (event.equalsIgnoreCase("SOLTwo"))
		{
			htmltext = "32122-51.htm";
			if (st.getInt("SOL") < 1)
				st.set("SOL", "1");
		}
		else if (event.equalsIgnoreCase("TITwo"))
		{
			htmltext = "32122-55.htm";
			if (st.getInt("TI") < 1)
				st.set("TI", "1");
		}
		else if (event.equalsIgnoreCase("SOL_2Two"))
		{
			htmltext = "32122-59.htm";
			if (st.getInt("SOL_2") < 1)
				st.set("SOL_2", "1");
		}
		else if (event.equalsIgnoreCase("FA_2Two"))
		{
			if (st.getInt("FA_2") < 1)
				st.set("FA_2", "1");
			htmltext = getSongTwo(st);
		}
		else if (event.equalsIgnoreCase("SOLTri"))
		{
			htmltext = "32122-68.htm";
			if (st.getInt("SOL") < 1)
				st.set("SOL", "1");
		}
		else if (event.equalsIgnoreCase("FATri"))
		{
			htmltext = "32122-72.htm";
			if (st.getInt("FA") < 1)
				st.set("FA", "1");
		}
		else if (event.equalsIgnoreCase("MITri"))
		{
			htmltext = "32122-76.htm";
			if (st.getInt("MI") < 1)
				st.set("MI", "1");
		}
		else if (event.equalsIgnoreCase("FA_2Tri"))
		{
			htmltext = "32122-80.htm";
			if (st.getInt("FA_2") < 1)
				st.set("FA_2", "1");
		}
		else if (event.equalsIgnoreCase("MI_2Tri"))
		{
			if (st.getInt("MI_2") < 1)
				st.set("MI_2", "1");
			htmltext = getSongThree(st);
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
		QuestState st1 = player.getQuestState("125_TheNameOfEvil-1");
		if (st1 != null)
		{
			int id1 = st1.getState();
			if (id1 != State.COMPLETED)
				return "32115-04.htm";
			else if (id1 == State.COMPLETED && player.getLevel() < 77)
				return "32115-02.htm";
			else if (st.getQuestItemsCount(EPITAPH) == 0)
				return "32115-04.htm";
		}
		else
			return "32115-04.htm";
		
		if (st != null)
		{
			int id = st.getState();
			if (id == State.COMPLETED)
				return "<html><body>Данный квест уже Вами пройден.</body></html>";
			if (id == State.CREATED)
				st.set("cond", "0");
			int cond = st.getInt("cond");
			
			if (npcId == ASAMAH)
			{
				if (cond == 0)
					htmltext = "32115-01.htm";
				else if (cond == 1)
					htmltext = "32115-11.htm";
				else if (cond > 1 && cond < 20)
					htmltext = "32115-12.htm";
				else if (cond == 20)
					htmltext = "32115-13.htm";
				else if (cond == 22)
					htmltext = "32115-29.htm";
			}
			else if (npcId == ULUKAIMU)
			{
				if (cond == 1)
					htmltext = "32119-01a.htm";
				else if (cond == 2)
					htmltext = "32119-02.htm";
				else if (cond == 3)
					htmltext = "32119-08.htm";
				else if (cond == 4)
					htmltext = "32119-09.htm";
				else if (cond >= 5)
					htmltext = "32119-12.htm";
			}
			else if (npcId == BALUKAIMU)
			{
				if (cond < 5)
					htmltext = "32120-02.htm";
				else if (cond == 5)
					htmltext = "32120-01.htm";
				else if (cond == 6)
					htmltext = "32120-03.htm";
				else if (cond == 7)
					htmltext = "32120-08.htm";
				else if (cond >= 8)
					htmltext = "32120-12.htm";
			}
			else if (npcId == CHUTAKAIMU)
			{
				if (cond < 8)
					htmltext = "32121-02.htm";
				else if (cond == 8)
					htmltext = "32121-01.htm";
				else if (cond == 9)
					htmltext = "32121-03.htm";
				else if (cond == 10)
					htmltext = "32121-10.htm";
				else if (cond >= 11)
					htmltext = "32121-16.htm";
			}
			else if (npcId == WARRIORGRAVE)
			{
				if (cond < 11)
					htmltext = "32122-02.htm";
				else if (cond == 11)
					htmltext = "32122-01.htm";
				else if (cond == 12)
					htmltext = "32122-15.htm";
				else if (cond == 13)
					htmltext = "32122-18.htm";
				else if (cond == 14)
					htmltext = "32122-24.htm";
				else if (cond == 15)
					htmltext = "32122-45.htm";
				else if (cond == 16)
					htmltext = "32122-66.htm";
				else if (cond == 17)
					htmltext = "32122-84.htm";
				else if (cond == 18)
					htmltext = "32122-91.htm";
			}
			else if (npcId == STATUE)
			{
				if (cond < 18)
					htmltext = "32109-03.htm";
				else if (cond == 18)
					htmltext = "32109-02.htm";
				else if (cond == 19)
					htmltext = "32109-05.htm";
				else if (cond > 19)
					htmltext = "32109-04.htm";
			}
			else if (npcId == MUSHIKA)
			{
				if (cond < 22)
					htmltext = "32114-02.htm";
				else if (cond == 22)
					htmltext = "32114-01.htm";
				else if (cond == 23)
					htmltext = "32114-04.htm";
			}
		}
		return htmltext;
	}
	
	private String getSongOne(QuestState st)
	{
		String htmltext = "32122-24.htm";
		if (st.getInt("cond") == 14 && st.getInt("DO") > 0 && st.getInt("MI") > 0 && st.getInt("FA") > 0 && st.getInt("SOL") > 0 && st.getInt("FA_2") > 0)
		{
			htmltext = "32122-42.htm";
			st.set("cond", "15");
			st.unset("DO");
			st.unset("MI");
			st.unset("FA");
			st.unset("SOL");
			st.unset("FA_2");
			st.playSound("ItemSound.quest_middle");
		}
		return htmltext;
	}

	private String getSongTwo(QuestState st)
	{
		String htmltext = "32122-45.htm";
		if (st.getInt("cond") == 15 && st.getInt("FA") > 0 && st.getInt("SOL") > 0 && st.getInt("TI") > 0 && st.getInt("SOL_2") > 0 && st.getInt("FA_2") > 0)
		{
			htmltext = "32122-63.htm";
			st.set("cond", "16");
			st.unset("FA");
			st.unset("SOL");
			st.unset("TI");
			st.unset("SOL_2");
			st.unset("FA3_2");
			st.playSound("ItemSound.quest_middle");
		}
		return htmltext;
	}

	private String getSongThree(QuestState st)
	{
		String htmltext = "32122-66.htm";
		if (st.getInt("cond") == 16 && st.getInt("SOL") > 0 && st.getInt("FA") > 0 && st.getInt("MI") > 0 && st.getInt("FA_2") > 0 && st.getInt("MI_2") > 0)
		{
			htmltext = "32122-84.htm";
			st.set("cond", "17");
			st.unset("SOL");
			st.unset("FA");
			st.unset("MI");
			st.unset("FA_2");
			st.unset("MI_2");
			st.playSound("ItemSound.quest_middle");
		}
		return htmltext;
	}
}