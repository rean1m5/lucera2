package ru.catssoftware.gameserver.handler;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public interface IExItemHandler {
	public void useItem(L2PcInstance player, L2ItemInstance item, String [] params);
	public int[] getItemIds();
}
