package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class L2PaganZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (character.getLevel() >= 73)
			{
				L2PcInstance player = (L2PcInstance) character;
				L2ItemInstance item = player.getInventory().getItemByItemId(8064);
				if (item != null)
				{
					player.destroyItemByItemId("Mark", 8064, 1, player, true);
					L2ItemInstance fadedMark = player.getInventory().addItem("Faded Mark", 8065, 1, player, player);

					SystemMessage msg = new SystemMessage(SystemMessageId.EARNED_S1);
					msg.addItemName(fadedMark);
					player.sendPacket(msg);

					InventoryUpdate u = new InventoryUpdate();
					u.addNewItem(fadedMark);
					player.sendPacket(u);
				}
			}
			else
				character.teleToLocation(TeleportWhereType.Town);
		}
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		super.onExit(character);
	}
}