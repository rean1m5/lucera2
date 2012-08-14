package ru.catssoftware.gameserver.model.actor.listeners;

/*
 * @author Ro0TT
 * @date 21.06.2012
 */

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.listeners.actor.OnDeathListener;
import ru.catssoftware.gameserver.model.actor.listeners.actor.OnKillListener;
import ru.catssoftware.gameserver.model.actor.listeners.actor.OnTeleportListener;

public class CharactionListeners extends ListenerList<L2Character>
{
	final static ListenerList<L2Character> global = new ListenerList<L2Character>();

	protected final L2Character actor;

	public CharactionListeners(L2Character actor)
	{
		this.actor = actor;
	}

	public L2Character getActor()
	{
		return actor;
	}

	public final static boolean addGlobal(Listener<L2Character> listener)
	{
		return global.add(listener);
	}

	public final static boolean removeGlobal(Listener<L2Character> listener)
	{
		return global.remove(listener);
	}

	public void onKill(L2Character victim)
	{
		if(!global.getListeners().isEmpty())
			for(Listener<L2Character> listener : global.getListeners())
			if (listener instanceof  OnKillListener)
				try
				{
					((OnKillListener) listener).onKill(getActor(), victim);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

		if(!getListeners().isEmpty())
			for(Listener<L2Character> listener : getListeners())
				if (listener instanceof  OnKillListener)
					try
					{
						((OnKillListener) listener).onKill(getActor(), victim);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
	}

	public void onDeath(L2Character killer)
	{
		if(!global.getListeners().isEmpty())
			for(Listener<L2Character> listener : global.getListeners())
				if (listener instanceof OnDeathListener)
					try
					{
						((OnDeathListener) listener).onDeath(getActor(), killer);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

		if(!getListeners().isEmpty())
			for(Listener<L2Character> listener : getListeners())
				if (listener instanceof OnDeathListener)
					try
					{
						((OnDeathListener) listener).onDeath(getActor(), killer);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
	}

	public void onTeleport()
	{
		if(!global.getListeners().isEmpty())
			for(Listener<L2Character> listener : global.getListeners())
				if (listener instanceof OnTeleportListener)
				try
				{
					((OnTeleportListener) listener).onTeleport(getActor());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

		if(!getListeners().isEmpty())
			for(Listener<L2Character> listener : getListeners())
				if (listener instanceof OnTeleportListener)
					try
					{
						((OnTeleportListener) listener).onTeleport(getActor());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
	}
}
