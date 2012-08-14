package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExMPCCShowPartyMemberInfo;

public final class RequestExMPCCShowPartyMembersInfo extends L2GameClientPacket
{
	private static final String	_C__D0_26_REQUESTMPCCSHOWPARTYMEMBERINFO	= "[C] D0:26 RequestExMPCCShowPartyMembersInfo";
	private int					_leaderId;

	@Override
	protected void readImpl()
	{
		_leaderId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null || player.getParty() == null || player.getParty().getCommandChannel() == null)
			return;

		for (L2Party party : player.getParty().getCommandChannel().getPartys())
		{
			if (party.getLeader().getObjectId() == _leaderId)
			{
				player.sendPacket(new ExMPCCShowPartyMemberInfo(party));
				return;
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_26_REQUESTMPCCSHOWPARTYMEMBERINFO;
	}
}