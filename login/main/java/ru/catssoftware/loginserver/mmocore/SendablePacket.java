package ru.catssoftware.loginserver.mmocore;

public abstract class SendablePacket<T extends MMOConnection<T>> extends AbstractPacket
{
	protected SendablePacket()
	{
	}

	protected void putShort(int value)
	{
		getByteBuffer().putShort((short)value);
	}

	protected void putInt(int value)
	{
		getByteBuffer().putInt(value);
	}

	protected void putDouble(double value)
	{
		getByteBuffer().putDouble(value);
	}

	protected void putFloat(float value)
	{
		getByteBuffer().putFloat(value);
	}

	protected void writeC(int data)
	{
		getByteBuffer().put((byte)data);
	}

	protected void writeF(double value)
	{
		getByteBuffer().putDouble(value);
	}

	protected void writeH(int value)
	{
		getByteBuffer().putShort((short)value);
	}

	protected void writeD(int value)
	{
		getByteBuffer().putInt(value);
	}

	protected void writeQ(long value)
	{
		getByteBuffer().putLong(value);
	}

	protected void writeB(byte[] data)
	{
		getByteBuffer().put(data);
	}

	protected void writeS(CharSequence charSequence)
	{
		if (charSequence == null)
			charSequence = "";

		int length = charSequence.length();
		for (int i = 0; i < length; i++)
			getByteBuffer().putChar(charSequence.charAt(i));

		getByteBuffer().putChar('\000');
	}

	protected abstract void write(T client);

	protected abstract int getHeaderSize();

	protected abstract void writeHeader(int dataSize);
}