package ru.catssoftware.gameserver.mmocore;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

@SuppressWarnings("unchecked")
public abstract class TCPHeaderHandler<T extends MMOClient> extends HeaderHandler<T, TCPHeaderHandler<T>>
{
	/**
	 * @param subHeaderHandler
	 */
	public TCPHeaderHandler(TCPHeaderHandler<T> subHeaderHandler)
	{
		super(subHeaderHandler);
	}

	private final HeaderInfo<T> _headerInfoReturn = new HeaderInfo<T>();

	public abstract HeaderInfo handleHeader(SelectionKey key, ByteBuffer buf);

	/**
	 * @return the headerInfoReturn
	 */
	protected final HeaderInfo<T> getHeaderInfoReturn()
	{
		return _headerInfoReturn;
	}
}
