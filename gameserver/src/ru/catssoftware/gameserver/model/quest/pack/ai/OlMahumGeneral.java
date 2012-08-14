package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class OlMahumGeneral extends Quest
{
	private int OlMahumGeneral = 20438;
	private boolean FirstAttacked = false;
	
	public OlMahumGeneral()
	{
		super(-1, "ol_mahum_general", "ai");
		addKillId(OlMahumGeneral);
		addAttackId(OlMahumGeneral);		
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
        int objId = npc.getObjectId();
        if (FirstAttacked)
        {
           if (Rnd.get(100)<40)
        	   return null;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "We shall see about that!"));
        }
        else
        {
           FirstAttacked = true;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "I will definitely repay this humiliation!"));
        }
        return null; 
	}
	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
        int npcId = npc.getNpcId();
        if (npcId == OlMahumGeneral)
            FirstAttacked = false;
        else if (FirstAttacked)
            addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
        return null;
	}
}