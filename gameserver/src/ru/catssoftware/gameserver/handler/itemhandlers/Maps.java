package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.serverpackets.ShowMiniMap;

public class Maps implements IItemHandler
{
	// all the items ids that this handler knowns
	private static final int[]	ITEM_IDS	= { 1665, 1863 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;

		playable.sendPacket(new ShowMiniMap());
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}