package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestPrivateStoreQuitBuy extends L2GameClientPacket
{
	private static final String	_C__93_REQUESTPRIVATESTOREQUITBUY	= "[C] 93 RequestPrivateStoreQuitBuy";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
		player.standUp();
		player.broadcastUserInfo();
	}

	@Override
	public String getType()
	{
		return _C__93_REQUESTPRIVATESTOREQUITBUY;
	}
}
