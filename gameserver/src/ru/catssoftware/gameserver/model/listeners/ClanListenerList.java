package ru.catssoftware.gameserver.model.listeners;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

import ru.catssoftware.gameserver.listener.clan.OnSetCastleListner;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.listener.Listener;
import ru.catssoftware.listener.ListenerList;

public class ClanListenerList extends ListenerList<L2Clan>
{
	public ClanListenerList(L2Clan clan)
	{
		this.clan = clan;
	}

	final static ListenerList<L2Clan> global = new ListenerList<L2Clan>();

	private L2Clan clan;

	public static void addGlobal(Listener<L2Clan> listener)
	{
		global.add(listener);
	}

	public static void removeGlobal(Listener<L2Clan> listener)
	{
		global.remove(listener);
	}

	public void setCastle(int castleId)
	{
		for(Listener<L2Clan> listener : getListeners())
			if(OnSetCastleListner.class.isInstance(listener))
				((OnSetCastleListner) listener).setCastle(clan, castleId);

		for (Listener<L2Clan> listener : global.getListeners())
			if(OnSetCastleListner.class.isInstance(listener))
				((OnSetCastleListner) listener).setCastle(clan, castleId);
	}
}