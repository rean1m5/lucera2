package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.instancemanager.grandbosses.SailrenManager;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.zone.L2BossZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;

public class Sailren extends Quest
{
	//NPC
	private int STATUE        = 32109;
	public static int VELOCIRAPTOR  = 22218;
	public static int PTEROSAUR     = 22199;
	public static int TYRANNOSAURUS = 22217;
	public static int SAILREN       = 29065;
	public static String QUEST = "sailren";  

	//ITEM
	private int GAZKH = 8784;
	
	public Sailren()
	{
		super(-1, QUEST, "ai");
		for (L2Spawn s : SpawnTable.getInstance().findAllNpc(TYRANNOSAURUS)) {
			SpawnTable.getInstance().deleteSpawn(s,true);
		}
		
		addStartNpc(STATUE);
		addTalkId(STATUE);
		addKillId(VELOCIRAPTOR);
		addKillId(PTEROSAUR);
		addKillId(TYRANNOSAURUS);
		addKillId(SAILREN);
		
	}
	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		L2BossZone zone = (L2BossZone)npc.getZone("Boss");
		if(zone==null || zone.getBoss()!=L2Zone.Boss.SAILREN) 
			return null;
	    int npcId = npc.getNpcId();
	    if (npcId == VELOCIRAPTOR)
	      SailrenManager.getInstance().setSailrenSpawnTask(PTEROSAUR);
	    else if (npcId == PTEROSAUR)
	      SailrenManager.getInstance().setSailrenSpawnTask(TYRANNOSAURUS);
	    else if (npcId == TYRANNOSAURUS)
	      SailrenManager.getInstance().setSailrenSpawnTask(SAILREN);
	    else if (npcId == SAILREN)
	      SailrenManager.getInstance().setCubeSpawn();
	    return null;
	}
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		QuestState st = player.getQuestState("sailren");
	    if (st==null)
	    	return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	    int npcId = npc.getNpcId();
	    if (npcId == STATUE)
	    {
	      if (st.getQuestItemsCount(GAZKH)!=0)
	      {
	        int ENTRY_SATAT = SailrenManager.getInstance().canIntoSailrenLair(player);
	        if (ENTRY_SATAT == 1 || ENTRY_SATAT == 2)
	        {
	          st.exitQuest(true);
	          return "<html><body>Shilen's Stone Statue:<br>Another adventurers have already fought against the sailren. Do not obstruct them.</body></html>";
	        }
	        else if (ENTRY_SATAT == 3)
	        {
	          st.exitQuest(true);
	          return "<html><body>Shilen's Stone Statue:<br>The sailren is very powerful now. It is not possible to enter the inside.</body></html>";
	        }
	        else if (ENTRY_SATAT == 4)
	        {
	          st.exitQuest(true);
	          return "<html><body>Shilen's Stone Statue:<br>You seal the sailren alone? You should not do so! Bring the companion.</body></html>";
	        }
	        else if (ENTRY_SATAT == 0)
	        {
	          st.takeItems(GAZKH,1);
	          SailrenManager.getInstance().entryToSailrenLair(player);
	          return "<html><body>Shilen's Stone Statue:<br>Please seal the sailren by your ability.</body></html>";
	        }
	      }
	      else
	      {
	        st.exitQuest(true);
	        return "<html><body>Shilen's Stone Statue:<br><font color=\"LEVEL\">Gazkh</font> is necessary for seal the sailren.</body></html>";
	      }
	    }
	    return null;
	}	
}
