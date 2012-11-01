package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.GMViewPledgeInfo;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class pledge extends gmHandler
{
	private static final String[] commands =
	{
		"pledge"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		L2Object target = admin.getTarget();
		L2PcInstance player = null;
		if (target != null && target.isPlayer())
			player = (L2PcInstance) target;
		else
		{
			admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		String name = player.getName();
		if (command.startsWith("pledge"))
		{
			String action = null;
			String parameter = null;
			try
			{
				action = params[1]; // create|info|dismiss|setlevel|rep
				parameter = params[2]; // clanname|nothing|nothing|level|rep_points
			}
			catch (Exception e)
			{
			}

			if (action.equals("create"))
			{
				long cet = player.getClanCreateExpiryTime();
				player.setClanCreateExpiryTime(0);
				if (parameter == null)
					return;

				L2Clan clan = ClanTable.getInstance().createClan(player, parameter);
				if (clan != null)
					admin.sendMessage("Клан " + parameter + " создан. Лидер: " + name);
				else
				{
					player.setClanCreateExpiryTime(cet);
					admin.sendMessage("Не удается создать клан");
				}
			}
			else if (!player.isClanLeader())
			{
				admin.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(name));
				return;
			}
			else if (action.equals("dismiss"))
			{
				ClanTable.getInstance().destroyClan(player.getClanId());
				L2Clan clan = player.getClan();
				if (clan == null)
					admin.sendMessage("Клан удален");
				else
					admin.sendMessage("Не удается удалить клан");
			}
			else if (parameter == null)
			{
				admin.sendMessage("Используйте: //pledge <setlevel|rep> <number>");
			}
			else if (action.equals("info"))
			{
				admin.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
			}
			else if (action.equals("setlevel"))
			{
				int level = 0;
				try
				{
					level = Integer.parseInt(parameter);
				}
				catch (NumberFormatException nfe)
				{
				}

				if (level >= 0 && level < 12)
				{
					player.getClan().changeLevel(level);
					admin.sendMessage("Уровень клана " + player.getClan().getName() + " изменен на " + level);
				}
				else
					admin.sendMessage("Неверный уровень");
			}
			else if (action.startsWith("rep"))
			{
				int points = 0;
				try
				{
					points = Integer.parseInt(parameter);
				}
				catch (NumberFormatException nfe)
				{
					admin.sendMessage("Используйте: //pledge <rep> <number>");
				}

				L2Clan clan = player.getClan();
				if (clan.getLevel() < 5)
				{
					admin.sendMessage("Репутацию могут получать только кланы, которые достигли 5 уровня");
					return;
				}
				clan.setReputationScore(clan.getReputationScore() + points, true);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				admin.sendMessage("Вы " + (points > 0 ? "добавили " : "отняли ") + Math.abs(points) + " очков репутации клана " + clan.getName() + ". Количество репутации клана составило " + clan.getReputationScore());
			}
		}
		return;
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}
