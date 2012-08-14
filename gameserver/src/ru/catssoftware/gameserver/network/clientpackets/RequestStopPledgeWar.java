package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.taskmanager.AttackStanceTaskManager;

public class RequestStopPledgeWar extends L2GameClientPacket
{
	private static final String	_C__4F_REQUESTSTOPPLEDGEWAR	= "[C] 4F RequestStopPledgeWar";

	String						_pledgeName;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		L2Clan playerClan = player.getClan();
		if (playerClan == null)
			return;

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if (clan == null)
		{
			player.sendMessage("Клан не существует.");
			ActionFailed();
			return;
		}
		if (!playerClan.isAtWarWith(clan.getClanId()))
		{
			player.sendMessage("Вы не состоите в войне с кланами.");
			ActionFailed();
			return;
		}
		if (!((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			player.sendMessage("У Вас нет прав для отмены войны клана");
			ActionFailed();
			return;
		}
		for (L2ClanMember member : playerClan.getMembers())
		{
			if (member == null || member.getPlayerInstance() == null)
				continue;
			if (AttackStanceTaskManager.getInstance().getAttackStanceTask(member.getPlayerInstance()))
			{
				player.sendMessage("Нельзя отменить войну, когда один из членов клана находится в бою.");
				return;
			}
		}
		ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
	}

	@Override
	public String getType()
	{
		return _C__4F_REQUESTSTOPPLEDGEWAR;
	}
}
