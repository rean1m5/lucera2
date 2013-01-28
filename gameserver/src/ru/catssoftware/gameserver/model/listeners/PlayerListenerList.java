package ru.catssoftware.gameserver.model.listeners;

/*
 * @author Ro0TT
 * @date 13.01.2013
 */

import ru.catssoftware.gameserver.listener.actor.player.OnPlayerEnterListener;
import ru.catssoftware.gameserver.listener.actor.player.OnPlayerExitListener;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.listener.Listener;

public class PlayerListenerList extends CharListenerList
{
	public PlayerListenerList(L2PcInstance actor) {
		super(actor);
	}

	@Override
	public L2PcInstance getActor()
	{
		return actor.getPlayer();
	}

	public void onEnter()
	{
		if(!global.getListeners().isEmpty())
			for(Listener<L2Character> listener : global.getListeners())
				if(OnPlayerEnterListener.class.isInstance(listener))
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());

		if(!getListeners().isEmpty())
			for(Listener<L2Character> listener : getListeners())
				if(OnPlayerEnterListener.class.isInstance(listener))
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());
	}

	public void onExit()
	{
		if(!global.getListeners().isEmpty())
			for(Listener<L2Character> listener : global.getListeners())
				if(OnPlayerExitListener.class.isInstance(listener))
					((OnPlayerExitListener) listener).onPlayerExit(getActor());

		if(!getListeners().isEmpty())
			for(Listener<L2Character> listener : getListeners())
				if(OnPlayerExitListener.class.isInstance(listener))
					((OnPlayerExitListener) listener).onPlayerExit(getActor());
	}
}
