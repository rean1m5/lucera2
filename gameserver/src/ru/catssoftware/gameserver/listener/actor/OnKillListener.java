package ru.catssoftware.gameserver.listener.actor;

import ru.catssoftware.gameserver.listener.CharListener;
import ru.catssoftware.gameserver.model.L2Character;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

public interface OnKillListener extends CharListener
{
	public void onKill(L2Character actor, L2Character victim);
}
