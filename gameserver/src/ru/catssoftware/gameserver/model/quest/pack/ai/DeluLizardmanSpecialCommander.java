package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class DeluLizardmanSpecialCommander extends Quest
{
	private int DeluLizardmanSpecialCommander = 21107;
	private boolean FirstAttacked = false;
	
	public DeluLizardmanSpecialCommander()
	{
		super(-1, "delu_lizardman_special_commander", "ai");
		addKillId(DeluLizardmanSpecialCommander);
		addAttackId(DeluLizardmanSpecialCommander);		
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
        int objId = npc.getObjectId();
        if (FirstAttacked)
        {
           if (Rnd.get(100)<40)
        	   return null;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Come on, I'll take you on!"));
        }
        else
        {
           FirstAttacked = true;
           npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "How dare you interrupt a sacred duel! You must be taught a lesson!"));
        }
        return null; 
	}
	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
        int npcId = npc.getNpcId();
        if (npcId == DeluLizardmanSpecialCommander)
            FirstAttacked = false;
        else if (FirstAttacked)
            addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
        return null;
	}
}