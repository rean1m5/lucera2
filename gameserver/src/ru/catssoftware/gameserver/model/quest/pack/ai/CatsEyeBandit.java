package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;

public class CatsEyeBandit extends Quest
{
	private int CatsEyeBandit = 27038;
	private boolean FirstAttacked = false;

	public CatsEyeBandit()
	{
		super(-1, "cats_eye_bandit", "ai");
		addKillId(CatsEyeBandit);
		addAttackId(CatsEyeBandit);
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		int objId = npc.getObjectId();
		if (FirstAttacked)
		{
			if (Rnd.get(100) < 40)
				return super.onAttack(npc, attacker, damage, isPet);
			npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Вы смешны, серьезно думаете что сможете меня споймать?"));
		}
		else
			FirstAttacked = true;

		return super.onAttack(npc, attacker, damage, isPet);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == CatsEyeBandit)
		{
			int objId = npc.getObjectId();
			if (Rnd.get(100)<80)
				npc.broadcastPacket(new NpcSay(objId, 0, npc.getNpcId(), "Ох, это печально, я ничего не смог сделать...."));
			FirstAttacked = false;
		}
		else if (FirstAttacked)
		{
			addSpawn(npcId, npc);
		}
		return super.onKill(npc, killer, isPet);
	}
}