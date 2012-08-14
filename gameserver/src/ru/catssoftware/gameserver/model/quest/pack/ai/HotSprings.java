package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.tools.random.Rnd;
import javolution.util.FastList;


public class HotSprings extends Quest
{
	private int NPC[] = {21314, 21316, 21317, 21319, 21321, 21322};
//	private FastList<Integer>	LIST_MOB_RHEUMATISM			= new FastList<Integer>();
	private FastList<Integer>	LIST_MOB_HOLERA				= new FastList<Integer>();
	private FastList<Integer>	LIST_MOB_FLY				= new FastList<Integer>();
	private FastList<Integer>	LIST_MOB_MALARIA			= new FastList<Integer>();
	
	public HotSprings()
	{
		super(-1, "HotSprings", "ai");
		for (int id:NPC)
			addAttackId(id);
		addMobs();
	}

	private void addMobs()
	{
//		LIST_MOB_RHEUMATISM.add(21314);
//		LIST_MOB_RHEUMATISM.add(21321);
		LIST_MOB_MALARIA.add(21314);
		LIST_MOB_MALARIA.add(21316);
		LIST_MOB_MALARIA.add(21317);
		LIST_MOB_MALARIA.add(21319);
		LIST_MOB_MALARIA.add(21321);
		LIST_MOB_MALARIA.add(21322);
		LIST_MOB_HOLERA.add(21316);
		LIST_MOB_HOLERA.add(21319);
		LIST_MOB_FLY.add(21317);
		LIST_MOB_FLY.add(21322);
	}

	public String onAttack (L2NpcInstance npc, L2PcInstance player, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		//Effect Rheumatism
/*		if (LIST_MOB_RHEUMATISM.contains(npcId))
		{
			if (Rnd.get(100) < Config.HS_DEBUFF_CHANCE)
			{
				if (player.getFirstEffect(4551)!=null)
				{
					int rheumatism = player.getFirstEffect(4551).getLevel();
					if (Rnd.get(100) < 50)
					{
						if (rheumatism < 10)
						{
							int lvl = rheumatism + 1;
							npc.setTarget(player);
							player.stopSkillId(4551);
							SkillTable.getInstance().getInfo(4551,lvl).getEffects(npc,player);
						}
						else
						{
							npc.setTarget(player);
							player.stopSkillId(4551);
							SkillTable.getInstance().getInfo(4551,1).getEffects(npc,player);
						}
					}
				}
				else
				{
					npc.setTarget(player);
					SkillTable.getInstance().getInfo(4551,1).getEffects(npc,player);
				}
			}
		} */
		// Effect Holera
		if (LIST_MOB_HOLERA.contains(npcId))
		{
			if (Rnd.get(100) < Config.HS_DEBUFF_CHANCE)
			{
				if (player.getFirstEffect(4552)!=null)
				{
					int holera = player.getFirstEffect(4552).getLevel();
					if (Rnd.get(100) < 50)
					{
						if (holera < 10)
						{
							int lvl = holera + 1;
							npc.setTarget(player);
							player.stopSkillId(4552);
							SkillTable.getInstance().getInfo(4552,lvl).getEffects(npc,player);
						}
						else
						{
							npc.setTarget(player);
							player.stopSkillId(4552);
							SkillTable.getInstance().getInfo(4552,1).getEffects(npc,player);
						}
					}
				}
				else
				{
					npc.setTarget(player);
					SkillTable.getInstance().getInfo(4552,1).getEffects(npc,player);
				}
			}
		}
		// Effect Fly
		if (LIST_MOB_FLY.contains(npcId))
		{
			if (Rnd.get(100) < Config.HS_DEBUFF_CHANCE)
			{
				if (player.getFirstEffect(4553)!=null)
				{
					int fly = player.getFirstEffect(4553).getLevel();
					if (Rnd.get(100) < 50)
					{
						if (fly < 10)
						{
							int lvl = fly + 1;
							npc.setTarget(player);
							player.stopSkillId(4553);
							SkillTable.getInstance().getInfo(4553,lvl).getEffects(npc,player);
						}
						else
						{
							npc.setTarget(player);
							player.stopSkillId(4553);
							SkillTable.getInstance().getInfo(4553,1).getEffects(npc,player);
						}
					}
				}
				else
				{
					npc.setTarget(player);
					SkillTable.getInstance().getInfo(4553,1).getEffects(npc,player);
				}
			}
		}
		// Effect Malaria
		if (LIST_MOB_MALARIA.contains(npcId))
		{
			if (Rnd.get(100) < Config.HS_DEBUFF_CHANCE)
			{
				if (player.getFirstEffect(4554)!=null)
				{
					int malaria = player.getFirstEffect(4554).getLevel();
					if (Rnd.get(100) < 50)
					{
						if (malaria < 10)
						{
							int lvl = malaria + 1;
							npc.setTarget(player);
							player.stopSkillId(4554);
							SkillTable.getInstance().getInfo(4554,lvl).getEffects(npc,player);
						}
						else
						{
							npc.setTarget(player);
							player.stopSkillId(4554);
							SkillTable.getInstance().getInfo(4554,1).getEffects(npc,player);
						}
					}
				}
				else
				{
					npc.setTarget(player);
					SkillTable.getInstance().getInfo(4554,1).getEffects(npc,player);
				}
			}
		}
		return super.onAttack(npc, player, damage, isPet);
	}
}