package ru.catssoftware.gameserver.handler;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;

public interface IItemHandler
{
	public static Logger	_log	= Logger.getLogger(IItemHandler.class.getName());

	public void useItem(L2PlayableInstance playable, L2ItemInstance item);

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean animation);

	public int[] getItemIds();
}