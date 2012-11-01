package ru.catssoftware.gameserver.mmocore;

import javolution.text.TextBuilder;

import java.nio.ByteBuffer;
import java.util.Arrays;

@SuppressWarnings("unchecked")
public abstract class ReceivablePacket<T extends MMOClient> extends AbstractPacket<T> implements Runnable
{
	protected ByteBuffer _buf;
	protected String _debugData = "";

	protected ReceivablePacket()
	{}

	protected void setByteBuffer(ByteBuffer buf)
	{
		_buf = buf;
	}

	@Override
	protected ByteBuffer getByteBuffer()
	{
		return _buf;
	}

	protected int getAvaliableBytes()
	{
		return getByteBuffer().remaining();
	}

	protected abstract boolean read();

	@Override
	public abstract void run();

	protected void readB(byte[] dst)
	{
		getByteBuffer().get(dst);
		_debugData+="|" + Arrays.toString(dst);
	}

	protected void readB(byte[] dst, int offset, int len)
	{
		getByteBuffer().get(dst, offset, len);
		_debugData+="|" + Arrays.toString(dst);
	}

	protected int readC()
	{
		int data = getByteBuffer().get() & 0xFF;
		_debugData+="|" + data;
		return data;
	}

	protected int readH()
	{
		int data = getByteBuffer().getShort() & 0xFFFF;
		_debugData+="|" + data;
		return data;
	}

	protected int readD()
	{
		int data = getByteBuffer().getInt();
		_debugData+="|" + data;
		return data;
	}

	protected long readQ()
	{
		long data = getByteBuffer().getLong();
		_debugData+="|" + data;
		return data;
	}

	protected double readF()
	{
		double data = getByteBuffer().getDouble();
		_debugData+="|" + data;
		return data;
	}

	protected String readS()
	{
		TextBuilder tb = TextBuilder.newInstance();
		char ch;
		ByteBuffer buf = getByteBuffer();

		while((ch = buf.getChar()) != 0)
			tb.append(ch);
		String str = tb.toString();
		TextBuilder.recycle(tb);
		_debugData+="|" + str;
		return str;
	}

	protected String readS(int Maxlen)
	{
		String ret = readS();
		_debugData+="|" + (ret.length() > Maxlen ? ret.substring(0, Maxlen) : ret);
		return ret.length() > Maxlen ? ret.substring(0, Maxlen) : ret;
	}

	public String getDebugData()
	{
		return _debugData;
	}
}