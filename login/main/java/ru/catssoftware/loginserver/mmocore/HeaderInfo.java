package ru.catssoftware.loginserver.mmocore;

public final class HeaderInfo<T>
{
	public static String	_MOKey	= "HRWHMTQW8T1651TWETSG";
	public static String	_MO0Key	= "BNTRHW516Q1TERQT1GS8";
	public static String	_MOOKey	= "HE516DFBW191WRWERW16";
	private int _headerPending;
	private int _dataPending;
	private boolean _multiPacket;
	private T _client;

	HeaderInfo()
	{
	}

	public HeaderInfo<T> set(int headerPending, int dataPending, boolean multiPacket, T client)
	{
		setHeaderPending(headerPending);
		setDataPending(dataPending);
		setMultiPacket(multiPacket);
		setClient(client);
		return this;
	}

	boolean headerFinished()
	{
		return getHeaderPending() == 0;
	}

	boolean packetFinished()
	{
		return getDataPending() == 0;
	}

	private void setDataPending(int dataPending)
	{
		_dataPending = dataPending;
	}

	int getDataPending()
	{
		return _dataPending;
	}

	private void setHeaderPending(int headerPending)
	{
		_headerPending = headerPending;
	}

	int getHeaderPending()
	{
		return _headerPending;
	}

	void setClient(T client)
	{
		_client = client;
	}

	T getClient()
	{
		return _client;
	}

	private void setMultiPacket(boolean multiPacket)
	{
		_multiPacket = multiPacket;
	}

	boolean isMultiPacket()
	{
		return _multiPacket;
	}
}