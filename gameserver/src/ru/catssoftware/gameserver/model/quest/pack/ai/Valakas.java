package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.instancemanager.grandbosses.ValakasManager;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;

public class Valakas extends Quest
{
	public static String QUEST = "valakas"; 
	public Valakas()
	{
		super(-1,QUEST,"ai");
		addStartNpc(31540);
		addStartNpc(31385);
		addTalkId(31540);
		addTalkId(31385);
		addKillId(29028);
	}
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState("valakas");
	    if (st==null)
	    	return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	    int npcId = npc.getNpcId();
	    if (npcId == 31385)    // Heart of Volcano
	    {
	      if (st.getInt("ok")!=0)
	      {
	        if (ValakasManager.getInstance().isEnableEnterToLair())
	        {
	          ValakasManager.getInstance().setValakasSpawnTask();
	          player.teleToLocation(203940,-111840,66);
	          return null;
	        }
	        else
	        {
	          st.exitQuest(true);
	          return "<html><body>Heart of Volcano:<br>Valakas is already awake!<br>You may not enter the Lair of Valakas.</body></html>";
	        }
	      }
	      else
	      {
	        st.exitQuest(true);
	        return "<html><body>Heart of Volcano:<br>Conditions are not right to enter to Lair of Valakas.</body></html>";
	      }
	    }
	    else if (npcId == 31540)    // Klein
	    {
	      if (ValakasManager.getInstance().isEnableEnterToLair())
	      {
	        if (st.getQuestItemsCount(7267) > 0)    // Check Floating Stone
	        {
	          st.takeItems(7267,1);
	          player.teleToLocation(183831,-115457,-3296);
	          st.set("ok","1");
	        }
	        else
	        {
	          st.exitQuest(true);
	          return "<html><body>Klein:<br>You do not have the Floating Stone. Go get one and then come back to me.</body></html>";
	        }
	      }
	      else
	      {
	        st.exitQuest(true);
	        return "<html><body>Klein:<br>Valakas is already awake!<br>You may not enter the Lair of Valakas.</body></html>";
	      }
	    }
	    return null;		
	}
	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{

		for(L2PcInstance pc : ValakasManager.getInstance().getPlayersInside()) {
	    	   QuestState pst = pc.getQuestState("valakas");
	           if (pst!=null) 
	               if (pst.getQuestItemsCount(8567) < 1) {
	                   pst.giveItems(8567,1);
	                   pst.exitQuest(true);
	               }
	    }
	    ValakasManager.getInstance().setCubeSpawn();
	    return null;
	}	
}
