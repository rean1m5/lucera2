package ru.catssoftware.gameserver.model.actor.listeners.actor;

import ru.catssoftware.gameserver.model.L2Character;

/*
 * @author Ro0TT
 * @date 21.06.2012
 */

public interface OnDeathListener extends CharListener
{
	public void onDeath(L2Character victim, L2Character killer);
}
