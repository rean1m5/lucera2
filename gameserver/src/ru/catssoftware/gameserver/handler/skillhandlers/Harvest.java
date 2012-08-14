package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Attackable.RewardItem;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

public class Harvest implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.HARVEST };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance activePlayer = (L2PcInstance) activeChar;

		InventoryUpdate iu =  new InventoryUpdate();

		for (L2Character element : targets)
		{
			if (!(element instanceof L2MonsterInstance))
				continue;

			L2MonsterInstance target = (L2MonsterInstance) element;

			if (activePlayer != target.getSeeder())
			{
				activePlayer.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
				continue;
			}

			boolean send = false;
			int total = 0;
			int cropId = 0;

			if (target.isSeeded())
			{
				int penalty = getPenalty(activePlayer, target);
				if (Rnd.nextInt(99) < penalty)
				{
					RewardItem[] items = target.takeHarvest();
					if (items != null && items.length > 0)
					{
						for (RewardItem ritem : items)
						{
							cropId = ritem.getItemId(); // always got 1 type of
							ritem.setCount((int)((double)ritem.getCount()/100*penalty));
							// crop as reward
							if (activePlayer.isInParty())
								activePlayer.getParty().distributeItem(activePlayer, ritem, true, target);
							else
							{
								L2ItemInstance item = activePlayer.getInventory().addItem("Manor", ritem.getItemId(), ritem.getCount(), activePlayer, target);
								if (iu != null)
									iu.addItem(item);
								send = true;
								total += ritem.getCount();
							}
						}
						if (send)
						{
							SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
							smsg.addNumber(total);
							smsg.addItemName(cropId);
							activePlayer.sendPacket(smsg);
							if (activePlayer.getParty() != null)
							{
								smsg = new SystemMessage(SystemMessageId.S1_HARVESTED_S3_S2S);
								smsg.addString(activeChar.getName());
								smsg.addNumber(total);
								smsg.addItemName(cropId);
								activePlayer.getParty().broadcastToPartyMembers(activePlayer, smsg);
							}

							if (iu != null)
								activePlayer.sendPacket(iu);
							else
								activePlayer.sendPacket(new ItemList(activePlayer, false));
						}
					}
				}
				else
					activePlayer.sendPacket(SystemMessageId.THE_HARVEST_HAS_FAILED);
			}
			else
				activePlayer.sendPacket(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
		}
	}
	
	private int getPenalty(L2PcInstance activePlayer, L2MonsterInstance target)
	{
		int basicSuccess = 100;
		int levelPlayer = activePlayer.getLevel();
		int levelTarget = target.getLevel();

		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
			diff = -diff;

		// apply penalty, target <=> player levels
		// 5% penalty for each level
		if (diff > 5)
			basicSuccess -= (diff - 5) * 5;

		// success rate cant be less than 1%
		if (basicSuccess < 1)
			basicSuccess = 1;
		return basicSuccess;
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}