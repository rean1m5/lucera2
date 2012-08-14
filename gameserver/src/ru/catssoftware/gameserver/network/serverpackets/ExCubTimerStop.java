package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public final class ExCubTimerStop extends L2GameServerPacket
{
	public ExCubTimerStop(){}
	
	@Override
	public void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xFE);
		writeH(0x8B);
	}
}