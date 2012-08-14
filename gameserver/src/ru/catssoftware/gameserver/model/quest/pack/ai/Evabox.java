package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.tools.random.Rnd;

public class Evabox extends Quest
{
	private int BOX = 32342;
	private int KISS_OF_EVA[] = {1073,3143,3252};
	private int REWARDS[] = {9692,9693};
	
	public Evabox()
	{
		super(-1,"evabox","ai");
		addKillId(BOX);
	}
	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		boolean found = false;
		for (L2Effect effect : player.getAllEffects())
			for (int kof : KISS_OF_EVA)
				if (effect.getSkill().getId()==kof)
					found = true;
		if (found)
		{
			int dropid = Rnd.get(REWARDS.length);
			dropItem(npc,REWARDS[dropid],1);
		}
		return null;
	}
	private void dropItem(L2NpcInstance npc,int itemId,int count)
	{
		L2ItemInstance ditem = ItemTable.getInstance().createItem("quest", itemId, count, null);
		ditem.dropMe(npc, npc.getX(), npc.getY(), npc.getZ()); 
	}
}
