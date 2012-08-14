package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.instancemanager.grandbosses.AntharasManager;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;

public class Antharas extends Quest
{
	private int PORTAL_STONE		= 3865;
	private int HEART				= 13001;
	private int ANTHARAS_WEAK		= 29066;
	private int ANTHARAS_NORMAL		= 29067;
	private int ANTHARAS_STRONG		= 29068;
	private int BEHEMOTH = 29069;
	public static String QUEST = "antharas";
	public Antharas()
	{
		super(-1,QUEST , "ai");
		addStartNpc(HEART);
		addTalkId(HEART);
		addKillId(ANTHARAS_WEAK);
		addKillId(ANTHARAS_NORMAL);
		addKillId(ANTHARAS_STRONG);
		addKillId(BEHEMOTH);
		for(int i=29070;i<29077;i++)
			addKillId(i);
	}

	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(QUEST);
		String htmltext = "<html><body>Heart of Warding:<br>Your can't enter to lair!<br>You can't enter to Antharas lair.</body></html>"; 
		if (st==null)
			st = newQuestState(player);

		int npcId = npc.getNpcId();
		if (npcId == HEART)
		{
			if (player.isFlying())
				return "<html><body>Heart of Warding:<br>Нельзя войти в логово в полете.</body></html>";

			if (AntharasManager.getInstance().isEnableEnterToLair())
			{
				if (st.getQuestItemsCount(PORTAL_STONE) >= 1)
				{
				  st.takeItems(PORTAL_STONE,1);
				  AntharasManager.getInstance().setAntharasSpawnTask();
				  player.teleToLocation(173826,115333,-7708);
				  return null;
				}
				else
				{
				  st.exitQuest(true);
				  return "<html><body>Heart of Warding:<br>You do not have the proper stones needed for teleport.<br>It is for the teleport where does 1 stone to you need.</body></html>";
				}
			}
			else
			{
				st.exitQuest(true);
				return htmltext;
			}
		}
		return htmltext;
	}

	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		AntharasManager.getInstance().onKill(npc);
		if(npc.getNpcId()!=ANTHARAS_NORMAL && npc.getNpcId()!=ANTHARAS_STRONG && npc.getNpcId()!=ANTHARAS_WEAK)
			return null;
		for(L2PcInstance pc : AntharasManager.getInstance().getPlayersInside()) {
			QuestState st = pc.getQuestState(QUEST);
			if(st!=null)
				if (st.getQuestItemsCount(8568) < 1)
					st.giveItems(8568,1);
					st.exitQuest(true);
		}
		AntharasManager.getInstance().setCubeSpawn();
		return super.onKill(npc, player, isPet);
	}
}

