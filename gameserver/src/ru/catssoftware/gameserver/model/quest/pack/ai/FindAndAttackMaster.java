package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/** Данный скрипт описывает тех мобов, которые игнорируют атаки саммонов/петов, а атакуют только их хозяев */

public class FindAndAttackMaster extends L2AttackableAIScript
{
	public FindAndAttackMaster()
	{
		super(-1, "FindAndAttackMaster", "ai");
		int[] temp = {20965,20966,20967,20968,20969,20970,20971,20972,20973};
		//Chimera Piece, Changed Creation, Past Creature, Nonexistent Man, Giant's Shadow, Soldier of Ancient Times,
		//Warrior of Ancient Times, Shaman of Ancient Times, Forgotten Ancient People
		this.registerMobs(temp);
	}
	
	public String onAttack(L2NpcInstance npc, L2PcInstance player, int damage, boolean isPet)
	{
		if (player == null)
			return null;
		
		L2Character attacker = isPet ? player.getPet().getOwner() : player;
		npc.setIsRunning(true);
		((L2Attackable) npc).addDamageHate(attacker, 0, 999);
		npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		
		return super.onAttack(npc, player, damage, isPet);
	}
}