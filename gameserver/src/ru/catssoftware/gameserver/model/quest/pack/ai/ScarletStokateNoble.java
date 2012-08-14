package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.tools.random.Rnd;

public class ScarletStokateNoble extends Quest
{
	private int ScarletStokateNoble = 21378;
	private int ScarletStokateNobleB = 21652;
	
	public ScarletStokateNoble()
	{
		super(-1, "scarlet_stokate_noble", "ai");
		addKillId(ScarletStokateNoble);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
        int npcId = npc.getNpcId();
        if (npcId == ScarletStokateNoble && Rnd.get(100) < 20)
		{
			for(int i=0;i<1+Rnd.get(5);i++)
			{
				L2Attackable mob = (L2Attackable) addSpawn(ScarletStokateNobleB, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
				mob.addDamageHate(isPet?killer.getPet():killer, 0, 100);
			}
		}
        return null;
	}
}