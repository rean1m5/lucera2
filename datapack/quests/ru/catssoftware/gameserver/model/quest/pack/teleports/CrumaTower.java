package ru.catssoftware.gameserver.model.quest.pack.teleports;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.Quest;

public class CrumaTower extends Quest
{
	private static String qn = "1108_cruma_tower";
	private int MOZELLA = 30483;

	public CrumaTower()
	{
		super(1108,qn,"Teleports");
		addStartNpc(MOZELLA);
		addTalkId(MOZELLA);
	}

	
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(qn);
		if (player.getLevel() > 55)
		{
			htmltext = htmltext + "<html><body>Хранитель Портала Мозелла:<br>";
			htmltext = htmltext + "О ! Вы слишком сильны чтобы пройти в этот портал. На Вас распространяется ограничение магнитного щита башни.<br>";
			htmltext = htmltext + "(Персонажи чей уровень достиг 56 или более не могут войти в Башню Крумы.)</body></html>";
		}
		else
			player.teleToLocation(17724,114004,-11672);
		st.exitQuest(true);
		return htmltext;
	}
}