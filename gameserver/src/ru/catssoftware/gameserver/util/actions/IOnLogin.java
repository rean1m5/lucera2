package ru.catssoftware.gameserver.util.actions;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/*
 * @author Ro0TT
 * @date 14.05.2012
 */

public interface IOnLogin
{
	/*
	 * @author Ro0TT
	 * Интерфейс входа игрока в игру.
	 */
	
	public void intoTheGame(L2PcInstance player);
}