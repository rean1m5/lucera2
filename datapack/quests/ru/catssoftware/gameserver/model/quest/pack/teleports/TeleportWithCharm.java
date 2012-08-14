package ru.catssoftware.gameserver.model.quest.pack.teleports;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class TeleportWithCharm extends Quest
{
	private static String qn = "1100_teleport_with_charm";
	
	private int ORC_GATEKEEPER_CHARM = 1658;
	private int DWARF_GATEKEEPER_TOKEN = 1659;
	private int WHIRPY = 30540;
	private int TAMIL = 30576;
	
	public TeleportWithCharm()
	{
		super(1100,qn,"Teleports");
		addStartNpc(WHIRPY);
		addStartNpc(TAMIL);
		addTalkId(WHIRPY);
		addTalkId(TAMIL);
	}
	
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = "";
		int npcId = npc.getNpcId();
		if (npcId == TAMIL)
		{
			if (st.getQuestItemsCount(ORC_GATEKEEPER_CHARM) >= 1)
			{
				st.takeItems(ORC_GATEKEEPER_CHARM,1);
				player.teleToLocation(-80826,149775,-3043);
				st.exitQuest(true);
			}
			else
			{
				st.exitQuest(true);
				htmltext = "<html><body>Хранитель Портала Тамил:<br>";
				htmltext = htmltext + "Вы не можете телепортироваться без Оберега Хранителя Портала. Я дам Вам его, если выполните мой квест.";
				htmltext = htmltext + "</body></html>";
			}
		}
		else if (npcId == WHIRPY)
		{
			if (st.getQuestItemsCount(DWARF_GATEKEEPER_TOKEN) >= 1)
			{
				st.takeItems(DWARF_GATEKEEPER_TOKEN,1);
				player.teleToLocation(-80826,149775,-3043);
				st.exitQuest(true);
			}
			else
			{
				st.exitQuest(true);
				htmltext = "<html><body>Хранитель Портала Вирфи:<br>";
				htmltext = htmltext + "Мои сенсоры сообщают мне что у вас нет Знака Хранителя Портала. Если Вы раздобудете для меня Звездных камней, Я дам Вам Знак Хранителя Портала.";
				htmltext = htmltext + "</body></html>";
			}
		}
		return htmltext;
	}
}