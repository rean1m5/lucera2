package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Clan.RankPrivs;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PledgePowerGradeList;

public class RequestPledgePowerGradeList extends L2GameClientPacket
{
	private static final String	_C__C0_REQUESTPLEDGEPOWER	= "[C] C0 RequestPledgePowerGradeList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		L2Clan clan = player.getClan();
		if (clan != null)
		{
			RankPrivs[] privs = clan.getAllRankPrivs();
			player.sendPacket(new PledgePowerGradeList(privs));
		}
	}

	@Override
	public String getType()
	{
		return _C__C0_REQUESTPLEDGEPOWER;
	}
}