package ru.catssoftware.gameserver.util;

/*
 * @author Ro0TT
 * @date 15.01.2012
 */

import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;

public abstract class TalkNpc extends Quest
{
	public TalkNpc(String name)
	{
		super(-1, name, "custom");
	}

	protected void addNpc(int ...npcIds)
	{
		for (int npcId : npcIds)
		{
			addFirstTalkId(npcId);
			addTalkId(npcId);
		}
	}

	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		return onTalk(npc,player);
	}

	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance talker)
	{
		QuestState qs = talker.getQuestState(getName());
		if (qs==null)
			newQuestState(talker);
		return onTalk(talker, npc);
	}

	@Override
	public String onEvent(String event, QuestState qs)
	{
		return onEvent(qs.getPlayer(), event);
	}

	public String onTalk(L2PcInstance player, L2NpcInstance npc)
	{
		return onEvent(player, "main");
	}

	public abstract String onEvent(L2PcInstance player, String command);
	
	protected String getAddrPage(String name)
	{
		return "data/html/mods/" + getName() + "/" + name + ".htm";
	}
	
	protected String getContentPage(L2PcInstance player, String name)
	{
		return HtmCache.getInstance().getHtm(getAddrPage(name), player);
	}

	protected int parse(String command, int def)
	{
		try
		{
			def = Integer.parseInt(command);
		}
		catch (Exception ignore)
		{}

		return def;
	}

}
