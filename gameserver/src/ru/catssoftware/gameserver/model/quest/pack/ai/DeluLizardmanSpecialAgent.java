package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class DeluLizardmanSpecialAgent extends Quest
{
	private int DeluLizardmanSpecialAgent = 21105;
	private boolean FirstAttacked = false;
	
	public DeluLizardmanSpecialAgent()
	{
		super(-1, "delu_lizardman_special_agent", "ai");
		addKillId(DeluLizardmanSpecialAgent);
		addAttackId(DeluLizardmanSpecialAgent);
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		int objId = npc.getObjectId();
		if (FirstAttacked)
		{
			if (Rnd.get(100) < 15)
				return super.onAttack(npc, attacker, damage, isPet);
			npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Эй, я вызываю тебя на дуэль прямо тут!"));
		}
		else
		{
			FirstAttacked = true;
			npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Как ты посмел напасть, ребята помогайте!"));
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == DeluLizardmanSpecialAgent)
			FirstAttacked = false;
		else if (FirstAttacked)
			addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
		return super.onKill(npc, killer, isPet);
	}
}