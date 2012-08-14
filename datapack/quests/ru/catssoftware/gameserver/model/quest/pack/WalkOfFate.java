package ru.catssoftware.gameserver.model.quest.pack;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;

public class WalkOfFate extends Quest
{
	private static String qn="112_WalkOfFate";
	
	private int Livina = 30572;
	private int Karuda = 32017;
	private int EnchantD = 956;

	public WalkOfFate()
	{
		super(112, qn, "Путь судьбы");
		addStartNpc(Livina);
		addTalkId(Livina);
		addTalkId(Karuda);
	}

	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = "<html><body>Вы не взяли квест для этого NPC или просто не соответствуете его минимальным требованиям!</body></html>";
                QuestState st=player.getQuestState(qn);
		if (st==null)
			return htmltext;
		int id = st.getState();
		int npcId = npc.getNpcId();
		if (id == State.COMPLETED)
			return "<html><body>Данный квест уже выполнен.</body></html>";
		else if (id == State.CREATED)
		{
			if (npcId == Livina)
			{
				if (player.getLevel() >= 20)
					htmltext = "30572-01.htm";
				else
				{
					htmltext = "30572-00.htm";
					st.exitQuest(true);
				}
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == Livina)
				htmltext = "30572-03.htm";
			else if (npcId == Karuda)
				htmltext = "32017-01.htm";
		}
		return htmltext;
	}

	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st=player.getQuestState(qn);
		if (st==null)
			return "";
		String htmltext = event;
		int cond = st.getInt("cond");
		if (event.equalsIgnoreCase("32017-02.htm") && (cond == 1))
		{
			st.rewardItems(57,22308);
			st.giveItems(EnchantD,1);
			st.addExpAndSp(112876,5774);
			st.exitQuest(false);
			st.playSound("ItemSound.quest_finish");
		}
		else if (event.equalsIgnoreCase("30572-02.htm"))
		{
			st.playSound("ItemSound.quest_accept");
			st.setState(State.STARTED);
			st.set("cond","1");
		}
		return htmltext;
	}
}