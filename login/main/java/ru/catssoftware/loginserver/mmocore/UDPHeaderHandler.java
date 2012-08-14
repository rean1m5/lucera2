package ru.catssoftware.loginserver.mmocore;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public abstract class UDPHeaderHandler<T extends MMOConnection<T>> extends HeaderHandler<T, UDPHeaderHandler<T>>
{
	public UDPHeaderHandler(UDPHeaderHandler<T> subHeaderHandler)
	{
		super(subHeaderHandler);
	}

	protected abstract HeaderInfo<T> handleHeader(ByteBuffer buf);

	protected abstract void onUDPConnection(SelectorThread<T> selector, DatagramChannel dc, SocketAddress key, ByteBuffer buf);
}