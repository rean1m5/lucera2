package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.tools.random.Rnd;

public class FairyTrees extends L2AttackableAIScript
{
	private static final int[] mobs = { 27185, 27186, 27187, 27188 };

	public FairyTrees()
	{
		super(-1, "fairy_trees", "ai");
		this.registerMobs(mobs);
		super.addSpawnId(27189);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (contains(mobs, npcId))
		{
			for (int i = 0; i < 20; i++)
			{
				L2Attackable newNpc = (L2Attackable) addSpawn(27189, npc.getX(), npc.getY(), npc.getZ(), 0, false, 30000);
				L2Character originalKiller = isPet ? killer.getPet() : killer;
				newNpc.setRunning();
				newNpc.addDamageHate(originalKiller, 0, 999);
				newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalKiller);
				if (Rnd.get(1, 2) == 1)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(4243, 1);
					if (skill != null && originalKiller != null)
						skill.getEffects(newNpc, originalKiller);
				}
			}
		}
		return super.onKill(npc, killer, isPet);
	}
}