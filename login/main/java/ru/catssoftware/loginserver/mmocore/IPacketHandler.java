package ru.catssoftware.loginserver.mmocore;

import java.nio.ByteBuffer;

public interface IPacketHandler<T extends MMOConnection<T>>
{
	public ReceivablePacket<T> handlePacket(ByteBuffer buf, T client);
}