package ru.catssoftware.gameserver.util.actions;

/*
 * @author Ro0TT
 * @date 27.10.2012
 */

import ru.catssoftware.gameserver.model.L2Character;

public interface IOnKill
{
	public void onKill(L2Character killer, L2Character actor);
}
