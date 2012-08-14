package ru.catssoftware.loginserver.mmocore;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import javolution.util.FastList;

public final class SelectorThread<T extends MMOConnection<T>> extends Thread
{
	private final Selector _selector;

	// Implementations
	private final IPacketHandler<T> _packetHandler;
	private final IPacketHandler<T> _udpPacketHandler;
	private final IMMOExecutor<T> _executor;
	private final IClientFactory<T> _clientFactory;
	private final IAcceptFilter _acceptFilter;
	private final UDPHeaderHandler<T> _udpHeaderHandler;
	private final TCPHeaderHandler<T> _tcpHeaderHandler;

	private volatile boolean _shutdown;

	// Pending Close
	private final FastList<MMOConnection<T>> _pendingClose = new FastList<MMOConnection<T>>();

	// Configs
	private final int HELPER_BUFFER_SIZE;
	private final int HELPER_BUFFER_COUNT;
	private final int MAX_SEND_PER_PASS;
	private final int HEADER_SIZE = 2;
	private final ByteOrder BYTE_ORDER;
	private final long SLEEP_TIME;

	// MAIN BUFFERS
	private final ByteBuffer DIRECT_WRITE_BUFFER;
	private final ByteBuffer WRITE_BUFFER;
	private final ByteBuffer READ_BUFFER;

	// ByteBuffers General Purpose Pool
	private final FastList<ByteBuffer> _bufferPool = new FastList<ByteBuffer>();

	public SelectorThread(SelectorConfig<T> sc, IMMOExecutor<T> executor, IClientFactory<T> clientFactory, IAcceptFilter acceptFilter) throws IOException
	{
		HELPER_BUFFER_SIZE = sc.getHelperBufferSize();
		HELPER_BUFFER_COUNT = sc.getHelperBufferCount();
		MAX_SEND_PER_PASS = sc.getMaxSendPerPass();
		BYTE_ORDER = sc.getByteOrder();
		SLEEP_TIME = sc.getSelectorSleepTime();

		DIRECT_WRITE_BUFFER = ByteBuffer.allocateDirect(sc.getWriteBufferSize()).order(BYTE_ORDER);
		WRITE_BUFFER = ByteBuffer.wrap(new byte[sc.getWriteBufferSize()]).order(BYTE_ORDER);
		READ_BUFFER = ByteBuffer.wrap(new byte[sc.getReadBufferSize()]).order(BYTE_ORDER);

		_udpHeaderHandler = sc.getUDPHeaderHandler();
		_tcpHeaderHandler = sc.getTCPHeaderHandler();
		initBufferPool();
		_acceptFilter = acceptFilter;
		_packetHandler = sc.getTCPPacketHandler();
		_udpPacketHandler = sc.getUDPPacketHandler();
		_clientFactory = clientFactory;
		_executor = executor;
		setName("SelectorThread-" + getId());
		_selector = Selector.open();
	}

	private void initBufferPool()
	{
		for (int i = 0; i < HELPER_BUFFER_COUNT; i++)
			getFreeBuffers().addLast(ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER));
	}

	public void openServerSocket(InetAddress address, int tcpPort) throws IOException
	{
		ServerSocketChannel selectable = ServerSocketChannel.open();
		selectable.configureBlocking(false);

		ServerSocket ss = selectable.socket();
		if (address == null)
			ss.bind(new InetSocketAddress(tcpPort));
		else
			ss.bind(new InetSocketAddress(address, tcpPort));
		selectable.register(getSelector(), SelectionKey.OP_ACCEPT);
		
	}

	public void openDatagramSocket(InetAddress address, int udpPort) throws IOException
	{
		DatagramChannel selectable = DatagramChannel.open();
		selectable.configureBlocking(false);

		DatagramSocket ss = selectable.socket();
		if (address == null)
			ss.bind(new InetSocketAddress(udpPort));
		else
			ss.bind(new InetSocketAddress(address, udpPort));
		selectable.register(getSelector(), SelectionKey.OP_READ);
	}

	ByteBuffer getPooledBuffer()
	{
		if (getFreeBuffers().isEmpty())
			return ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER);
		else
			return getFreeBuffers().removeFirst();
	}

	void recycleBuffer(ByteBuffer buf)
	{
		if (getFreeBuffers().size() < HELPER_BUFFER_COUNT)
		{
			buf.clear();
			getFreeBuffers().addLast(buf);
		}
	}

	private FastList<ByteBuffer> getFreeBuffers()
	{
		return _bufferPool;
	}

	@Override
	public void run()
	{
		// main loop
		for (;;)
		{
			// check for shutdown
			if (isShuttingDown())
			{
				close();
				break;
			}

			boolean hasPendingWrite = false;
			
			try
			{
				if (getSelector().selectNow() > 0)
				{
					Set<SelectionKey> keys = getSelector().selectedKeys();

					for (SelectionKey key : keys)
					{
						switch (key.readyOps())
						{
							case SelectionKey.OP_CONNECT:
								finishConnection(key);
								break;
							case SelectionKey.OP_ACCEPT:
								acceptConnection(key);
								break;
							case SelectionKey.OP_READ:
								readPacket(key);
								break;
							case SelectionKey.OP_WRITE:
								hasPendingWrite |= writePacket2(key);
								break;
							case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
								hasPendingWrite |= writePacket2(key);
								// key might have been invalidated on writePacket
								if (key.isValid())
									readPacket(key);
								break;
						}
					}

					keys.clear();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			closePendingConnections();

			try
			{
				if (!hasPendingWrite)
					Thread.sleep(SLEEP_TIME);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			closePendingConnections();
		}
	}

	private void closePendingConnections()
	{
		// process pending close
		synchronized (getPendingClose())
		{
			for (FastList.Node<MMOConnection<T>> n = getPendingClose().head(), end = getPendingClose().tail(); (n = n.getNext()) != end;)
			{
				final MMOConnection<T> con = n.getValue();
				
				synchronized (con)
				{
					if (con.getSendQueue2().isEmpty() && !con.hasPendingWriteBuffer() || con.closeTimeouted())
					{
						FastList.Node<MMOConnection<T>> temp = n.getPrevious();
						getPendingClose().delete(n);
						n = temp;
						closeConnectionImpl(con, false);
					}
				}
			}
		}
	}

	private void finishConnection(SelectionKey key)
	{
		try
		{
			((SocketChannel) key.channel()).finishConnect();
		}
		catch (IOException e)
		{
			@SuppressWarnings("unchecked")
			T con = (T)key.attachment();
			closeConnectionImpl(con, true);
		}

		// key might have been invalidated on finishConnect()
		if (key.isValid())
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		}
	}

	private void acceptConnection(SelectionKey key)
	{
		SocketChannel sc;
		try
		{
			while ((sc = ((ServerSocketChannel)key.channel()).accept()) != null)
			{
				if (getAcceptFilter() == null || getAcceptFilter().accept(sc))
				{
					sc.configureBlocking(false);
					SelectionKey clientKey = sc.register(getSelector(), SelectionKey.OP_READ);
					
					clientKey.attach(getClientFactory().create(this, new TCPSocket(sc.socket()), clientKey));
				}
				else
					sc.socket().close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void readPacket(SelectionKey key)
	{
		if (key.channel() instanceof SocketChannel)
			readTCPPacket(key);
		else
			readUDPPacket(key);
	}

	private void readTCPPacket(SelectionKey key)
	{
		@SuppressWarnings("unchecked")
		T con = (T)key.attachment();

		ByteBuffer buf;
		if ((buf = con.getReadBuffer()) == null)
			buf = READ_BUFFER;

		int result = -2;

		// if we try to to do a read with no space in the buffer it will read 0 bytes
		// going into infinite loop
		if (buf.position() == buf.limit())
		{
			// should never happen
			System.err.println("POS ANTES SC.READ(): " + buf.position() + " limit: " + buf.limit());
			System.err.println("NOOBISH ERROR " + (buf == READ_BUFFER ? "READ_BUFFER" : "temp"));
			System.exit(0);
		}
		
		try
		{
			result = con.getReadableByteChannel().read(buf);
		}
		catch (IOException e)
		{
			//error handling goes bellow
		}

		if (result > 0)
		{
			// TODO this should be done vefore even reading
			if (!con.isClosed())
			{
				buf.flip();
				// try to read as many packets as possible
				while (tryReadPacket2(key, con, buf))
				{
					// ...
				}
			}
			else
			{
				if (buf == READ_BUFFER)
					READ_BUFFER.clear();
			}
		}
		else if (result == 0)
		{
			// read interest but nothing to read? wtf?
			System.out.println("NOOBISH ERROR 2 THE MISSION");
			//System.exit(0);
		}
		else if (result == -1)
			closeConnectionImpl(con, false);
		else
		{
			closeConnectionImpl(con, true);
		}
	}

	private void readUDPPacket(SelectionKey key)
	{
		int result = -2;
		ByteBuffer buf = READ_BUFFER;

		DatagramChannel dc = (DatagramChannel)key.channel();
		if (!dc.isConnected())
		{
			try
			{
				dc.configureBlocking(false);
				SocketAddress address = dc.receive(buf);
				buf.flip();
				_udpHeaderHandler.onUDPConnection(this, dc, address, buf);
			}
			catch (IOException e)
			{
			}
			buf.clear();
		}
		else
		{
			try
			{
				result = dc.read(buf);
			}
			catch (IOException e)
			{
				//error handling goes bellow
			}

			if (result > 0)
			{
				buf.flip();
				// try to read as many packets as possible
				while (tryReadUDPPacket(key, buf))
				{
					// ...
				}
			}
			else if (result == 0)
			{
				// read interest but nothing to read? wtf?
				System.out.println("CRITICAL ERROR ON SELECTOR");
				System.exit(0);
			}
			else
			{
			}
		}
	}

	private boolean tryReadPacket2(SelectionKey key, T con, ByteBuffer buf)
	{		
		if (buf.hasRemaining())
		{
			TCPHeaderHandler<T> handler = _tcpHeaderHandler;
			// parse all jeaders
			HeaderInfo<T> ret;
			while (!handler.isChildHeaderHandler())
			{
				handler.handleHeader(key, buf);
				handler = handler.getSubHeaderHandler();
			}
			// last header
			ret = handler.handleHeader(key, buf);
			
			if (ret != null)
			{
				int result = buf.remaining();
				
				// then check if header was processed
				if (ret.headerFinished())
				{
					// get expected packet size
					int size = ret.getDataPending();

					// do we got enough bytes for the packet?
					if (size <= result)
					{
						// avoid parsing dummy packets (packets without body)
						if (size > 0)
						{
							int pos = buf.position();
							parseClientPacket(getPacketHandler(), buf, size, con);
							buf.position(pos + size);
						}
						
						// if we are done with this buffer
						if (!buf.hasRemaining())
						{
							if (buf != READ_BUFFER)
							{
								con.setReadBuffer(null);
								recycleBuffer(buf);
							}
							else
								READ_BUFFER.clear();
							
							return false;
						}
						else
						{
							// nothing
						}

						return true;
					}
					else
					{
						// we dont have enough bytes for the dataPacket so we need to read
						con.enableReadInterest();

						if (buf == READ_BUFFER)
						{
							buf.position(buf.position() - HEADER_SIZE);
							allocateReadBuffer(con);
						}
						else
						{
							buf.position(buf.position() - HEADER_SIZE);
							buf.compact();
						}
						return false;
					}
				}
				else
				{
					// we dont have enough data for header so we need to read
					con.enableReadInterest();
					
					if (buf == READ_BUFFER)
						allocateReadBuffer(con);
					else
						buf.compact();
					return false;
				}
			}
			else
			{
				// null ret means critical error
				// kill the connection
				closeConnectionImpl(con, true);
				return false;
			}
		}
		else
			return false; //empty buffer
	}

	private boolean tryReadUDPPacket(SelectionKey key, ByteBuffer buf)
	{
		if (buf.hasRemaining())
		{
			UDPHeaderHandler<T> handler = _udpHeaderHandler;
			// parse all jeaders
			HeaderInfo<T> ret;
			while (!handler.isChildHeaderHandler())
			{
				handler.handleHeader(buf);
				handler = handler.getSubHeaderHandler();
			}
			// last header
			ret = handler.handleHeader(buf);

			if (ret != null)
			{
				int result = buf.remaining();

				// then check if header was processed
				if (ret.headerFinished())
				{
					T con = ret.getClient();

					// get expected packet size
					int size = ret.getDataPending();

					// do we got enough bytes for the packet?
					if (size <= result)
					{
						if (ret.isMultiPacket())
						{
							while (buf.hasRemaining())
							{
								parseClientPacket(_udpPacketHandler, buf, buf.remaining(), con);
							}
						}
						else
						{
							// avoid parsing dummy packets (packets without body)
							if (size > 0)
							{
								int pos = buf.position();
								parseClientPacket(_udpPacketHandler, buf, size, con);
								buf.position(pos + size);
							}
						}

						// if we are done with this buffer
						if (!buf.hasRemaining())
						{
							if (buf != READ_BUFFER)
							{
								con.setReadBuffer(null);
								recycleBuffer(buf);
							}
							else
								READ_BUFFER.clear();

							return false;
						}
						else
						{
							// nothing
						}

						return true;
					}
					else
					{
						// we dont have enough bytes for the dataPacket so we need to read
						con.enableReadInterest();

						if (buf == READ_BUFFER)
						{
							buf.position(buf.position() - HEADER_SIZE);
							allocateReadBuffer(con);
						}
						else
						{
							buf.position(buf.position() - HEADER_SIZE);
							buf.compact();
						}
						return false;
					}
				}
				else
				{
					buf.clear(); // READ_BUFFER
					return false;
				}
			}
			else
			{
				buf.clear(); // READ_BUFFER
				return false;
			}
		}
		else
		{
			buf.clear();
			return false; //empty buffer
		}
	}

	private void allocateReadBuffer(T con)
	{
		con.setReadBuffer(getPooledBuffer().put(READ_BUFFER));
		READ_BUFFER.clear();
	}

	private void parseClientPacket(IPacketHandler<T> handler, ByteBuffer buf, int dataSize, T client)
	{
		int pos = buf.position();

		boolean ret = client.decrypt(buf, dataSize);

		if (buf.hasRemaining() && ret)
		{
			//  apply limit
			int limit = buf.limit();
			buf.limit(pos + dataSize);
			//System.out.println("pCP2 -> BUF: POS: "+buf.position()+" - LIMIT: "+buf.limit()+" == Packet: SIZE: "+size);
			ReceivablePacket<T> cp = handler.handlePacket(buf, client);

			if (cp != null)
			{
				cp.setByteBuffer(buf);
				cp.setClient(client);

				if (cp.read())
					getExecutor().execute(cp);
			}
			buf.limit(limit);
		}
	}

	private boolean writePacket2(SelectionKey key)
	{
		@SuppressWarnings("unchecked")
		T con = (T)key.attachment();

		prepareWriteBuffer2(con);
		DIRECT_WRITE_BUFFER.flip();

		int size = DIRECT_WRITE_BUFFER.remaining();

		int result = -1;

		try
		{
			result = con.getWritableChannel().write(DIRECT_WRITE_BUFFER);
		}
		catch (IOException e)
		{
			// error handling goes on the if bellow
			//System.err.println("IOError: " + e.getMessage());
		}

		// check if no error happened
		if (result >= 0)
		{
			// check if we writed everything
			if (result == size)
			{
				synchronized (con)
				{
					if (con.getSendQueue2().isEmpty() && !con.hasPendingWriteBuffer())
					{
						con.disableWriteInterest();
						return false;
					}
					else
						return true;
				}
			}
			else
			{
				con.createWriteBuffer(DIRECT_WRITE_BUFFER);
				return false;
			}
			//if (result == 0)
			//{
			//}
			//return hasPendingWrite;
		}
		else
		{
			closeConnectionImpl(con, true);
			return false;
		}
	}

	private void prepareWriteBuffer2(T con)
	{
		DIRECT_WRITE_BUFFER.clear();

		// if theres pending content add it
		if (con.hasPendingWriteBuffer())
			con.movePendingWriteBufferTo(DIRECT_WRITE_BUFFER);

		if (DIRECT_WRITE_BUFFER.remaining() > 1 && !con.hasPendingWriteBuffer())
		{
			synchronized (con)
			{
				final FastList<SendablePacket<T>> sendQueue = con.getSendQueue2();
				
				for (int i = 0; !sendQueue.isEmpty() && i < MAX_SEND_PER_PASS; i++)
				{
					// put into WriteBuffer
					if(putPacketIntoWriteBuffer(con, sendQueue.removeFirst())>0)
					{
						WRITE_BUFFER.flip();
						if (DIRECT_WRITE_BUFFER.remaining() >= WRITE_BUFFER.limit())
							DIRECT_WRITE_BUFFER.put(WRITE_BUFFER);
						else
						{
							// there is no more space in the direct buffer
							con.createWriteBuffer(WRITE_BUFFER);
							break;
						}
					}
				}
			}
		}
	}

	private final int putPacketIntoWriteBuffer(T client, SendablePacket<T> sp)
	{
		WRITE_BUFFER.clear();
		
		// set the write buffer
		sp.setByteBuffer(WRITE_BUFFER);

		// reserve space for the size
		int headerPos = sp.getByteBuffer().position();
		int headerSize = sp.getHeaderSize();
		sp.getByteBuffer().position(headerPos + headerSize);

		// write content to buffer
		sp.write(client);

		// size (incl header)
		int dataSize = sp.getByteBuffer().position() - headerPos - headerSize;
		sp.getByteBuffer().position(headerPos + headerSize);
		client.encrypt(sp.getByteBuffer(), dataSize);

		// recalculate size after encryption
		dataSize = sp.getByteBuffer().position() - headerPos - headerSize;
		// prepend header
		//prependHeader(headerPos, size);
		sp.getByteBuffer().position(headerPos);
		sp.writeHeader(dataSize);
		sp.getByteBuffer().position(headerPos + headerSize + dataSize);
		return dataSize; 
	}

	private Selector getSelector()
	{
		return _selector;
	}

	private IMMOExecutor<T> getExecutor()
	{
		return _executor;
	}

	private IPacketHandler<T> getPacketHandler()
	{
		return _packetHandler;
	}

	private IClientFactory<T> getClientFactory()
	{
		return _clientFactory;
	}

	private IAcceptFilter getAcceptFilter()
	{
		return _acceptFilter;
	}

	void closeConnection(MMOConnection<T> con)
	{
		synchronized (getPendingClose())
		{
			getPendingClose().addLast(con);
		}
	}

	private void closeConnectionImpl(MMOConnection<T> con, boolean forced)
	{
		try
		{
			if (forced)
				con.onForcedDisconnection();
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			// notify connection
			con.onDisconnection();
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				// close socket and the SocketChannel
				con.getSocket().close();
			}
			catch (IOException e)
			{
				// ignore, we are closing anyway
			}
			finally
			{
				con.releaseBuffers();
				// clear attachment
				con.getSelectionKey().attach(null);
				// cancel key
				con.getSelectionKey().cancel();
			}
		}
	}

	private FastList<MMOConnection<T>> getPendingClose()
	{
		return _pendingClose;
	}

	public void shutdown() throws InterruptedException
	{
		_shutdown = true;
		
		join();
	}

	private boolean isShuttingDown()
	{
		return _shutdown;
	}

	private void close()
	{
		for (SelectionKey key : getSelector().keys())
		{
			try
			{
				key.channel().close();
			}
			catch (IOException e)
			{
				// ignore
			}
		}

		try
		{
			getSelector().close();
		}
		catch (IOException e)
		{
			// Ignore
		}
	}
}