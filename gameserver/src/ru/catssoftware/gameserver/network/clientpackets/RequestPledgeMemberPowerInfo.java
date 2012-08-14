package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.network.serverpackets.PledgeReceivePowerInfo;

public class RequestPledgeMemberPowerInfo extends L2GameClientPacket
{
	private static final String	_C__24_REQUESTJOINPLEDGE	= "[C] 24 RequestPledgeMemberPowerInfo";

	@SuppressWarnings("unused")
	private int					_unk1;
	private String				_target;

	@Override
	protected void readImpl()
	{
		_unk1 = readD();
		_target = readS();
	}

	@Override
	protected void runImpl()
	{
		L2Clan clan = getClient().getActiveChar().getClan();
		if (clan != null)
		{
			L2ClanMember cm = clan.getClanMember(_target);
			if (cm != null)
				getClient().getActiveChar().sendPacket(new PledgeReceivePowerInfo(cm));
		}
	}

	@Override
	public String getType()
	{
		return _C__24_REQUESTJOINPLEDGE;
	}
}
