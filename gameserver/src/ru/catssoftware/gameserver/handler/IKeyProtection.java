package ru.catssoftware.gameserver.handler;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public interface IKeyProtection
{
	public boolean access(L2PcInstance player, String target);
}
