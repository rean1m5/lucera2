package ru.catssoftware.gameserver.model.listeners;

import ru.catssoftware.gameserver.listener.actor.OnDeathListener;
import ru.catssoftware.gameserver.listener.actor.OnKillListener;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.listener.Listener;
import ru.catssoftware.listener.ListenerList;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

public class CharListenerList extends ListenerList<L2Character>
{
	public CharListenerList(L2Character actor)
	{
		this.actor = actor;
	}

	protected L2Character actor;

	public L2Character getActor()
	{
		return actor;
	}

	final static ListenerList<L2Character> global = new ListenerList<L2Character>();

	public static void addGlobal(Listener<L2Character> listener)
	{
		global.add(listener);
	}

	public static void removeGlobal(Listener<L2Character> listener)
	{
		global.remove(listener);
	}

	public void onDeath(L2Character killer)
	{
		for(Listener<L2Character> listener : getListeners())
			if(OnDeathListener.class.isInstance(listener))
				((OnDeathListener) listener).onDeath(actor, killer);

		for (Listener<L2Character> listener : global.getListeners())
			if(OnDeathListener.class.isInstance(listener))
				((OnDeathListener) listener).onDeath(actor, killer);
	}

	public void onKill(L2Character victim)
	{
		for(Listener<L2Character> listener : getListeners())
			if(OnKillListener.class.isInstance(listener))
				((OnKillListener) listener).onKill(actor, victim);

		for (Listener<L2Character> listener : global.getListeners())
			if(OnKillListener.class.isInstance(listener))
				((OnKillListener) listener).onKill(actor, victim);
	}
}