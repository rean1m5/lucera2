package ru.catssoftware.gameserver.network.clientpackets;

public class RequestPledgeExtendedInfo extends L2GameClientPacket
{

	@SuppressWarnings("unused")
	private String	_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl(){}

	@Override
	public String getType()
	{
		return "[C] 0x67 RequestPledgeExtendedInfo";
	}

}
