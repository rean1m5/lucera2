package ru.catssoftware.gameserver.listener.actor;

import ru.catssoftware.gameserver.listener.CharListener;
import ru.catssoftware.gameserver.model.L2Character;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

public interface OnDeathListener extends CharListener
{
	public void onDeath(L2Character actor, L2Character killer);
}
