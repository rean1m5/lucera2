package ru.catssoftware.gameserver.model.quest.pack.teleports;

import javolution.util.FastList;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class ToiVortexBlue extends Quest
{
	private static String qn = "1102_toivortex_blue";
	
	private int BLUE_DIMENSION_STONE = 4402;
	
	private int[] VORTEXS = {30952, 30954};
	private FastList<Integer> VORTEXS_ID = new FastList<Integer>();
	
	public ToiVortexBlue()
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
			if (st.getQuestItemsCount(BLUE_DIMENSION_STONE) >= 1)
			{
				st.takeItems(BLUE_DIMENSION_STONE,1);
				player.teleToLocation(114097,19935,935);
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