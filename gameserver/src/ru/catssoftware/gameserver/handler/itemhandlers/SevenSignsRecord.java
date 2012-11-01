package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.serverpackets.SSQStatus;

public class SevenSignsRecord implements IItemHandler
{
	private static final int[]	ITEM_IDS	= { 5707 };
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;

		if (playable.isPlayer())
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;

		SSQStatus ssqs = new SSQStatus(activeChar, 1);
		activeChar.sendPacket(ssqs);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}