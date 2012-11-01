package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.xml.ExtractableItemsData;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.items.model.L2ExtractableItem;
import ru.catssoftware.gameserver.items.model.L2ExtractableProductItem;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;

public class ExtractableItems implements IItemHandler
{
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		int itemId = item.getItemId();
		L2Skill skill = null;
		L2ExtractableItem exitem = ExtractableItemsData.getInstance().getExtractableItem(itemId);
		for (L2ExtractableProductItem expi : exitem.getProductItemsArray())
		{
			skill = expi.getSkill();
			if (skill != null)
				activeChar.useMagic(skill, false, false);
			return;
		}
	}

	public int[] getItemIds()
	{
		return ExtractableItemsData.getInstance().itemIDs();
	}
}