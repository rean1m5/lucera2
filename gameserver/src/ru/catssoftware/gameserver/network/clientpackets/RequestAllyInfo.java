package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.AllyInfo;

public class RequestAllyInfo extends L2GameClientPacket
{
	private static final String	_C__8E_REQUESTALLYINFO	= "[C] 8E RequestAllyInfo";

	@Override
	public void readImpl(){}

	@Override
	protected void runImpl()
	{
		AllyInfo ai = new AllyInfo(getClient().getActiveChar());
		sendPacket(ai);
	}

	@Override
	public String getType()
	{
		return _C__8E_REQUESTALLYINFO;
	}
}
