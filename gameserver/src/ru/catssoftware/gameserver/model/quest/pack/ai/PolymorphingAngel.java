package ru.catssoftware.gameserver.model.quest.pack.ai;

import java.util.Map;

import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import javolution.util.FastMap;

/**
 * Angel spawns...when one of the angels in the keys dies, the other angel will spawn.
 */
public class PolymorphingAngel extends L2AttackableAIScript
{
	private static final Map<Integer, Integer> ANGELSPAWNS = new FastMap<Integer, Integer>();

	static
	{
		ANGELSPAWNS.put(20830, 20859);
		ANGELSPAWNS.put(21067, 21068);
		ANGELSPAWNS.put(21062, 21063);
		ANGELSPAWNS.put(20831, 20860);
		ANGELSPAWNS.put(21070, 21071);
	}

	public PolymorphingAngel()
	{
		super(-1, "polymorphing_angel", "ai");
		int[] temp = {20830, 21067, 21062, 20831, 21070};
		this.registerMobs(temp);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (ANGELSPAWNS.containsKey(npcId))
		{
			L2Attackable newNpc = (L2Attackable) addSpawn(ANGELSPAWNS.get(npcId), npc);
			newNpc.setRunning();
		}
		return super.onKill(npc, killer, isPet);
	}
}