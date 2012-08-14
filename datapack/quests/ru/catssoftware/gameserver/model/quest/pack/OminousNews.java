package ru.catssoftware.gameserver.model.quest.pack;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;

public class OminousNews extends Quest
{
	private static String qn="122_OminousNews";
	
	private int MOIRA = 31979;
	private int KARUDA = 32017;
	private String defaulttxt="<html><body>Вы не взяли квест для этого NPC или просто не соответствуете его минимальным требованиям!</body></html>";
	
	public OminousNews()
	{
		super(122,qn,"Тревожные новости");
		addStartNpc(MOIRA);
		addTalkId(MOIRA);
		addTalkId(KARUDA);
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		int npcId = npc.getNpcId();
		String htmltext = defaulttxt;
		QuestState st = player.getQuestState(qn);
		if (st == null)	
			return htmltext;
		int id = st.getState();
		int cond = st.getInt("cond");
		if (id == State.COMPLETED)
			htmltext="<html><body>Данный квест уже выполнен.</body></html>";
		else if (npcId == MOIRA)
		{
			if (cond == 0)
			{
				if (player.getLevel()>=20)
					htmltext = "31979-02.htm";
				else
				{
					htmltext = "31979-01.htm";
					st.exitQuest(true);
				}
			}
			else
				htmltext = "31979-03.htm";
		}
		else if ((npcId == KARUDA) && (cond==1) && (id == State.STARTED))
		{
			htmltext = "32017-01.htm";
			st.set("ok","1");
		}
		return htmltext;
	}
	
	@Override
	public String onEvent(String event, QuestState st)
	{
		String htmltext = defaulttxt;
		int id = st.getState();
		int cond = st.getInt("cond");
		if (id != State.COMPLETED)
		{
			htmltext = event;
			if (event.equalsIgnoreCase("31979-03.htm") && (cond == 0))
			{
				st.set("cond","1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
			}
			else if (event.equalsIgnoreCase("32017-02.htm"))
			{
				if ((cond == 1) && (st.getInt("ok")==1))
				{
					st.rewardItems(57,8923);
					st.addExpAndSp(45151,2310);
					st.unset("cond");
					st.unset("ok");
					st.exitQuest(false);
					st.playSound("ItemSound.quest_finish");
				}
				else
					htmltext=defaulttxt;
			}
		}
		return htmltext;
	}
}