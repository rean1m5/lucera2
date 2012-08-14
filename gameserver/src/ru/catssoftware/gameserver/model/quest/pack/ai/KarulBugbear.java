package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class KarulBugbear extends Quest
{
	private int KarulBugbear = 20600;
	private boolean FirstAttacked = false;
	
	public KarulBugbear()
	{
		super(-1, "karul_bugbear", "ai");
		addKillId(KarulBugbear);
		addAttackId(KarulBugbear);
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		int objId = npc.getObjectId();
		if (FirstAttacked)
		{
			if (Rnd.get(100)<15)
				return null;
			npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Вы так смешны, я Вас убью!"));
		}
		else
		{
			FirstAttacked = true;
			npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(),"Зачем Вы вернулись?!"));
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == KarulBugbear)
			FirstAttacked = false;
		else if (FirstAttacked)
			addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
		return super.onKill(npc, killer, isPet);
	}
}