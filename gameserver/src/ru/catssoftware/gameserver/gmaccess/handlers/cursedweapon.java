package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.CursedWeapon;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import javolution.text.TextBuilder;


public class cursedweapon extends gmHandler
{
	CursedWeaponsManager 			cwm 			= CursedWeaponsManager.getInstance();
	private int						itemId			= 0;
	private static final String[] 	commands 		=
	{
			"cw_info",
			"cw_remove",
			"cw_goto",
			"cw_reload",
			"cw_drop",
			"cw_add",
			"cw_info_menu"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];
		int id = 0;

		if (command.startsWith("cw_drop")) {
			for (CursedWeapon cw : cwm.getCursedWeapons())
				if(cw.getPlayer()!=null)
					cw.dropIt(admin);
		}
		if (command.startsWith("cw_info"))
		{
			info(admin,command);
			return;
		}
		else if (command.equals("cw_reload"))
		{
			cwm.reload();
			info(admin,command);
			return;
		}
		else if (command.startsWith("cw_"))
		{
			CursedWeapon cw = null;
			try
			{
				if (params.length < 2)
				{
					admin.sendMessage("Не указан ID Проклятого Оружия");
					info(admin,command);
					return;
				}
				
				id = Integer.parseInt(params[1]);
				cw = cwm.getCursedWeapon(id);
				if (cw == null)
				{
					admin.sendMessage("Указаный ID оружия не найден в базе Проклятого Оружия сервера");
					info(admin,command);
					return;
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Указан неверный ID или ID отсутствует");
				return;
			}

			if (command.equals("cw_remove") && cw != null)
			{
				cw.endOfLife();
			}
			else if (command.equals("cw_goto") && cw != null)
			{
				cw.goTo(admin);
			}
			else if (command.equals("cw_add"))
			{
				if (cw == null)
				{
					admin.sendMessage("Используйте: //cw_add <itemid");
					info(admin,command);
					return;
				}
				else if (cw.isActive())
					admin.sendMessage("Это Проклятое оружие уже активно");
				else
				{
					L2Object target = admin.getTarget();
					if (target != null && target instanceof L2PcInstance)
						((L2PcInstance) target).addItem("AdminCursedWeaponAdd", id, 1, target, true);
					else
						admin.addItem("AdminCursedWeaponAdd", id, 1, admin, true);
				}
			}
			info(admin,command);
		}
	}

	public void info(L2PcInstance admin, String command)
	{
		if (!command.contains("menu"))
		{
			admin.sendMessage("====== Cursed Weapons: ======");
			for (CursedWeapon cw : cwm.getCursedWeapons())
			{
				admin.sendMessage("> " + cw.getName() + " (" + cw.getItemId() + ")");
				if (cw.isActivated())
				{
					L2PcInstance pl = cw.getPlayer();
					admin.sendMessage("  Player holding: " + (pl == null ? "null" : pl.getName()));
					admin.sendMessage("    Player karma: " + cw.getPlayerKarma());
					admin.sendMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
					admin.sendMessage("    Kills : " + cw.getNbKills());
				}
				else if (cw.isDropped())
				{
					admin.sendMessage("  Lying on the ground.");
					admin.sendMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
					admin.sendMessage("    Kills : " + cw.getNbKills());
				}
				else
				{
					admin.sendMessage("  Don't exist in the world.");
				}
				admin.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
			}
		}
		else
		{
			TextBuilder replyMSG = new TextBuilder();
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setFile("data/html/admin/menus/submenus/cwinfo.htm");
			for (CursedWeapon cw : cwm.getCursedWeapons())
			{
				itemId = cw.getItemId();
				replyMSG.append("<table width=270><tr><td>Name:</td><td>" + cw.getName() + "</td></tr>");
				if (cw.isActivated())
				{
					L2PcInstance pl = cw.getPlayer();
					replyMSG.append("<tr><td>Weilder:</td><td>" + (pl == null ? "null" : pl.getName()) + "</td></tr>");
					replyMSG.append("<tr><td>Karma:</td><td>" + String.valueOf(cw.getPlayerKarma()) + "</td></tr>");
					replyMSG.append("<tr><td>Kills:</td><td>" + String.valueOf(cw.getPlayerPkKills()) + "/" + String.valueOf(cw.getNbKills()) + "</td></tr>");
					replyMSG.append("<tr><td>Time remaining:</td><td>" + String.valueOf(cw.getTimeLeft() / 60000) + " min.</td></tr>");
					replyMSG.append("<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove " + String.valueOf(itemId) + "\" width=73 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
					replyMSG.append("<td><button value=\"Go\" action=\"bypass -h admin_cw_goto " + String.valueOf(itemId) + "\" width=73 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
				}
				else if (cw.isDropped())
				{
					replyMSG.append("<tr><td>Position:</td><td>Lying on the ground</td></tr>");
					replyMSG.append("<tr><td>Time remaining:</td><td>" + String.valueOf(cw.getTimeLeft() / 60000) + " min.</td></tr>");
					replyMSG.append("<tr><td>Kills:</td><td>" + String.valueOf(cw.getNbKills()) + "</td></tr>");
					replyMSG.append("<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove " + String.valueOf(itemId) + "\" width=73 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
					replyMSG.append("<td><button value=\"Go\" action=\"bypass -h admin_cw_goto " + String.valueOf(itemId) + "\" width=73 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
				}
				else
				{
					replyMSG.append("<tr><td>Position:</td><td>Doesn't exist.</td></tr>");
					replyMSG.append("<tr><td><button value=\"Give to Target\" action=\"bypass -h admin_cw_add " + String.valueOf(itemId) + "\" width=99 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td></td></tr>");
				}
				replyMSG.append("</table>");
				replyMSG.append("<br>");
			}
			adminReply.replace("%cwinfo%", replyMSG.toString());
			admin.sendPacket(adminReply);
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}