package ru.catssoftware.gameserver.mmocore;

import java.nio.channels.SocketChannel;

public interface IAcceptFilter
{
	public boolean accept(SocketChannel sc);
}
