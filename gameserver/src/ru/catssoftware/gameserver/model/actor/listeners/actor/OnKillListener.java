package ru.catssoftware.gameserver.model.actor.listeners.actor;

/*
 * @author Ro0TT
 * @date 21.06.2012
 */

import ru.catssoftware.gameserver.model.L2Character;

public interface OnKillListener extends CharListener
{
	public void onKill(L2Character killer, L2Character victim);
}