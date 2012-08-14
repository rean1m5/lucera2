package ru.catssoftware.gameserver.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.mmocore.IAcceptFilter;
import ru.catssoftware.gameserver.network.clientpackets.L2GameClientPacket;
import ru.catssoftware.info.Version;
import ru.catssoftware.lang.L2System;

public final class IOFloodManager implements IAcceptFilter
{
	private static final Logger _log = Logger.getLogger(IOFloodManager.class);


	public static List<String> _ips = new FastList<String>();
	public static enum ErrorMode
	{
		BUFFER_UNDER_FLOW,
		FAILED_READING,
		FAILED_RUNNING;
	}

	private static IOFloodManager _instance;

	public static IOFloodManager getInstance()
	{
		if (_instance == null)
			_instance = new IOFloodManager();
		return _instance;
	}

	private final FloodManager _packets;

	private final FloodManager _errors;
	
	private IOFloodManager()
	{
		_packets = new FloodManager(1000, 10);
		_packets.addFloodFilter(100, 1);
		_errors = new FloodManager(100, 10);
		_errors.addFloodFilter(10, 1);
		try {
			InetAddress addr = InetAddress.getByName("l2top.ru");
			if(addr!=null) 
				_ips.add(addr.getHostAddress());
		} catch(UnknownHostException e) {
			
		}
			
		
		_log.info("IOFloodManager: initialized.");
	}

	public synchronized void report(ErrorMode mode, L2GameClient client, L2GameClientPacket packet, Throwable throwable)
	{
		final boolean isFlooding = _errors.isFlooding(client.getAccountName(), true);
		final StringBuilder sb = new StringBuilder();

		/**
		 * Формируем текс сообщения об ошибке
		 * Отправляем в лог сообщение
		 */
		if (isFlooding)
			sb.append("Flooding with ");
		
		sb.append(mode);
		sb.append(": ");
		sb.append(client);
		if (packet != null)
		{
			sb.append(" - ");
			sb.append(packet.getType());
		}
		sb.append(" - ");
		sb.append(Version.Version);

		if (throwable != null)
			_log.fatal(sb, throwable);
		else
			_log.fatal(sb);
	}

	public synchronized  void addIp(String ip) {
		if(!_ips.contains(ip))
			_ips.add(ip);
	}
	@Override
	public synchronized boolean accept(SocketChannel socketChannel)
	{
			if(!Config.ENABLE_DDOS_PROTECTION)
				return true;
			String ip =  socketChannel.socket().getInetAddress().getHostAddress();
			if(_ips.contains(ip)) 
				return true;
			return false;
	}

	private static final class FloodManager
	{
		private final Map<String, LogEntry> _entries = new HashMap<String, LogEntry>();

		private final int _tickLength;
		private final int _tickAmount;

		private FloodFilter[] _filters = new FloodFilter[0];

		private FloodManager(int msecPerTick, int tickAmount)
		{
			_tickLength = msecPerTick;
			_tickAmount = tickAmount;
		}

		private void addFloodFilter(int entryLimit, int tickLimit)
		{
			_filters = Arrays.copyOf(_filters, _filters.length + 1);
			_filters[_filters.length - 1] = new FloodFilter(entryLimit, tickLimit);
		}

		private static final class FloodFilter
		{
			private final int _entryLimit;
			private final int _tickLimit;

			private FloodFilter(int entryLimit, int tickLimit)
			{
				_entryLimit = entryLimit;
				_tickLimit = tickLimit;
			}

			private int getEntryLimit()
			{
				return _entryLimit;
			}

			private int getTickLimit()
			{
				return _tickLimit;
			}
		}

		private boolean isFlooding(String key, boolean increment)
		{
			if (key == null || key.isEmpty())
				return false;

			LogEntry entry = _entries.get(key);

			if (entry == null)
			{
				entry = new LogEntry();

				_entries.put(key, entry);
			}

			return entry.isFlooding(increment);
		}

		private final class LogEntry
		{
			private final int[] _ticks = new int[_tickAmount];

			private int _lastTick = getCurrentTick();

			private int getCurrentTick()
			{
				return (int)(L2System.milliTime() / _tickLength);
			}

			private boolean isFlooding(boolean increment)
			{
				final int currentTick = getCurrentTick();

				if (currentTick - _lastTick >= _ticks.length)
				{
					_lastTick = currentTick;

					Arrays.fill(_ticks, 0);
				}
				else
				{
					while (currentTick != _lastTick)
					{
						_lastTick++;

						_ticks[_lastTick % _ticks.length] = 0;
					}
				}

				if (increment)
					_ticks[_lastTick % _ticks.length]++;

				if(_lastTick>0)
				for (FloodFilter filter : _filters)
				{
					int previousSum = 0;
					int currentSum = 0;

					for (int i = 0; i <= filter.getTickLimit(); i++)
					try {
						int value = _ticks[(_lastTick - i) % _ticks.length];

						if (i != 0)
							previousSum += value;

						if (i != filter.getTickLimit())
							currentSum += value;
					} catch(ArrayIndexOutOfBoundsException oob) {
						return false;
					}

					if (previousSum > filter.getEntryLimit() || currentSum > filter.getEntryLimit())
						return true;
				}

				return false;
			}
		}
	}
}