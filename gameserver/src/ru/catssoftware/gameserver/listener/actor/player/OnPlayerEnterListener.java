package ru.catssoftware.gameserver.listener.actor.player;

import ru.catssoftware.gameserver.listener.PlayerListener;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

public interface OnPlayerEnterListener extends PlayerListener
{
	public void onPlayerEnter(L2PcInstance player);
}
