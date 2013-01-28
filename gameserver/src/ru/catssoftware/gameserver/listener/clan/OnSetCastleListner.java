package ru.catssoftware.gameserver.listener.clan;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

import ru.catssoftware.gameserver.listener.ClanListener;
import ru.catssoftware.gameserver.model.L2Clan;

public interface OnSetCastleListner extends ClanListener
{
	public void setCastle(L2Clan clan, int castleId);
}
