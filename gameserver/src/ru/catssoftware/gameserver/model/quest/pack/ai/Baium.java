package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BaiumManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.tools.random.Rnd;


/*
 * @author m095 (L2CatsSoftware)
 * @ver 1.1
 * @date: 30.01.10
 */

public class Baium extends Quest
{
	private final int BAIUM = 29020;
	private final int ARCHANGEL = 29021;
	private final int STATUE = 29025;
	private final int VORTEX = 31862;
	private final int FABRIC = 4295;

	public static String QUEST = "baium";
	public Baium ()
	{
		super(-1, QUEST, "ai");
		addStartNpc(STATUE);
		addStartNpc(VORTEX);
		addTalkId(STATUE);
		addTalkId(VORTEX);
		addAttackId(BAIUM);
		addAttackId(ARCHANGEL);
		addKillId(BAIUM);
		addSkillSeeId(BAIUM);
	}
	
	public String onTalk(L2NpcInstance npc,L2PcInstance player)
	{
		String htmltext = "<html><body>Вы не соответствуете требованиям данного NPC.</body></html>";
		
		QuestState st = player.getQuestState(QUEST);
		
		if (st == null)
			st = newQuestState(player);

		int npcId = npc.getNpcId();
		if (npcId == STATUE)
		{
			if (st.getInt("ok") == 1 || player.isGM())
			{
				if (!npc.isBusy())
				{
					BaiumManager.getInstance().wakeBaium(player);
					npc.setBusy(true);
					npc.setBusyMessage("Attending another player's request");
					htmltext = "<html><body>Вы разбудили короля Баюма!</body></html>";
				}
			}
			else
			{
				st.exitQuest(true);
				htmltext = "<html><body>Вы не можете разбудить короля Баюма!</body></html>";
				return htmltext;
			}
		}
		else if (npcId == VORTEX)
		{
			if (BaiumManager.getInstance().isEnableEnterToLair())
			{
				if (player.isFlying())
				{
					htmltext = "<html><body>Angelic Vortex:<br>Вы не можете войти в полете.</body></html>";
					return htmltext;
				}
				if (st.getQuestItemsCount(FABRIC) >= 1 )
				{
					st.takeItems(FABRIC,1);
					st.set("ok","1");
					player.teleToLocation(113100,14500,10077);
					htmltext = "<html><body>Angelic Vortex:<br>Вы успешно вошли в логово Баюма.</body></html>";
				}
				else
				{
					htmltext = "<html><body>Angelic Vortex:<br>У вас нет необходых вещей.</body></html>";
					return htmltext;
				}
			}
			else
			{
				htmltext = "<html><body>Angelic Vortex:<br>Вы не можете войти в данный момент.</body></html>";
				return htmltext;
			}
		}
		return htmltext;
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();

		if (npcId == ARCHANGEL)
		{
			if (Rnd.get(100) < 10)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(4132,1);
				if (skill != null)
				{
					npc.setTarget(attacker);
					npc.doCast(skill);
				}
			}
			if (Rnd.get(100) < 5 && ((npc.getStatus().getCurrentHp() / npc.getMaxHp())*100) < 50)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(4133,1);
				if (skill != null)
				{
					npc.setTarget(npc);
					npc.doCast(skill);
				}
			}
		}

		if (npc.getNpcId() == BAIUM)
		{
			BaiumManager.getInstance()._lastAttackTime = System.currentTimeMillis();
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}


	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == BAIUM)
		{
			BaiumManager.getInstance().setCubeSpawn();
		}
		return super.onKill(npc, killer, isPet);
	}
	public String onSkillSee(L2NpcInstance npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) {
		if(caster.getZ()<10055 && BaiumManager.getInstance().checkIfInZone(caster.getTarget())) 
			caster.reduceCurrentHp(caster.getMaxHp() + caster.getMaxCp() + 1, npc);
		return null;
	}
}