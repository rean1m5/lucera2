package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FeedableBeastInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class BeastSpice implements IItemHandler
{
	// Golden Spice, Crystal Spice
	private static final int[]	ITEM_IDS	= { 6643, 6644 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		if (!(activeChar.getTarget() instanceof L2FeedableBeastInstance))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		int itemId = item.getItemId();
		if (itemId == 6643)
			activeChar.useMagic(SkillTable.getInstance().getInfo(2188, 1), false, false);
		else if (itemId == 6644)
			activeChar.useMagic(SkillTable.getInstance().getInfo(2189, 1), false, false);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}