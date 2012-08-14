package ru.catssoftware.gameserver.model.actor.listeners.actor;

/*
 * @author Ro0TT
 * @date 21.06.2012
 */

import ru.catssoftware.gameserver.model.L2Character;

public interface OnTeleportListener
{
	public void onTeleport(L2Character teleporter);
}
