package ru.catssoftware.loginserver.mmocore;

import javolution.text.TextBuilder;

public abstract class ReceivablePacket<T extends MMOConnection<T>> extends AbstractPacket implements Runnable
{
	protected ReceivablePacket()
	{
	}

	private T _client;

	void setClient(T client)
	{
		_client = client;
	}

	public T getClient()
	{
		return _client;
	}

	protected int getAvaliableBytes()
	{
		return getByteBuffer().remaining();
	}

	protected abstract boolean read();

	public abstract void run();

	protected void readB(byte[] dst)
	{
		getByteBuffer().get(dst);
	}

	protected void readB(byte[] dst, int offset, int len)
	{
		getByteBuffer().get(dst, offset, len);
	}

	protected int readC()
	{
		return getByteBuffer().get() & 0xFF;
	}

	protected int readH()
	{
		return getByteBuffer().getShort() & 0xFFFF;
	}

	protected int readD()
	{
		return getByteBuffer().getInt();
	}

	protected long readQ()
	{
		return getByteBuffer().getLong();
	}

	protected double readF()
	{
		return getByteBuffer().getDouble();
	}

	protected String readS()
	{
		TextBuilder tb = TextBuilder.newInstance();

		for (char c; (c = getByteBuffer().getChar()) != 0;)
			tb.append(c);

		String str = tb.toString();
		TextBuilder.recycle(tb);
		return str;
	}
}