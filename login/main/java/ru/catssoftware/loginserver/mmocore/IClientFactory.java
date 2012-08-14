package ru.catssoftware.loginserver.mmocore;

import java.nio.channels.SelectionKey;

public interface IClientFactory<T extends MMOConnection<T>>
{
	public T create(SelectorThread<T> selectorThread, ISocket socket, SelectionKey key);
}