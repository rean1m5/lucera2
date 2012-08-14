package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class AllyLeave extends L2GameClientPacket
{
	private static final String	_C__84_ALLYLEAVE	= "[C] 84 AllyLeave";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		if (player.getClan() == null)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
			return;
		}
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_WITHDRAW_ALLY);
			return;
		}
		L2Clan clan = player.getClan();
		if (clan.getAllyId() == 0)
		{
			player.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}
		if (clan.getClanId() == clan.getAllyId())
		{
			player.sendPacket(SystemMessageId.ALLIANCE_LEADER_CANT_WITHDRAW);
			return;
		}

		long currentTime = System.currentTimeMillis();
		clan.setAllyId(0);
		clan.setAllyName(null);
		clan.setAllyCrestId(0);
		clan.setAllyPenaltyExpiryTime(currentTime + Config.ALT_ALLY_JOIN_DAYS_WHEN_LEAVED * 86400000L, L2Clan.PENALTY_TYPE_CLAN_LEAVED); //24*60*60*1000 = 86400000
		clan.updateClanInDB();

		player.sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_ALLIANCE);

		// Added to delete the Alliance Crest when a clan leaves an ally.
		player.getClan().setAllyCrestId(0);
		for (L2PcInstance member : player.getClan().getOnlineMembers(0))
			member.broadcastUserInfo();
	}

	@Override
	public String getType()
	{
		return _C__84_ALLYLEAVE;
	}
}
