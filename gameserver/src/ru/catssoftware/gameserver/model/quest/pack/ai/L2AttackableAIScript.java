package ru.catssoftware.gameserver.model.quest.pack.ai; 

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;

public abstract class L2AttackableAIScript extends Quest
{
	public void registerMobs(int[] mobs)
	{
		for (int id : mobs)
		{
			this.addEventId(id, Quest.QuestEventType.ON_ATTACK);
			this.addEventId(id, Quest.QuestEventType.ON_KILL);
			this.addEventId(id, Quest.QuestEventType.ON_SPAWN);
			this.addEventId(id, Quest.QuestEventType.ON_SPELL_FINISHED);
			this.addEventId(id, Quest.QuestEventType.ON_SKILL_SEE);
			this.addEventId(id, Quest.QuestEventType.ON_FACTION_CALL);
			this.addEventId(id, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
		}
	}

	public L2AttackableAIScript(int questId, String name, String descr)
	{
		super(questId, name, descr);
	}

	public static void main(String[] args)
	{
	}

	public String onAdvEvent (String event, L2NpcInstance npc, L2PcInstance player)
	{
		return null;
	}
	
	public String onSpellFinished(L2NpcInstance npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}

	public String onSkillSee (L2NpcInstance npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) 
	{
		return null;
	}

	public String onFactionCall (L2NpcInstance npc, L2NpcInstance caller, L2PcInstance attacker, boolean isPet) 
	{
		return null;
	}

	public String onAggroRangeEnter (L2NpcInstance npc, L2PcInstance player, boolean isPet) 
	{
		return null; 
	}

	public String onSpawn (L2NpcInstance npc) 
	{
		return null; 
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		return null;
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet) 
	{
		return null; 
	}
}