package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class TurekOrcFootman extends Quest
{
	private int TurekOrcFootman = 20499;
	private boolean FirstAttacked = false;
	
	public TurekOrcFootman()
	{
		super(-1, "turek_orc_footman", "ai");
		addKillId(TurekOrcFootman);
		addAttackId(TurekOrcFootman);		
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
        int objId = npc.getObjectId();
        if (FirstAttacked)
        {
           if (Rnd.get(100)<40)
        	   return null;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "There is no reason for you to kill me! I have nothing you need!"));
        }
        else
        {
           FirstAttacked = true;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "We shall see about that!"));           
        }
        return null; 
	}
	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
        int npcId = npc.getNpcId();
        if (npcId == TurekOrcFootman)
            FirstAttacked = false;
        else if (FirstAttacked)
            addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
        return null;
	}
}