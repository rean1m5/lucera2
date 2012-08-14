package ru.catssoftware.loginserver.mmocore;

import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

import javolution.util.FastList;

public abstract class MMOConnection<T extends MMOConnection<T>>
{
	private final SelectorThread<T> _selectorThread;
	private final ISocket _socket;

	private FastList<SendablePacket<T>> _sendQueue;
	private final SelectionKey _selectionKey;

	private ByteBuffer _readBuffer;

	private ByteBuffer _primaryWriteBuffer;
	private ByteBuffer _secondaryWriteBuffer;

	private long _timeClosed = -1;

	protected MMOConnection(SelectorThread<T> selectorThread, ISocket socket, SelectionKey key)
	{
		_selectorThread = selectorThread;
		_socket = socket;
		_selectionKey = key;
	}

	public synchronized void sendPacket(SendablePacket<T> sp)
	{
		if (isClosed())
			return;

		try
		{
			getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
			getSendQueue2().addLast(sp);
		}
		catch (CancelledKeyException e)
		{
		}
	}

	private SelectorThread<T> getSelectorThread()
	{
		return _selectorThread;
	}

	SelectionKey getSelectionKey()
	{
		return _selectionKey;
	}

	void enableReadInterest()
	{
		try
		{
			getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_READ);
		}
		catch (CancelledKeyException e)
		{
		}
	}

	void disableReadInterest()
	{
		try
		{
			getSelectionKey().interestOps(getSelectionKey().interestOps() & ~SelectionKey.OP_READ);
		}
		catch (CancelledKeyException e)
		{
		}
	}

	void enableWriteInterest()
	{
		try
		{
			getSelectionKey().interestOps(getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
		}
		catch (CancelledKeyException e)
		{
		}
	}

	void disableWriteInterest()
	{
		try
		{
			getSelectionKey().interestOps(getSelectionKey().interestOps() & ~SelectionKey.OP_WRITE);
		}
		catch (CancelledKeyException e)
		{
		}
	}

	public ISocket getSocket()
	{
		return _socket;
	}

	WritableByteChannel getWritableChannel()
	{
		return _socket.getWritableByteChannel();
	}

	ReadableByteChannel getReadableByteChannel()
	{
		return _socket.getReadableByteChannel();
	}

	synchronized FastList<SendablePacket<T>> getSendQueue2()
	{
		if (_sendQueue == null)
			_sendQueue = new FastList<SendablePacket<T>>();

		return _sendQueue;
	}

	void createWriteBuffer(ByteBuffer buf)
	{
		if (_primaryWriteBuffer == null)
		{
			_primaryWriteBuffer = getSelectorThread().getPooledBuffer();
			_primaryWriteBuffer.put(buf);
		}
		else
		{
			ByteBuffer temp = getSelectorThread().getPooledBuffer();
			temp.put(buf);

			int remaining = temp.remaining();
			_primaryWriteBuffer.flip();
			int limit = _primaryWriteBuffer.limit();

			if (remaining >= _primaryWriteBuffer.remaining())
			{
				temp.put(_primaryWriteBuffer);
				getSelectorThread().recycleBuffer(_primaryWriteBuffer);
				_primaryWriteBuffer = temp;
			}
			else
			{
				_primaryWriteBuffer.limit(remaining);
				temp.put(_primaryWriteBuffer);
				_primaryWriteBuffer.limit(limit);
				_primaryWriteBuffer.compact();
				_secondaryWriteBuffer = _primaryWriteBuffer;
				_primaryWriteBuffer = temp;
			}
		}
	}

	boolean hasPendingWriteBuffer()
	{
		return _primaryWriteBuffer != null;
	}

	void movePendingWriteBufferTo(ByteBuffer dest)
	{
		_primaryWriteBuffer.flip();
		dest.put(_primaryWriteBuffer);
		getSelectorThread().recycleBuffer(_primaryWriteBuffer);
		_primaryWriteBuffer = _secondaryWriteBuffer;
		_secondaryWriteBuffer = null;
	}

	void setReadBuffer(ByteBuffer buf)
	{
		_readBuffer = buf;
	}

	ByteBuffer getReadBuffer()
	{
		return _readBuffer;
	}

	boolean isClosed()
	{
		return _timeClosed != -1;
	}

	boolean closeTimeouted()
	{
		return System.currentTimeMillis() > _timeClosed + 10000;
	}
	

	public synchronized void closeNow()
	{
		if (isClosed())
			return;

		_timeClosed = System.currentTimeMillis();
		getSendQueue2().clear();
		disableWriteInterest();
		getSelectorThread().closeConnection(this);
	}

	public synchronized void close(SendablePacket<T> sp)
	{
		if (isClosed())
			return;

		getSendQueue2().clear();
		sendPacket(sp);
		_timeClosed = System.currentTimeMillis();
		getSelectorThread().closeConnection(this);
	}

	void releaseBuffers()
	{
		if (_primaryWriteBuffer != null)
		{
			getSelectorThread().recycleBuffer(_primaryWriteBuffer);
			_primaryWriteBuffer = null;
			if (_secondaryWriteBuffer != null)
			{
				getSelectorThread().recycleBuffer(_secondaryWriteBuffer);
				_secondaryWriteBuffer = null;
			}
		}

		if (_readBuffer != null)
		{
			getSelectorThread().recycleBuffer(_readBuffer);
			_readBuffer = null;
		}
	}

	protected abstract void onDisconnection();

	protected abstract void onForcedDisconnection();

	protected abstract boolean decrypt(ByteBuffer buf, int size);

	protected abstract boolean encrypt(ByteBuffer buf, int size);
}