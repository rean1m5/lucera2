package ru.catssoftware.gameserver.model.quest.pack.teleports;

import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class ToiVortexExit extends Quest
{
	private static String qn = "2400_toivortex_exit";
	
	private int NPC = 29055;
	
	public ToiVortexExit()
	{
		super(2400,qn,"Teleports");
		addStartNpc(NPC);
		addTalkId(NPC);
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		int chance = Rnd.get(3);
		int x = 0;
		int y = 0;
		int z = 0;
		if (chance == 0)
		{
			x= x + 108784 + Rnd.get(100);
			y= y + 16000 + Rnd.get(100);
			z= z - 4928;
		}
		else if (chance == 1)
		{
			x= x + 113824 + Rnd.get(100);
			y= x + 10448 + Rnd.get(100);
			z= z - 5164;
		}
		else
		{
			x= x + 115488 + Rnd.get(100);
			y= y + 22096 + Rnd.get(100);
			z= z - 5168;
		}
		player.teleToLocation(x, y, z);
		st.exitQuest(true);
		return "";
	}
}