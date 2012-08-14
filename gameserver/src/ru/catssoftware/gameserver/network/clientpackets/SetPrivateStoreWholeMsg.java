package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;

public class SetPrivateStoreWholeMsg extends L2GameClientPacket
{
	private String	_msg;

	@Override
	protected void readImpl()
	{
		_msg = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null || player.getSellList() == null)
			return;

		player.getSellList().setTitle(_msg);
		sendPacket(new ExPrivateStoreSetWholeMsg(player));
	}

	@Override
	public String getType()
	{
		return "[C] D0:4D SetPrivateStoreWholeMsg";
	}
}
