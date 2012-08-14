package ru.catssoftware.gameserver.network.clientpackets;

public class RequestSiegeInfo extends L2GameClientPacket
{
	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl(){}

	@Override
	public String getType()
	{
		return "[C] 0x47 RequestSiegeInfo";
	}

}
