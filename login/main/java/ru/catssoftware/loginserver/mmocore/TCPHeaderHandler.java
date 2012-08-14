package ru.catssoftware.loginserver.mmocore;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class TCPHeaderHandler<T extends MMOConnection<T>> extends HeaderHandler<T, TCPHeaderHandler<T>>
{
	public TCPHeaderHandler(TCPHeaderHandler<T> subHeaderHandler)
	{
		super(subHeaderHandler);
	}

	protected abstract HeaderInfo<T> handleHeader(SelectionKey key, ByteBuffer buf);
}