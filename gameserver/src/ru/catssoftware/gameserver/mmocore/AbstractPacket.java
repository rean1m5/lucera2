package ru.catssoftware.gameserver.mmocore;

import java.nio.ByteBuffer;

@SuppressWarnings("unchecked")
public abstract class AbstractPacket<T extends MMOClient>
{
	protected T _client;

	protected void setClient(T client)
	{
		_client = client;
	}

	public T getClient()
	{
		return _client;
	}

	protected SelectorThread<T> getCurrentSelectorThread()
	{
		Thread result = Thread.currentThread();
		return result instanceof SelectorThread ? (SelectorThread<T>) result : null;
	}

	protected abstract ByteBuffer getByteBuffer();
}