package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Sweep implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SWEEP };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;
		InventoryUpdate iu =new InventoryUpdate();
		boolean send = false;

		for (L2Character element : targets)
		{
			if (!(element instanceof L2Attackable))
				continue;

			L2Attackable target = (L2Attackable) element;

			L2Attackable.RewardItem[] items = null;
			boolean isSweeping = false;
			synchronized (target)
			{
				if (target.isSweepActive())
				{
					items = target.takeSweep();
					isSweeping = true;
				}
			}
			if (isSweeping)
			{
				if (items == null || items.length == 0)
					continue;

				for (L2Attackable.RewardItem ritem : items)
				{
					if (player.isInParty())
						player.getParty().distributeItem(player, ritem, true, target);
					else
					{
						if (player.getInventory().validateCapacityByItemId(ritem.getItemId()))
						{
							L2ItemInstance item = player.getInventory().addItem("Sweep", ritem.getItemId(), ritem.getCount(), player, target);
							if (iu != null)
								iu.addItem(item);
							send = true;

							SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
							smsg.addNumber(ritem.getCount());
							smsg.addItemName(item);
							player.sendPacket(smsg);
						}
					}
				}
			}
			target.endDecayTask();
			if (send)
			{
				if (iu != null)
					player.sendPacket(iu);
				else
					player.sendPacket(new ItemList(player, false));
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}