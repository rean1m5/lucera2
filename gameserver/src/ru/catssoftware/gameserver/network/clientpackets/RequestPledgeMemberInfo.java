package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PledgeReceiveMemberInfo;

public class RequestPledgeMemberInfo extends L2GameClientPacket
{
	private static final String	_C__24_REQUESTJOINPLEDGE	= "[C] 24 RequestPledgeMemberInfo";

	@SuppressWarnings("unused")
	private int					_pledgeType;
	private String				_target;

	@Override
	protected void readImpl()
	{
		_pledgeType = readD();
		_target = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			L2ClanMember cm = clan.getClanMember(_target);
			activeChar.sendPacket(new PledgeReceiveMemberInfo(cm));
		}
	}

	@Override
	public String getType()
	{
		return _C__24_REQUESTJOINPLEDGE;
	}
}