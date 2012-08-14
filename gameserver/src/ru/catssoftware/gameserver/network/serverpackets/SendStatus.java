package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2World;

public final class SendStatus extends L2GameServerPacket
{
	public SendStatus()
	{
	}

	@Override
	public void writeImpl()
	{
		if(!Config.USE_OFF_EMULATOR) {
			writeD(0x0);
			return;
		}
			int online = 0;

			online = L2World.getInstance().getAllPlayersCount();
			writeC(0x2E);			// Packet ID
			writeD(0x01);			// World ID
			writeD(5000);			// Max Online
			writeD(online + 2);		// Current Online
			writeD(online);			// Current Online
			writeD(884);			// Priv.Store Chars

			// SEND TRASH
			writeH(0x30);
			writeH(0x2C);
			writeH(0x35);
			writeH(0x31);
			writeH(0x30);
			writeH(0x2C);
			writeH(0x37);
			writeH(0x37);
			writeH(0x37);
			writeH(0x35);
			writeH(0x38);
			writeH(0x2C);
			writeH(0x36);
			writeH(0x35);
			writeH(0x30);
			writeD(0x36);
			writeD(0x77);
			writeD(0xB7);
			writeQ(0x9F);
			writeD(0);
			writeH(0x41);
			writeH(0x75);
			writeH(0x67);
			writeH(0x20);
			writeH(0x32);
			writeH(0x39);
			writeH(0x20);
			writeH(0x32);
			writeH(0x30);
			writeH(0x30);
			writeD(0x39);
			writeH(0x30);
			writeH(0x32);
			writeH(0x3A);
			writeH(0x34);
			writeH(0x30);
			writeH(0x3A);
			writeH(0x34);
			writeD(0x33);
			writeD(0x57);
			writeC(0x11);
			writeC(0x5D);
			writeC(0x1F);
			writeC(0x60);
	}
}