package ru.catssoftware.gameserver;

import java.nio.ByteBuffer;

import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.clientpackets.L2GameClientPacket;


public interface CustomPacketHandlerInterface
{
	public L2GameClientPacket handlePacket(ByteBuffer data, L2GameClient client);
}