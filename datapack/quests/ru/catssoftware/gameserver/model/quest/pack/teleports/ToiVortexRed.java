package ru.catssoftware.gameserver.model.quest.pack.teleports;

import javolution.util.FastList;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class ToiVortexRed extends Quest
{
	private static String qn = "1102_toivortex_red";
	
	private int RED_DIMENSION_STONE = 4403;
	private int[] VORTEXS = {30952, 30953};
	private FastList<Integer> VORTEXS_ID = new FastList<Integer>();
	
	public ToiVortexRed()
	{
		super(1102,qn,"Teleports");
		for (int id:VORTEXS)
		{
			addStartNpc(id);
			addTalkId(id);
			VORTEXS_ID.add(id);
		}
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext="";
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (VORTEXS_ID.contains(npcId))
		{
			if (st.getQuestItemsCount(RED_DIMENSION_STONE) >= 1)
			{
				st.takeItems(RED_DIMENSION_STONE,1);
				player.teleToLocation(118558,16659,5987);
				st.exitQuest(true);
			}
			else
			{
				st.exitQuest(true);
				htmltext = "<html><head><body>Пространственный Вихрь:<br>У вас нет Камня Иных Миров, необходимого для телепортации.</body></html>";
			}
		}
		return htmltext;
	}
}