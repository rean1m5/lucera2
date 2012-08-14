package ru.catssoftware.gameserver.gmaccess.handlers;

import java.util.Calendar;

import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.AuctionManager;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

import javolution.text.TextBuilder;

public class siege extends gmHandler
{
	private static final String[] commands =
	{
		"rainbowspring_startsiege",
		"devcastle_startsiege",
		"fodcastle_startsiege",
		"clanhall",
		"siege",
		"add_attacker",
		"add_defender",
		"list_siege_clans",
		"clear_siege_list",
		"move_defenders",
		"spawn_doors",
		"endsiege",
		"startsiege",
		"setsiegetime",
		"setcastle",
		"removecastle",
		"clanhallset",
		"clanhalldel",
		"clanhallopendoors",
		"clanhallclosedoors",
		"clanhallteleportself"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];
		Castle castle = null;
		ClanHall clanhall = null;

		if (command.equals("rainbowspring_startsiege"))
		{
			RainbowSpringSiege.getInstance().startSiege();
			return;
		}
		if (command.equals("devcastle_startsiege"))
		{
			DevastatedCastleSiege.getInstance().startSiege();
			return;
		}
		if (command.equals("fodcastle_startsiege"))
		{
			FortressOfDeadSiege.getInstance().startSiege();
			return;
		}

		if (command.startsWith("clanhall") && params.length > 1)
			clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(params[1]));
		else if (params.length > 1)
			castle = CastleManager.getInstance().getCastleByName(params[1]);

		String val = "";
		if (params.length > 2)
			val = params[2];

		if ((castle == null || castle.getCastleId() < 0) && clanhall == null)
			showCastleSelectPage(admin);
		else
		{
			L2Object target = admin.getTarget();
			L2PcInstance player = null;
			if (target != null && target instanceof L2PcInstance)
				player = (L2PcInstance) target;

			if (command.equalsIgnoreCase("add_attacker") && castle != null)
			{
				if (player == null)
					admin.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else
					castle.getSiege().registerToSiege(player, castle.getCastleId(), 1);
			}
			else if (command.equalsIgnoreCase("add_defender") && castle != null)
			{
				if (player == null)
					admin.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else
					castle.getSiege().registerToSiege(player, castle.getCastleId(), 2);
			}
			else if (command.equalsIgnoreCase("clear_siege_list") && castle != null)
			{
				castle.getSiege().clearSiegeClan();
			}
			else if (command.equalsIgnoreCase("endsiege") && castle != null)
			{
				castle.getSiege().endSiege();
			}
			else if (command.equalsIgnoreCase("list_siege_clans") && castle != null)
			{
				castle.getSiege().listRegisterClan(admin);
			}
			else if (command.equalsIgnoreCase("move_defenders"))
			{
				admin.sendMessage("Функция не реализована");
			}
			else if (command.equalsIgnoreCase("setcastle") && castle != null)
			{
				if (player == null || player.getClan() == null)
					admin.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else
					castle.setOwner(player.getClan());
			}
			else if (command.equalsIgnoreCase("removecastle") && castle != null)
			{
				L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				if (clan != null)
					castle.removeOwner(clan);
				else
					admin.sendMessage("Невозможно удалить владельца");
			}
			else if (command.equalsIgnoreCase("setsiegetime"))
			{
				if (params.length > 3)
				{
					if (castle == null)
						return;

					Calendar newAdminSiegeDate = castle.getSiegeDate();
					if (val.equalsIgnoreCase("day"))
						newAdminSiegeDate.set(Calendar.DAY_OF_YEAR, Integer.parseInt(params[3]));
					else if (val.equalsIgnoreCase("hour"))
						newAdminSiegeDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(params[3]));
					else if (val.equalsIgnoreCase("min"))
						newAdminSiegeDate.set(Calendar.MINUTE, Integer.parseInt(params[3]));

					if (newAdminSiegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
						admin.sendMessage("Невозможно сменить время осады");
					else if (newAdminSiegeDate.getTimeInMillis() != castle.getSiegeDate().getTimeInMillis())
						castle.getSiegeDate().setTimeInMillis(newAdminSiegeDate.getTimeInMillis());
				}
				showSiegeTimePage(admin,castle);
				return;
			}
			else if (command.equalsIgnoreCase("clanhallset") && clanhall != null)
			{
				if (player == null || player.getClan() == null)
					admin.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				else if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
					admin.sendMessage("Этот клан холл занят");
				else if (player.getClan().getHasHideout() == 0)
				{
					ClanHallManager.getInstance().setOwner(clanhall.getId(), player.getClan());
					if (AuctionManager.getInstance().getAuction(clanhall.getId()) != null)
						AuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
					if (clanhall.getId()==34 && !DevastatedCastleSiege.getInstance().checkIsRegistered(player.getClan()))
						DevastatedCastleSiege.getInstance().saveSiegeClan(player.getClan());
					if (clanhall.getId()==64 && !FortressOfDeadSiege.getInstance().checkIsRegistered(player.getClan()))
						FortressOfDeadSiege.getInstance().saveSiegeClan(player.getClan());
				}
				else
					admin.sendMessage("У Вас уже есть холл клана");
			}
			else if (command.equalsIgnoreCase("clanhalldel") && clanhall != null)
			{
				if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
				{
					ClanHallManager.getInstance().setFree(clanhall.getId());
                    int ClanHallID = clanhall.getId(); 
                    if (!(ClanHallID==21 ||ClanHallID==34 ||ClanHallID==35 ||ClanHallID==62 ||ClanHallID==63 ||ClanHallID==64)) 
                    	AuctionManager.getInstance().initNPC(clanhall.getId());
				}
				else
					admin.sendMessage("Этот холл клана уже свободен");
			}
			else if (command.equalsIgnoreCase("clanhallopendoors") && clanhall != null)
				clanhall.openCloseDoors(true);
			else if (command.equalsIgnoreCase("clanhallclosedoors") && clanhall != null)
				clanhall.openCloseDoors(false);
			else if (command.equalsIgnoreCase("clanhallteleportself") && clanhall != null)
			{
				L2Zone zone = clanhall.getZone();
				if (zone != null)
				{
					Location loc = zone.getRestartPoint(L2Zone.RestartType.OWNER);
					if (loc == null)
						loc = zone.getRandomLocation();
					admin.teleToLocation(loc, false);
				}
			}
			else if (command.equalsIgnoreCase("spawn_doors") && castle != null)
				castle.spawnDoor();
			else if (command.equalsIgnoreCase("startsiege") && castle != null)
				castle.getSiege().startSiege();
			if (clanhall != null)
				showClanHallPage(admin, clanhall);
			else if (castle != null)
				showSiegePage(admin, castle.getName());
		}
		return;
	}

	private void showSiegeTimePage(L2PcInstance activeChar, Castle castle)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/castlesiegetime_menu.htm");
		adminReply.replace("%castleName%", castle.getName());
		adminReply.replace("%time%", castle.getSiegeDate().getTime().toString());
		Calendar newDay = Calendar.getInstance();
		boolean isSunday = false;
		if (newDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			isSunday = true;
		else
			newDay.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		if (!SevenSigns.getInstance().isDateInSealValidPeriod(newDay))
			newDay.add(Calendar.DAY_OF_MONTH, 7);

		if (isSunday)
		{
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 13);
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		else
		{
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 1);
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		activeChar.sendPacket(adminReply);
	}

	private void showCastleSelectPage(L2PcInstance activeChar)
	{
		int i = 0;
		int total = 0;
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/castles_menu2.htm");
		TextBuilder cList = new TextBuilder();
		for (Castle castle : CastleManager.getInstance().getCastles().values())
		{
			if (castle != null)
			{
				String name = castle.getName();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_siege " + name + "\">" + name + "</a></td>");
				i++;
				total++;
			}
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%castles%", total>0 ? cList.toString() : "<td><center>Замков нет</center></td>");
		cList.clear();
		i = 0;
		total = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">" + clanhall.getName() + "</a></td>");
				i++;
				total++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}		
		}
		adminReply.replace("%clanhalls%", total>0 ? cList.toString() : "<td><center>Клан холлов нет</center></td>");
		cList.clear();
		i = 0;
		total = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">");
				cList.append(clanhall.getName() + "</a></td>");
				i++;
				total++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", total>0 ? cList.toString() : "<td><center>Свободных холлов нет</center></td>");
		activeChar.sendPacket(adminReply);
	}

	private void showSiegePage(L2PcInstance activeChar, String castleName)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/castle_menu.htm");
		adminReply.replace("%castleName%", castleName);
		activeChar.sendPacket(adminReply);
	}

	private void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/clanhall_menu.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getId()));
		L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());
		if (owner == null)
			adminReply.replace("%clanhallOwner%", "None");
		else
			adminReply.replace("%clanhallOwner%", owner.getName());
		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}