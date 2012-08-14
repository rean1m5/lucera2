package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestPrivateStoreManageBuy extends L2GameClientPacket
{
	private static final String	_C__90_REQUESTPRIVATESTOREMANAGEBUY	= "[C] 90 RequestPrivateStoreManageBuy";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player != null) {
			player.tryOpenPrivateBuyStore();
		}
	}

	@Override
	public String getType()
	{
		return _C__90_REQUESTPRIVATESTOREMANAGEBUY;
	}
}
