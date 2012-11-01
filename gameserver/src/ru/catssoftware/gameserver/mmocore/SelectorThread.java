package ru.catssoftware.gameserver.mmocore;

import javolution.util.FastList;
import javolution.util.FastList.Node;
import javolution.util.FastMap;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.L2GameClient.GameClientState;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unchecked")
public class SelectorThread<T extends MMOClient> extends Thread
{
	class PacketRunner extends Thread
	{
		protected final FastList<T> _clients = new FastList<T>();

		public void ShedulePacket(ReceivablePacket<T> cp)
		{
			T _client = cp.getClient();
			synchronized (_clients)
			{
				_client.client_packets.add(cp);
				if(!_clients.contains(_client))
					_clients.add(_client);
			}
		}

		@Override
		public void run()
		{
			for(;;)
			{
				if(_shutdown)
					return;
				synchronized (_clients)
				{
					Iterator<T> it = _clients.iterator();
					while(it.hasNext())
					{
						T client_next = it.next();
						if(!client_next.isConnected()) {
							it.remove();
						}	
							
						else if(client_next.can_runImpl)
						{
							client_next.can_runImpl = false;
							getExecutor().execute((ReceivablePacket<T>) client_next.client_packets.removeFirst());
							if(client_next.client_packets.isEmpty())
								it.remove();
						}
					}
				}

				try
				{
					Thread.sleep(2);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	class TimeoutChecker implements Runnable
	{
		@Override
		public void run()
		{
			long time = System.currentTimeMillis() - 20000;
			for(Entry<L2GameClient, Long> e : _unauthedClients.entrySet())
				if(e.getValue() < time)
				{
					if(e.getKey().getState() == GameClientState.CONNECTED)
						e.getKey().closeNow(false);
					_unauthedClients.remove(e.getKey());
				}
		}
	}

	//private Future<?> timeoutChecker;

	private final Selector _selector = Selector.open();

	// Implementations
	private final IPacketHandler<T> _packetHandler;
	private final IMMOExecutor<T> _executor;
	private final IClientFactory<T> _clientFactory;
	private IAcceptFilter _acceptFilter;
	private final TCPHeaderHandler<T> _tcpHeaderHandler;

	private boolean _shutdown;

	// Pending Close
	private final FastList<MMOConnection<T>> _pendingClose = FastList.newInstance();

	// Configs
	private final int HELPER_BUFFER_SIZE;
	private final int HELPER_BUFFER_COUNT;
	private final int MAX_SEND_PER_PASS;
	private final int HEADER_SIZE = 2;
	private final ByteOrder BYTE_ORDER;
	private final long SLEEP_TIME;

	// MAIN BUFFERS
	private ByteBuffer DIRECT_WRITE_BUFFER;
	private final ByteBuffer WRITE_BUFFER, READ_BUFFER;

	private T WRITE_CLIENT;

	// ByteBuffers General Purpose Pool
	private final ConcurrentLinkedQueue<ByteBuffer> _bufferPool = new ConcurrentLinkedQueue<ByteBuffer>();
	private PacketRunner _pktrunner;

	private static int MAX_UNHANDLED_SOCKETS_PER_IP = 5;
	private static int UNHANDLED_SOCKET_TTL = 5000;
	private static boolean enableAntiflood = false;
	private static Object antifloodLock = new Object();
	private static final Map<String, Integer> _unhandledIPSockets = new FastMap<String, Integer>().setShared(true);
	private static final Map<Socket, Long> _unhandledChannels = new FastMap<Socket, Long>().setShared(true);
	private static final Map<L2GameClient, Long> _unauthedClients = new ConcurrentHashMap<L2GameClient, Long>();

	private static ReentrantLock global_read_lock = null;
	private static final FastList<SelectorThread> all_selectors = new FastList<SelectorThread>();

	public SelectorThread(SelectorConfig<T> sc, IPacketHandler<T> packetHandler, IMMOExecutor<T> executor, IClientFactory<T> clientFactory, IAcceptFilter acceptFilter) throws IOException
	{
		all_selectors.add(this);
		HELPER_BUFFER_SIZE = sc.getHelperBufferSize();
		HELPER_BUFFER_COUNT = sc.getHelperBufferCount();
		MAX_SEND_PER_PASS = sc.getMaxSendPerPass();
		BYTE_ORDER = sc.getByteOrder();
		SLEEP_TIME = sc.getSelectorSleepTime();

		DIRECT_WRITE_BUFFER = ByteBuffer.wrap(new byte[sc.getWriteBufferSize()]).order(BYTE_ORDER);
		WRITE_BUFFER = ByteBuffer.wrap(new byte[sc.getWriteBufferSize()]).order(BYTE_ORDER);
		READ_BUFFER = ByteBuffer.wrap(new byte[sc.getReadBufferSize()]).order(BYTE_ORDER);

		_tcpHeaderHandler = sc.getTCPHeaderHandler();
		initBufferPool();
		_acceptFilter = acceptFilter;
		_packetHandler = packetHandler;
		_clientFactory = clientFactory;
		_executor = executor;
		setPriority(Thread.MAX_PRIORITY - 1);
//		timeoutChecker =  ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimeoutChecker(), 10, 1000);
	}

	protected void initBufferPool()
	{
		for(int i = 0; i < HELPER_BUFFER_COUNT; i++)
			getFreeBuffers().add(ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER));
	}

	public void openServerSocket(InetAddress address, int tcpPort) throws IOException
	{
		ServerSocketChannel selectable = ServerSocketChannel.open();
		selectable.configureBlocking(false);

		address = address == null ? MMOSocket.getInstance(true) : address;
		selectable.socket().bind(address == null ? new InetSocketAddress(tcpPort) : new InetSocketAddress(address, tcpPort));
		selectable.register(getSelector(), SelectionKey.OP_ACCEPT);
		setName("SelectorThread:" + tcpPort);
	}

	protected ByteBuffer getPooledBuffer()
	{
		if(getFreeBuffers().isEmpty())
			return ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER);
		return getFreeBuffers().poll();
	}

	public void recycleBuffer(ByteBuffer buf)
	{
		if(getFreeBuffers().size() < HELPER_BUFFER_COUNT)
		{
			buf.clear();
			getFreeBuffers().add(buf);
		}
	}

	public void freeBuffer(ByteBuffer buf, MMOConnection<T> con)
	{
		if(buf == READ_BUFFER)
			READ_BUFFER.clear();
		else
		{
			con.setReadBuffer(null);
			recycleBuffer(buf);
		}
	}

	public static void setGlobalReadLock(boolean enable)
	{
		global_read_lock = enable ? new ReentrantLock() : null;
	}

	public ConcurrentLinkedQueue<ByteBuffer> getFreeBuffers()
	{
		return _bufferPool;
	}

	public SelectionKey registerClientSocket(SelectableChannel sc, int interestOps) throws ClosedChannelException
	{
		SelectionKey sk = null;

		sk = sc.register(getSelector(), interestOps);
		return sk;
	}

	@Override
	public void run()
	{
		_pktrunner = new PacketRunner();
		_pktrunner.start();
		int totalKeys = 0;
		Set<SelectionKey> keys;
		Iterator<SelectionKey> iter;
		SelectionKey key;
		MMOConnection<T> con;
		Node<MMOConnection<T>> n, end, temp;

		// main loop
		for(;;)
		{
			// check for shutdown
			if(isShuttingDown())
			{
				closeSelectorThread();
				break;
			}

			try
			{
				totalKeys = getSelector().selectNow();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}

			if(totalKeys > 0)
			{
				keys = getSelector().selectedKeys();
				iter = keys.iterator();

				while(iter.hasNext())
				{
					key = iter.next();
					iter.remove();

					if(!key.isValid())
						continue;
					switch(key.readyOps())
					{
						case SelectionKey.OP_CONNECT:
							finishConnection(key);
							break;
						case SelectionKey.OP_ACCEPT:
							acceptConnection(key);
							break;
						case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
							writePacket(key);
							// key might have been invalidated on writePacket
							if(key.isValid())
								readPacket(key);
							break;
						case SelectionKey.OP_WRITE:
							writePacket(key);
							break;
						case SelectionKey.OP_READ:
							readPacket(key);
							break;
					}
				}
				keys.clear();
			}

			// process pending close
			synchronized (_pendingClose)
			{
				for(n = _pendingClose.head(), end = _pendingClose.tail(); (n = n.getNext()) != end;)
				{
					con = n.getValue();
					if(con != null && con.getSendQueue() != null && con.getSendQueue().isEmpty())
					{
						temp = n.getPrevious();
						_pendingClose.delete(n);
						n = temp;
						closeConnectionImpl(con);
					}
				}
			}

			try
			{
				Thread.sleep(SLEEP_TIME);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}

	}

	protected void finishConnection(SelectionKey key)
	{
		try
		{
			((SocketChannel) key.channel()).finishConnect();
		}
		catch(IOException e)
		{
			MMOConnection<T> con = (MMOConnection<T>) key.attachment();
			T client = con.getClient();
			client.getConnection().onForcedDisconnection();
			closeConnectionImpl(client.getConnection());
		}

		// key might have been invalidated on finishConnect()
		if(key.isValid())
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		}
	}

	protected void acceptConnection(SelectionKey key)
	{
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc;
		SelectionKey clientKey;
		try
		{
			while((sc = ssc.accept()) != null)
			{
				if(enableAntiflood)
					synchronized (antifloodLock)
					{
						floodcloseold();
						if(!floodaccept(sc.socket()))
						{
							sc.socket().close();
							continue;
						}
					}
				if(getAcceptFilter() == null || getAcceptFilter().accept(sc))
				{
					sc.configureBlocking(false);
					clientKey = sc.register(getSelector(), SelectionKey.OP_READ /*| SelectionKey.OP_WRITE*/);

					MMOConnection<T> con = new MMOConnection<T>(this, new TCPSocket(sc.socket()), clientKey);
					MMOClient client = getClientFactory().create(con);
					if(client instanceof L2GameClient)
						_unauthedClients.put((L2GameClient) client, System.currentTimeMillis());
					clientKey.attach(con);
				}
				else
					sc.socket().close();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	protected void readPacket(SelectionKey key)
	{
		MMOConnection<T> con = (MMOConnection<T>) key.attachment();
		T client = con.getClient();

		ByteBuffer buf;
		int result = -2;

		if(global_read_lock != null)
			global_read_lock.lock();

		try
		{
			if((buf = con.getReadBuffer()) == null)
				buf = READ_BUFFER;

			// if we try to to do a read with no space in the buffer it will read 0 bytes
			// going into infinite loop
			if(buf.position() == buf.limit())
			{
				// should never happen
				closeConnectionImpl(con);
				return;
			}

			try
			{
				result = con.getReadableByteChannel().read(buf);
			}
			catch(IOException e)
			{
				//error handling goes bellow
			}

			if(result > 0)
			{
				// TODO this should be done before even reading
				if(con.isClosed())
					freeBuffer(buf, con);
				else
				{
					buf.flip();
					// try to read as many packets as possible
					while(tryReadPacket2(key, client, buf)) //FIXME
					{
					}
				}
			}
			else if(result == 0)
			{
				// read interest but nothing to read? wtf?
				closeConnectionImpl(con);
				return;
			}
			else if(result == -1)
				closeConnectionImpl(con);
			else
			{
				con.onForcedDisconnection();
				closeConnectionImpl(con);
			}
		}
		finally
		{
			if(global_read_lock != null)
				global_read_lock.unlock();
		}
	}

	protected boolean tryReadPacket2(SelectionKey key, T client, ByteBuffer buf)
	{
		MMOConnection<T> con = client.getConnection();

		if(buf.hasRemaining())
		{
			TCPHeaderHandler<T> handler = _tcpHeaderHandler;
			// parse all headers
			HeaderInfo<T> ret;
			while(!handler.isChildHeaderHandler())
			{
				handler.handleHeader(key, buf);
				handler = handler.getSubHeaderHandler();
			}
			// last header
			ret = handler.handleHeader(key, buf);

			if(ret != null)
			{
				int result = buf.remaining();

				// then check if header was processed
				if(ret.headerFinished())
				{
					// get expected packet size
					int size = ret.getDataPending();

					// do we got enough bytes for the packet?
					if(size <= result)
					{
						boolean parseRet = true;
						// avoid parsing dummy packets (packets without body)
						if(size > 0)
						{
							int pos = buf.position();
							parseRet = parseClientPacket(getPacketHandler(), buf, size, client);
							buf.position(pos + size);
						}

						// if we are done with this buffer
						if(!buf.hasRemaining() || !parseRet)
						{
							freeBuffer(buf, con);
							return false;
						}

						return parseRet;
					}
					// we dont have enough bytes for the dataPacket so we need to read
					client.getConnection().enableReadInterest();

					if(buf == READ_BUFFER)
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
				// we dont have enough data for header so we need to read
				client.getConnection().enableReadInterest();

				if(buf == READ_BUFFER)
					allocateReadBuffer(con);
				else
					buf.compact();
				return false;
			}
			// null ret means critical error
			// kill the connection
			closeConnectionImpl(con);
			return false;
		}
		//con.disableReadInterest();
		return false; //empty buffer
	}

	protected void allocateReadBuffer(MMOConnection con)
	{
		con.setReadBuffer(getPooledBuffer().put(READ_BUFFER));
		READ_BUFFER.clear();
	}

	protected boolean parseClientPacket(IPacketHandler<T> handler, ByteBuffer buf, int dataSize, T client)
	{
		if(enableAntiflood)
			synchronized (antifloodLock)
			{
				floodclose(((TCPSocket) client.getConnection().getSocket()).getSocket());
			}
		int pos = buf.position();

		boolean ret = client.decrypt(buf, dataSize);
		buf.position(pos);
		if(!ret)
			return false;

		if(buf.hasRemaining())
		{
			//  apply limit
			int limit = buf.limit();
			buf.limit(pos + dataSize);
			ReceivablePacket<T> cp = handler.handlePacket(buf, client);
			
			if(cp != null)
			{
				cp.setByteBuffer(buf);
				cp.setClient(client);
				if(cp.read())
					_pktrunner.ShedulePacket(cp);
				client.logInfo("client --> server [" + cp.getClass().getSimpleName() + "] data: " + cp.getDebugData());
				cp.setByteBuffer(null);
			}
			buf.limit(limit);
		}
		return true;
	}

	protected void writePacket(SelectionKey key)
	{
		MMOConnection<T> con = (MMOConnection<T>) key.attachment();

		prepareWriteBuffer(con);

		DIRECT_WRITE_BUFFER.flip();
		int size = DIRECT_WRITE_BUFFER.remaining();

		int result = -1;

		try
		{
			result = con.getWritableChannel().write(DIRECT_WRITE_BUFFER);
		}
		catch(IOException e)
		{
			// error handling goes on the if bellow
		}

		// check if no error happened
		if(result >= 0)
		{
			// check if we writed everything
			if(result == size)
				synchronized (con.getSendQueue())
				{
					if(con.getSendQueue().isEmpty() && !con.hasPendingWriteBuffer())
						con.disableWriteInterest();
				}
			else
				con.createWriteBuffer(DIRECT_WRITE_BUFFER);
		}
		else
		{
			con.onForcedDisconnection();
			closeConnectionImpl(con);
		}
	}

	protected void prepareWriteBuffer(MMOConnection<T> con)
	{
		DIRECT_WRITE_BUFFER.clear();

		if(con.hasPendingWriteBuffer()) // если осталось что-то с прошлого раза
			con.movePendingWriteBufferTo(DIRECT_WRITE_BUFFER);

		if(DIRECT_WRITE_BUFFER.remaining() > 1 && !con.hasPendingWriteBuffer())
		{
			int i = 0;
			ArrayDeque<SendablePacket<T>> sendQueue = con.getSendQueue();
			SendablePacket<T> sp;

			synchronized (sendQueue)
			{
				WRITE_CLIENT = con.getClient();
				while(i++ < MAX_SEND_PER_PASS && (sp = sendQueue.poll()) != null)
					try
					{
						putPacketIntoWriteBuffer((L2GameClient)WRITE_CLIENT, sp, true); // записываем пакет в WRITE_BUFFER
						WRITE_BUFFER.flip();
						if(DIRECT_WRITE_BUFFER.remaining() >= WRITE_BUFFER.limit())
							DIRECT_WRITE_BUFFER.put(WRITE_BUFFER);
						else
						// если не осталось места в DIRECT_WRITE_BUFFER для WRITE_BUFFER то мы его запишев в следующий раз
						{
							// there is no more space in the direct buffer
							con.createWriteBuffer(WRITE_BUFFER);
							break;
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
						WRITE_BUFFER.clear();
						break;
					}
			}
		}
	}


	protected final void putPacketIntoWriteBuffer(L2GameClient client, SendablePacket<T> sp, boolean encrypt)
	{
		WRITE_BUFFER.clear();

		// reserve space for the size
		int headerPos = WRITE_BUFFER.position();
		int headerSize = sp.getHeaderSize();
		WRITE_BUFFER.position(headerPos + headerSize);

		// write content to buffer
		sp.write(client);

		// size (incl header)
		int dataSize = WRITE_BUFFER.position() - headerPos - headerSize;
		if(dataSize == 0)
		{
			WRITE_BUFFER.position(headerPos);
			return;
		}
		WRITE_BUFFER.position(headerPos + headerSize);
		if(encrypt)
		{
			client.encrypt(WRITE_BUFFER, dataSize);
			// recalculate size after encryption
			dataSize = WRITE_BUFFER.position() - headerPos - headerSize;
		}

		// prepend header
		WRITE_BUFFER.position(headerPos);
		sp.writeHeader(dataSize);
		WRITE_BUFFER.position(headerPos + headerSize + dataSize);
	}

	public Selector getSelector()
	{
		return _selector;
	}

	protected IMMOExecutor<T> getExecutor()
	{
		return _executor;
	}

	public IPacketHandler<T> getPacketHandler()
	{
		return _packetHandler;
	}

	public IClientFactory<T> getClientFactory()
	{
		return _clientFactory;
	}

	public void setAcceptFilter(IAcceptFilter acceptFilter)
	{
		_acceptFilter = acceptFilter;
	}

	public IAcceptFilter getAcceptFilter()
	{
		return _acceptFilter;
	}

	public void closeConnection(MMOConnection<T> con)
	{
		synchronized (_pendingClose)
		{
			_pendingClose.addLast(con);
		}
	}

	protected void closeConnectionImpl(MMOConnection<T> con)
	{
		try
		{
			if(enableAntiflood)
				synchronized (antifloodLock)
				{
					floodclose(((TCPSocket) con.getSocket()).getSocket());
				}
			// notify connection
			con.onDisconnection();
		}
		finally
		{
			try
			{
				// close socket and the SocketChannel
				con.getSocket().close();
			}
			catch(IOException e)
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

	public void shutdown()
	{
		_shutdown = true;
	}

	public boolean isShuttingDown()
	{
		return _shutdown;
	}

	protected void closeAllChannels()
	{
		Set<SelectionKey> keys = getSelector().keys();
		for(SelectionKey key : keys)
			try
			{
				key.channel().close();
			}
			catch(IOException e)
			{
				// ignore
			}
	}

	protected void closeSelectorThread()
	{
		closeAllChannels();
//		timeoutChecker.cancel(false);
		try
		{
			getSelector().close();
		}
		catch(IOException e)
		{
			// Ignore
		}
	}

	public ByteBuffer getWriteBuffer()
	{
		return WRITE_BUFFER;
	}

	public T getWriteClient()
	{
		return WRITE_CLIENT;
	}

	protected static boolean floodaccept(Socket sc)
	{
		String _ip = sc.getInetAddress().getHostAddress();
		Integer cnt = _unhandledIPSockets.get(_ip);
		if(cnt == null)
		{
			_unhandledIPSockets.put(_ip, 1);
			_unhandledChannels.put(sc, System.currentTimeMillis());
			return true;
		}
		if(cnt < MAX_UNHANDLED_SOCKETS_PER_IP)
		{
			cnt++;
			_unhandledIPSockets.remove(_ip);
			_unhandledIPSockets.put(_ip, cnt);
			_unhandledChannels.put(sc, System.currentTimeMillis());
			return true;
		}
		return false;
	}

	protected static void floodclose(Socket sc)
	{
		if(sc == null)
			return;
		if(!_unhandledChannels.containsKey(sc))
			return;
		_unhandledChannels.remove(sc);
		if(sc.getInetAddress() == null)
			return;
		String _ip = sc.getInetAddress().getHostAddress();
		if(_ip == null)
			return;
		Integer cnt = _unhandledIPSockets.get(_ip);
		if(cnt == null)
			return;
		cnt--;
		if(cnt < 0)
			cnt = 0;
		_unhandledIPSockets.remove(_ip);
		_unhandledIPSockets.put(_ip, cnt);
	}

	protected static void floodcloseold()
	{
		Long now_time = System.currentTimeMillis();
		for(Socket sc : _unhandledChannels.keySet())
		{
			Long sc_time_diff = now_time - _unhandledChannels.get(sc);
			if(sc_time_diff >= UNHANDLED_SOCKET_TTL)
			{
				floodclose(sc);
				try
				{
					sc.close();
				}
				catch(IOException e)
				{}
			}
		}
	}

	public static void setAntiFlood(boolean _enableAntiflood)
	{
		enableAntiflood = _enableAntiflood;
	}

	public static boolean getAntiFlood()
	{
		return enableAntiflood;
	}

	public static void setAntiFloodSocketsConf(int MaxUnhandledSocketsPerIP, int UnhandledSocketsMinTTL)
	{
		MAX_UNHANDLED_SOCKETS_PER_IP = MaxUnhandledSocketsPerIP;
		UNHANDLED_SOCKET_TTL = UnhandledSocketsMinTTL;
	}


}