package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestStartPledgeWar extends L2GameClientPacket
{
	private static final String	_C__4D_REQUESTSTARTPLEDGEWAR	= "[C] 4D RequestStartPledgewar";

	String						_pledgeName;
	L2Clan						_clan;
	L2PcInstance				player;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		player = getClient().getActiveChar();
		if (player == null)
			return;

		_clan = getClient().getActiveChar().getClan();
		if (_clan == null)
			return;

		if (_clan.getLevel() < 3 || _clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			player.sendPacket(sm);
			ActionFailed();
			sm = null;
			return;
		}
		if (!((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			player.sendMessage("У Вас нет прав для создания войны клана");
			ActionFailed();
			return;
		}

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_CANNOT_DECLARED_CLAN_NOT_EXIST);
			player.sendPacket(sm);
			ActionFailed();
			return;
		}
		if (_clan.getAllyId() == clan.getAllyId() && _clan.getAllyId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
			player.sendPacket(sm);
			ActionFailed();
			sm = null;
			return;
		}
		if (clan.getLevel() < 3 || clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			player.sendPacket(sm);
			ActionFailed();
			sm = null;
			return;
		}
		if (_clan.isAtWarWith(clan.getClanId()))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.ALREADY_AT_WAR_WITH_S1_WAIT_5_DAYS);
			sm.addString(clan.getName());
			player.sendPacket(sm);
			ActionFailed();
			sm = null;
			return;
		}

		ClanTable.getInstance().storeclanswars(player.getClanId(), clan.getClanId());
	}

	@Override
	public String getType()
	{
		return _C__4D_REQUESTSTARTPLEDGEWAR;
	}
}
