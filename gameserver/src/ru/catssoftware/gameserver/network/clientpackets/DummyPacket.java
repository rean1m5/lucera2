package ru.catssoftware.gameserver.network.clientpackets;

public class DummyPacket extends L2GameClientPacket
{
	@SuppressWarnings("unused")	
	private int					_packetId;

	@Override
	protected void readImpl(){}

	@Override
	public void runImpl(){}

	@Override
	public String getType()
	{
		return "DummyPacket";
	}
}