package ru.catssoftware.loginserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.catssoftware.loginserver.manager.BanManager;
import ru.catssoftware.loginserver.mmocore.HeaderInfo;
import ru.catssoftware.loginserver.mmocore.IAcceptFilter;
import ru.catssoftware.loginserver.mmocore.IClientFactory;
import ru.catssoftware.loginserver.mmocore.IMMOExecutor;
import ru.catssoftware.loginserver.mmocore.ISocket;
import ru.catssoftware.loginserver.mmocore.ReceivablePacket;
import ru.catssoftware.loginserver.mmocore.SelectorThread;
import ru.catssoftware.loginserver.mmocore.TCPHeaderHandler;
import ru.catssoftware.loginserver.network.serverpackets.Init;



/**
 * @author  KenM
 */
public class SelectorHelper extends TCPHeaderHandler<L2LoginClient> implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter
{
	private final ThreadPoolExecutor _generalPacketsThreadPool = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

	public SelectorHelper()
	{
		super(null);
	}

	/**
	 * @see com.l2jserver.mmocore.network.IMMOExecutor#execute(com.l2jserver.mmocore.network.ReceivablePacket)
	 */
	public void execute(ReceivablePacket<L2LoginClient> packet)
	{
		_generalPacketsThreadPool.execute(packet);
	}

	/**
	 * @see com.l2jserver.mmocore.network.IClientFactory#create(com.l2jserver.mmocore.network.MMOConnection)
	 */
	public L2LoginClient create(SelectorThread<L2LoginClient> selectorThread, ISocket socket, SelectionKey key)
	{
		L2LoginClient client = new L2LoginClient(selectorThread, socket, key);
		client.sendPacket(new Init(client));
		return client;
	}

	/**
	 * @see com.l2jserver.mmocore.network.IAcceptFilter#accept(java.nio.channels.SocketChannel)
	 */
	public boolean accept(SocketChannel sc)
	{
		return !BanManager.getInstance().isBannedAddress(sc.socket().getInetAddress());
	}

	/**
	* @see ru.catssoftware.loginserver.mmocore.TCPHeaderHandler#handleHeader(java.nio.channels.SelectionKey, java.nio.ByteBuffer)
	*/
	@Override
	public HeaderInfo<L2LoginClient> handleHeader(SelectionKey key, ByteBuffer buf)
	{
		if (buf.remaining() >= 2)
		{
			int dataPending = (buf.getShort() & 0xffff) - 2;
			return getHeaderInfoReturn().set(0, dataPending, false, (L2LoginClient)key.attachment());
		}
		else
			return getHeaderInfoReturn().set(2 - buf.remaining(), 0, false, (L2LoginClient)key.attachment());
	}
}