package ru.catssoftware.loginserver.mmocore;

import java.nio.channels.SocketChannel;

public interface IAcceptFilter
{
	public boolean accept(SocketChannel sc);
}