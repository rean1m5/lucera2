package ru.catssoftware.gameserver.mmocore;

import ru.catssoftware.gameserver.network.L2GameClient;

import java.nio.ByteBuffer;
import java.util.Arrays;


@SuppressWarnings("unchecked")
public abstract class SendablePacket<T extends MMOClient> extends AbstractPacket<T>
{
	protected String _debugData = "";

	@Override
	protected ByteBuffer getByteBuffer()
	{
		return getCurrentSelectorThread().getWriteBuffer();
	}

	@Override
	public T getClient()
	{
		SelectorThread<T> selector = getCurrentSelectorThread();
		return selector == null ? null : selector.getWriteClient();
	}

	protected void writeC(boolean data) {
		_debugData+="|" + data;
		writeC(data?1:0);
	}
	protected void writeC(int data)
	{
		_debugData+="|" + data;
		getByteBuffer().put((byte) data);
	}

	protected void writeF(double value)
	{
		_debugData+="|" + value;
		getByteBuffer().putDouble(value);
	}

	protected void writeH(int value)
	{
		_debugData+="|" + value;
		getByteBuffer().putShort((short) value);
	}

	protected void writeD(int value)
	{
		_debugData+="|" + value;
		getByteBuffer().putInt(value);
	}

	/**
	 * Отсылает число позиций + массив
	 */
	protected void writeDD(int[] values, boolean sendCount)
	{
		ByteBuffer buf = getByteBuffer();
		if(sendCount)
		{
			_debugData+="|" + values.length;
			buf.putInt(values.length);
		}
		for(int value : values)
		{
			_debugData+="|" + value;
			buf.putInt(value);
		}
	}

	protected void writeDD(int[] values)
	{
		writeDD(values, false);
	}

	protected void writeQ(long value)
	{
		_debugData+="|" + value;
		getByteBuffer().putLong(value);
	}

	protected void writeB(byte[] data)
	{
		_debugData+="|" + (data != null ? Arrays.toString(data) : "null");
		getByteBuffer().put(data);
	}

	protected void writeS(CharSequence charSequence)
	{
		ByteBuffer buf = getByteBuffer();
		if(charSequence == null)
			charSequence = "";
		_debugData+="|" + charSequence;
		int length = charSequence.length();
		for(int i = 0; i < length; i++)
			buf.putChar(charSequence.charAt(i));
		buf.putChar('\000');
	}

	protected abstract void write(L2GameClient client);

	protected abstract int getHeaderSize();

	protected abstract void writeHeader(int dataSize);
}