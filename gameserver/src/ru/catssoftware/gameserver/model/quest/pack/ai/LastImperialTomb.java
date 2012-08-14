package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.instancemanager.lastimperialtomb.LastImperialTombManager;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;

public class LastImperialTomb extends Quest
{
	private int GUIDE = 32011;
	
	public LastImperialTomb()
	{
		super(-1, "lastimperialtomb", "ai");
		addStartNpc(GUIDE);
		addTalkId(GUIDE);
		addKillId(18328);
		addKillId(18339);
		addKillId(18334);
	}
	public String onKill (L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == 18328)
			LastImperialTombManager.getInstance().onKillHallAlarmDevice();
		else if (npcId == 18339)
			LastImperialTombManager.getInstance().onKillDarkChoirPlayer();
		else if (npcId == 18334)
			LastImperialTombManager.getInstance().onKillDarkChoirCaptain();
		return super.onKill(npc, player, isPet);
	}

	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		QuestState st = player.getQuestState("lastimperialtomb");
		if (st==null)
			return null;
	
		int npcId = npc.getNpcId();
		if (npcId == GUIDE)  // Frintezza Teleporter
		{
			if (player.isFlying())
				return "<html><body>Imperial Tomb Guide:<br>Вы не можете войти во время полета.</body></html>";

			if (Config.LIT_REGISTRATION_MODE == 0)
			{
				if (LastImperialTombManager.getInstance().tryRegistrationCc(player))
					LastImperialTombManager.getInstance().registration(player,npc);
			}
			else if (Config.LIT_REGISTRATION_MODE == 1)
			{
				if (LastImperialTombManager.getInstance().tryRegistrationPt(player))
					LastImperialTombManager.getInstance().registration(player,npc);
			}
			else if (Config.LIT_REGISTRATION_MODE == 2)
			{
				if (LastImperialTombManager.getInstance().tryRegistrationPc(player))
					LastImperialTombManager.getInstance().registration(player,npc);
			}
		}
		return "";
	} 
}
