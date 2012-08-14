package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestPrivateStoreManageSell extends L2GameClientPacket
{
	private static final String	_C__73_REQUESTPRIVATESTOREMANAGESELL	= "[C] 73 RequestPrivateStoreManageSell";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player != null)
		{
			player.tryOpenPrivateSellStore(false);
		}
	}

	@Override
	public String getType()
	{
		return _C__73_REQUESTPRIVATESTOREMANAGESELL;
	}
}
