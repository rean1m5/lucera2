package ru.catssoftware.loginserver.mmocore;

import java.nio.ByteOrder;

public final class SelectorConfig<T extends MMOConnection<T>>
{
	// UDP
	private final UDPHeaderHandler<T> UDP_HEADER_HANDLER;
	private final IPacketHandler<T> UDP_PACKET_HANDLER;

	// TCP
	private final TCPHeaderHandler<T> TCP_HEADER_HANDLER;
	private final IPacketHandler<T> TCP_PACKET_HANDLER;

	private final int READ_BUFFER_SIZE = 64 * 1024;
	private final int WRITE_BUFFER_SIZE = 64 * 1024;
	private int MAX_SEND_PER_PASS = 1;
	private int SLEEP_TIME = 10;
	private final HeaderSize HEADER_TYPE = HeaderSize.SHORT_HEADER;
	private final int HELPER_BUFFER_SIZE = 64 * 1024;
	private final int HELPER_BUFFER_COUNT = 20;
	private final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

	/**
	 * BYTE_HEADER: unsigned byte, max size: 255 <BR>
	 * SHORT_HEADER: unsigned short, max size: 65535<BR>
	 * INT_HEADER: signed integer, max size: Integer.MAX_SIZE<BR>
	 * @author KenM
	 */
	static enum HeaderSize
	{
		BYTE_HEADER, SHORT_HEADER, INT_HEADER,
	}

	public SelectorConfig(UDPHeaderHandler<T> udpHeaderHandler, IPacketHandler<T> udpPacketHandler, TCPHeaderHandler<T> tcpHeaderHandler, IPacketHandler<T> tcpPacketHandler)
	{
		UDP_HEADER_HANDLER = udpHeaderHandler;
		UDP_PACKET_HANDLER = udpPacketHandler;

		TCP_HEADER_HANDLER = tcpHeaderHandler;
		TCP_PACKET_HANDLER = tcpPacketHandler;
	}

	int getReadBufferSize()
	{
		return READ_BUFFER_SIZE;
	}

	int getWriteBufferSize()
	{
		return WRITE_BUFFER_SIZE;
	}

	int getHelperBufferSize()
	{
		return HELPER_BUFFER_SIZE;
	}

	int getHelperBufferCount()
	{
		return HELPER_BUFFER_COUNT;
	}

	ByteOrder getByteOrder()
	{
		return BYTE_ORDER;
	}

	HeaderSize getHeaderType()
	{
		return HEADER_TYPE;
	}

	UDPHeaderHandler<T> getUDPHeaderHandler()
	{
		return UDP_HEADER_HANDLER;
	}

	IPacketHandler<T> getUDPPacketHandler()
	{
		return UDP_PACKET_HANDLER;
	}

	TCPHeaderHandler<T> getTCPHeaderHandler()
	{
		return TCP_HEADER_HANDLER;
	}

	IPacketHandler<T> getTCPPacketHandler()
	{
		return TCP_PACKET_HANDLER;
	}

	/**
	 * Server will try to send maxSendPerPass packets per socket write call however it may send less if the write buffer was filled before achieving this value.
	 * 
	 * @param The maximum number of packets to be sent on a single socket write call
	 */
	public void setMaxSendPerPass(int maxSendPerPass)
	{
		MAX_SEND_PER_PASS = maxSendPerPass;
	}

	/**
	 * @return The maximum number of packets sent in an socket write call
	 */
	int getMaxSendPerPass()
	{
		return MAX_SEND_PER_PASS;
	}

	/**
	 * Defines how much time (in milis) should the selector sleep, an higher value increases throughput but also increases latency(to a max of the sleep value itself).<BR>
	 * Also an extremely high value(usually > 100) will decrease throughput due to the server not doing enough sends per second (depends on max sends per pass).<BR>
	 * <BR>
	 * Recommended values:<BR>
	 * 1 for minimal latency.<BR>
	 * 10-30 for an latency/troughput trade-off based on your needs.<BR>
	 * @param sleepTime the sleepTime to set
	 */
	public void setSelectorSleepTime(int sleepTime)
	{
		SLEEP_TIME = sleepTime;
	}

	/**
	 * @return the sleepTime setting for the selector
	 */
	int getSelectorSleepTime()
	{
		return SLEEP_TIME;
	}
}