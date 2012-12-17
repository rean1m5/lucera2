package ru.catssoftware.gameserver.model.quest.pack.ai;

import javolution.util.FastList;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.grandbosses.QueenAntManager;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.tools.random.Rnd;

import java.util.List;

public class QueenAnt extends L2AttackableAIScript
{
	public static final int QUEEN = 29001;
	public static final int LARVA = 29002;
	public static final int NURSE = 29003;
	public static final int GUARD = 29004;
	public static final int ROYAL = 29005;

	private static boolean _isAlive = false;
	private static L2NpcInstance larva;
	private static L2NpcInstance queen;
	private static List<L2Attackable> _Minions = new FastList<L2Attackable>();
	private List<L2Attackable> _Nurses = new FastList<L2Attackable>();

	public QueenAnt ()
	{
		super(-1, "queen_ant", "ai");
		int[] mobs = {QUEEN, LARVA, NURSE, GUARD, ROYAL};
		registerMobs(mobs);
	}

	public String onSpawn(L2NpcInstance npc)
	{
		if (npc.getNpcId() == QUEEN && !_isAlive)
		{
			queen = npc;
			_isAlive = true;
			startQuestTimer("action", 10000, npc, null, true);
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			//Spawn minions
			larva= addSpawn(LARVA,-21600, 179482, -5846, Rnd.get(360), false, 0);
			_Nurses.add((L2Attackable)addSpawn(NURSE,-22000, 179482, -5846, 0, false, 0));
			_Nurses.add((L2Attackable)addSpawn(NURSE,-21200, 179482, -5846, 0, false, 0));
			int radius = 400;
			for (int i = 0; i<QueenAntManager.MAX_NURSES; i++)
			{
				int x = (int) (radius*Math.cos(i*1.407)); //1.407~2pi/6
				int y = (int) (radius*Math.sin(i*1.407));
				_Nurses.add((L2Attackable)addSpawn(NURSE, npc.getX()+x, npc.getY()+y, npc.getZ(), 0, false, 0));
			}
			for (int i = 0; i < QueenAntManager.MAX_GUARDS; i++)
			{
				int x = (int) (radius*Math.cos(i*.7854)); //.7854~2pi/8
				int y = (int) (radius*Math.sin(i*.7854));
				_Minions.add((L2Attackable) addSpawn(ROYAL, npc.getX() + x, npc.getY() + y, npc.getZ(), 0, false, 0));
			}
			
		}
		
		return null;
	}

	public String onAdvEvent (String event, L2NpcInstance npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("action") && npc != null)
		{
			if (Rnd.get(3)==0)
			{
				if (Rnd.get(2)==0)
					npc.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
				else
					npc.broadcastPacket(new SocialAction(npc.getObjectId(), 4));
			}
		}
		else if (event.equalsIgnoreCase("despawn"))
		{
			queen.decayMe();
			larva.decayMe();
			for (L2Attackable mob : _Minions)
			{
				if (mob != null)
					mob.decayMe();
			}
			_Minions.clear();

			for (L2Attackable mob : _Nurses)
			{
				if (mob != null)
					mob.decayMe();
			}
			_Nurses.clear();
		}
		else if (event.equalsIgnoreCase("spawn_royal") && _isAlive && _Minions.size() < QueenAntManager.MAX_GUARDS)
			_Minions.add((L2Attackable) addSpawn(ROYAL, queen.getX(), queen.getY(), queen.getZ(), 0, false, 0));
		else if (event.equalsIgnoreCase("spawn_nurse") && _isAlive && _Nurses.size()<QueenAntManager.MAX_NURSES) {
			L2Attackable nurse = (L2Attackable) addSpawn(NURSE, queen.getX(), queen.getY(), queen.getZ(), 0, false, 0); 
			_Nurses.add(nurse);
		}
		return null;
	}
/*
	public String onFactionCall (L2NpcInstance npc, L2NpcInstance caller, L2PcInstance attacker, boolean isPet) 
	{
		if (caller == null || npc == null)
			return super.onFactionCall(npc, caller, attacker, isPet);
		int npcId = npc.getNpcId();
		int callerId = caller.getNpcId();
		if (npcId == NURSE)
		{
			if (callerId == LARVA)
			{
				npc.setTarget(caller);
				npc.doCast(SkillTable.getInstance().getInfo(4020,1));
				npc.doCast(SkillTable.getInstance().getInfo(4024,1));
				return null;
			}
			else if (callerId == QUEEN)
			{
				if (npc.getTarget() != null && npc.getTarget() instanceof L2NpcInstance)
				{
					if (((L2NpcInstance) npc.getTarget()).getNpcId() == LARVA)
						return null;
				}
				npc.setTarget(caller);
				npc.doCast(SkillTable.getInstance().getInfo(4020,1));
				return null;
			}
		} 
		return super.onFactionCall(npc, caller, attacker, isPet);
	}
*/
	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == LARVA) {
			if(npc.getCurrentHp()< npc.getMaxHp() * 0.6 )  {
				int radius = SkillTable.getInstance().getInfo(4020,1).getCastRange();
				for(L2Attackable a: _Nurses) {
					if(a.isInsideRadius(npc.getX(), npc.getY(), radius, false))
						a.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
					else
						if(a.getAI().getIntention() != CtrlIntention.AI_INTENTION_MOVE_TO) {
							a.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(npc.getLoc()));
						}
				}
			}
		}
		else if (npcId == QUEEN) {
			if(npc.getCurrentHp()< npc.getMaxHp()*0.3  && larva.getCurrentHp() > larva.getMaxHp()*0.3 ) {
				int radius = SkillTable.getInstance().getInfo(4020,1).getCastRange();
				for(L2Attackable a: _Nurses) {
					if(a.isInsideRadius(npc.getX(), npc.getY(), radius, false))
						a.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
					else
						if(a.getAI().getIntention() != CtrlIntention.AI_INTENTION_MOVE_TO) {
							a.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(npc.getLoc()));
						}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet) 
	{
		int npcId = npc.getNpcId();
		if (npcId == QUEEN)
		{
			_isAlive = false;
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			cancelQuestTimers("spawn_royal");
			cancelQuestTimers("spawn_nurse");
			startQuestTimer("despawn", 20000, null, null);
			larva.decayMe();
		}
		else if (_isAlive)
		{
			if (npcId == ROYAL)
			{
				_Minions.remove(npc);
				startQuestTimer("spawn_royal", (280+Rnd.get(40))*1000, npc, null);
			}
			else if (npcId == NURSE) {
				_Nurses.remove(npc);
				startQuestTimer("spawn_nurse", 30000, npc, null);
			}
		}
		return super.onKill(npc, killer, isPet);
	}
}