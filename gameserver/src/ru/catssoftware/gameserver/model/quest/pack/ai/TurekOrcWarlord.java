package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class TurekOrcWarlord extends Quest
{
	private int TurekOrcWarlord = 20495;
	private boolean FirstAttacked = false;
	
	public TurekOrcWarlord()
	{
		super(-1, "turek_orc_warlord", "ai");
		addKillId(TurekOrcWarlord);
		addAttackId(TurekOrcWarlord);		
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
        int objId = npc.getObjectId();
        if (FirstAttacked)
        {
           if (Rnd.get(100)<40)
        	   return null;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "You wont take me down easily."));
        }
        else
        {
           FirstAttacked = true;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "The battle has just begun!"));           
        }
        return null; 
	}
	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
        int npcId = npc.getNpcId();
        if (npcId == TurekOrcWarlord)
            FirstAttacked = false;
        else if (FirstAttacked)
            addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
        return null;
	}
}