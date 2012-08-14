package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.tools.random.Rnd;
import javolution.util.FastList;
import javolution.util.FastMap;


public class Monastery extends L2AttackableAIScript
{
	public Monastery()
	{
		super(-1, "Monastery", "ai");
		int[] mobs = {22124, 22125, 22126, 22127, 22129};
		this.registerMobs(mobs);
	}

	private FastMap<Integer, FastList<L2Character>> _attackersList = new FastMap<Integer, FastList<L2Character>>();

	private static boolean _isAttacked = false;

	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		int npcObjId = npc.getObjectId();

		L2Character target = isPet ? attacker.getPet() : attacker;

		if (npc.getNpcId() == 22129 && !isPet && !_isAttacked && Rnd.get(100) < 50 && attacker.getActiveWeaponItem() != null)
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), attacker.getName() + ", спрячь свое оружие!!"));

		if (_attackersList.get(npcObjId) == null)
		{
			FastList<L2Character> player = new FastList<L2Character>();
			player.add(target);
			_attackersList.put(npcObjId, player);
		}
		else if (!_attackersList.get(npcObjId).contains(target))
			_attackersList.get(npcObjId).add(target);

		_isAttacked = true;

		return super.onAttack(npc, attacker, damage, isPet);
	}

	public String onAggroRangeEnter(L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		int npcObjId = npc.getObjectId();

		L2Character target = isPet ? player.getPet() : player;

		if(!player.isVisible())
			return null;
		if (player.getActiveWeaponItem() != null && player.getActiveWeaponItem().getItemId()>252)
		{
			if (npc.getNpcId() == 22129)
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), target.getName() + ", спрячь свое оружие!!"));
			else
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), "Вы не можете войти сюда с оружием!"));
			((L2Attackable) npc).addDamageHate(target, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		}
		else
		{
			if (_attackersList.get(npcObjId) == null || !_attackersList.get(npcObjId).contains(target))
				((L2Attackable) npc).getAggroListRP().remove(target);
			else
			{
				((L2Attackable) npc).addDamageHate(target, 0, 999);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}

		return super.onAggroRangeEnter(npc, player, isPet);
	}

	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcObjId = npc.getObjectId();
		_isAttacked = false;
		if (_attackersList.get(npcObjId) != null)
			_attackersList.get(npcObjId).clear();

		return super.onKill(npc, killer, isPet);
	}
}