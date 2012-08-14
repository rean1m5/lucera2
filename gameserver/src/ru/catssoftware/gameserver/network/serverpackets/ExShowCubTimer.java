package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public final class ExShowCubTimer extends L2GameServerPacket
{
	private int _x;
	public ExShowCubTimer(int x)
	{
		_x = x;
	}
	
	@Override
	public void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xFE);
		if(client.getProtocolVer()>=83)
			writeH(0x8A);
		else
			writeH(0x89);
		writeD(_x);
	}
}