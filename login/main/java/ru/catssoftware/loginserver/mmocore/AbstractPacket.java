package ru.catssoftware.loginserver.mmocore;

import java.nio.ByteBuffer;

abstract class AbstractPacket
{
	private ByteBuffer _buf;

	void setByteBuffer(ByteBuffer buf)
	{
		_buf = buf;
	}

	protected ByteBuffer getByteBuffer()
	{
		return _buf;
	}
}