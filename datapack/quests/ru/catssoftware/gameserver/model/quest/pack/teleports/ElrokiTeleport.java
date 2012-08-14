package ru.catssoftware.gameserver.model.quest.pack.teleports;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class ElrokiTeleport extends Quest
{
	private static String qn = "6111_ElrokiTeleporters";
	
	public ElrokiTeleport()
	{
		super(6111,qn,"Teleports");
		addStartNpc(32111);
		addStartNpc(32112);
		addTalkId(32111);
		addTalkId(32112);
	}
	
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (npcId == 32111)
		{
			if (player.isInCombat())
			{
				htmltext = "<html><body>Orahochin:<br>Вы пришли ко мне ведя за собой динозавров?<br1>";
				htmltext = htmltext + "И теперь хотите чтобы я Вас отправил в нашу обитель? Моя обязанность защитить наш новый дом. Я не собираюсь делать наш дом приютом для таких авантюристов!<br1>";
				htmltext = htmltext + "В моих правилах не пропускать тех кто находится в сражении. Возвращаяся как закончишь свой бой.</body></html>";
			}
			else
				player.teleToLocation(4990,-1879,-3178);
		}
		else if (npcId == 32112)
			player.teleToLocation(7557,-5513,-3221);
		st.exitQuest(true);
		return htmltext;
	}
}