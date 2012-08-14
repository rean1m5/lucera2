package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestWithDrawalParty extends L2GameClientPacket
{
	private static final String	_C__2B_REQUESTWITHDRAWALPARTY	= "[C] 2B RequestWithDrawalParty";

	@Override
	public String getType()
	{
		return _C__2B_REQUESTWITHDRAWALPARTY;
	}

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (player.isInParty())
			if (player.getParty().isInDimensionalRift() && !player.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(player))
				player.sendMessage("Вы не можете покинуть группу в Дименьшен Рифт.");
			else
				player.getParty().removePartyMember(player,false);
	}
}