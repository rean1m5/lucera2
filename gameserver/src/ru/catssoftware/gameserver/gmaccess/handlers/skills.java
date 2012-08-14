package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.PledgeSkillList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import javolution.text.TextBuilder;



public class skills extends gmHandler
{
	private static final String[] commands =
	{
			"show_skills",
			"remove_skills",
			"skill_list",
			"skill_index",
			"add_skill",
			"remove_skill",
			"get_skills",
			"reset_skills",
			"give_all_skills",
			"remove_all_skills",
			"add_clan_skill",
			"cast_skill",
			"clear_skill_reuse"
	};

	private static L2Skill[]		adminSkills;

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		// Главное меню работы со скилами
		if (command.equals("show_skills"))
			showMainPage(admin);
		// Вывод списка скилов
		else if (command.equals("skill_list"))
			methods.showSubMenuPage(admin, "skills_menu.htm");
		// Получить скилы указаной цели
		else if (command.equals("get_skills"))
			adminGetSkills(admin);
		// Вернуть свои скилы
		else if (command.equals("reset_skills"))
			adminResetSkills(admin);
		// Выучить все скилы
		else if (command.equals("give_all_skills"))
			adminGiveAllSkills(admin);
		// Каст скила
		else if (command.equals("cast_skill"))
		{
			if (params.length > 1)
				castSkill(admin, params[1]);
			else
				admin.sendMessage("Неверный аргумент");
		}
		// Удаление скила по одному
		else if (command.startsWith("remove_skills"))
		{
			try
			{
				removeSkillsPage(admin, Integer.parseInt(params[1]));
			}
			catch (Exception e)
			{
			}
		}
		// Показ страницы работы со скилами
		else if (command.startsWith("skill_index"))
		{
			try
			{
				methods.showHelpPage(admin, "skills/" + params[1] + ".htm");
			}
			catch (Exception e)
			{
			}
		}
		// Добавление скила по одному
		else if (command.startsWith("add_skill"))
		{
			try
			{
				int id = Integer.parseInt(params[1]);
				int level = 1;

				if (params.length > 2)
					level = Integer.parseInt(params[2]);
				adminAddSkill(admin, id, level);
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //add_skill <id> <level>");
			}
		}
		// Удаление скила по одному
		else if (command.startsWith("remove_skill"))
		{
			try
			{
				adminRemoveSkill(admin, Integer.parseInt(params[1]));
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //remove_skill <id>");
			}
		}
		// Удаление всех скилов
		else if (command.equals("remove_all_skills"))
		{
			L2Object tmp = admin.getTarget();
			if (tmp == null)
				tmp = admin;

			if (tmp instanceof L2PcInstance)
			{
				// Переменные
				int count = 0;
				L2PcInstance player = (L2PcInstance)tmp;
				// Удаляем скилы
				for (L2Skill skill : player.getAllSkills())
				{
					if (skill == null)
						continue;
					player.removeSkill(skill);
					count++;
				}
				// Извещаем о ходе операции
				admin.sendMessage(count + " скил(ов) удалено игроку " + player.getName());
				player.sendMessage("Администратор удалил все Ваши умения");
				// Шлем список скилов
				player.sendSkillList();
			}
		}
		// Добавление клан скилов
		else if (command.equals("add_clan_skill"))
		{
			try
			{
				adminAddClanSkill(admin, Integer.parseInt(params[1]), Integer.parseInt(params[2]));
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //add_clan_skill <id> <level>");
			}
		}
		// Подготовка скилов к использованию
		else if (command.equals("clear_skill_reuse"))
		{
			L2Object object = admin.getTarget();
			if (object == null)
			{
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}

			if (object instanceof L2PcInstance)
			{
				((L2PcInstance)object).resetSkillTime(true);
				((L2PcInstance)object).sendMessage("Ваши умения вновь готовы к использованию.");
				admin.sendMessage("Умения игрока " + ((L2PcInstance)object).getName() + " готовы к использованию.");
			}
			else
			{
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}
		}
	}

	private void adminGiveAllSkills(L2PcInstance activeChar)
	{
		// Плучение цели
		L2Object target = activeChar.getTarget();
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		// Создание переменной чара
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		// Проверка NPE
		if (player == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		// Переменные
		// Получение всех скилов
		player.giveAvailableSkills();
	}

	private void removeSkillsPage(L2PcInstance activeChar, int page)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;
		
		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		L2Skill[] skills = player.getAllSkills();

		int MaxSkillsPerPage = 10;
		int MaxPages = skills.length / MaxSkillsPerPage;
		if (skills.length > MaxSkillsPerPage * MaxPages)
			MaxPages++;

		if (page > MaxPages)
			page = MaxPages;

		int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = skills.length;
		if (SkillsEnd - SkillsStart > MaxSkillsPerPage)
			SkillsEnd = SkillsStart + MaxSkillsPerPage;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Editing <font color=\"LEVEL\">" + player.getName() + "</font></center>");
		replyMSG.append("<br><table width=270><tr><td>Lv: " + player.getLevel() + " " + player.getTemplate().getClassName() + "</td></tr></table>");
		replyMSG.append("<br><table width=270><tr><td>Note: Dont forget that modifying players skills can</td></tr>");
		replyMSG.append("<tr><td>ruin the game...</td></tr></table>");
		replyMSG.append("<br><center>Click on the skill you wish to remove:</center>");
		replyMSG.append("<br>");
		String pages = "<center><table width=270><tr>";
		for (int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			pages += "<td><a action=\"bypass -h admin_remove_skills " + x + "\">Page " + pagenr + "</a></td>";
		}
		pages += "</tr></table></center>";
		replyMSG.append(pages);
		replyMSG.append("<br><table width=270>");
		replyMSG.append("<tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");
		for (int i = SkillsStart; i < SkillsEnd; i++)
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_remove_skill " + skills[i].getId() + "\">" + skills[i].getName() + "</a></td><td width=60>" + skills[i].getLevel() + "</td><td width=40>" + skills[i].getId() + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<br><center><table>");
		replyMSG.append("Remove skill by ID :");
		replyMSG.append("<tr><td>Id: </td>");
		replyMSG.append("<td><edit var=\"id_to_remove\" width=110></td></tr>");
		replyMSG.append("</table></center>");
		replyMSG.append("<center><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></center>");
		replyMSG.append("<br><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;
		
		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/charskills_menu.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", player.getTemplate().getClassName());
		activeChar.sendPacket(adminReply);
	}

	private void adminGetSkills(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;
		
		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		if (player == activeChar)
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		else
		{
			L2Skill[] skills = player.getAllSkills();
			adminSkills = activeChar.getAllSkills();
			for (L2Skill element : adminSkills)
				activeChar.removeSkill(element);
			for (L2Skill element : skills)
				activeChar.addSkill(element, true);
			activeChar.sendMessage("Вы получили бафы игрока " + player.getName());
			activeChar.sendSkillList();
		}
		showMainPage(activeChar);
	}

	private void adminResetSkills(L2PcInstance activeChar)
	{
		if (adminSkills == null)
			activeChar.sendMessage("Вы не можете сделать сброс сейчас");
		else
		{
			L2Skill[] skills = activeChar.getAllSkills();
			for (L2Skill skill : skills)
				activeChar.removeSkill(skill);
			for (L2Skill skill : adminSkills)
				activeChar.addSkill(skill, true);
			activeChar.sendMessage("Вы вернули свои бафы");
			adminSkills = null;
			activeChar.sendSkillList();
		}
		showMainPage(activeChar);
	}

	/**
	 * Добавление скила
	 * @param activeChar
	 * @param id
	 * @param lvl
	 */
	private void adminAddSkill(L2PcInstance activeChar, int id, int lvl)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		if (!(target instanceof L2PcInstance))
		{
			showMainPage(activeChar);
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
		if (skill != null)
		{
			String name = skill.getName();	
			player.addSkill(skill, true);
			player.sendSkillList();
			player.sendMessage("Вы изучили скил " + name);
			activeChar.sendMessage("Вы добавили скил " + name + " игроку " + player.getName());
		}
		else
			activeChar.sendMessage("Ошибка. Скил не существует");

		showMainPage(activeChar);		
	}

	private void adminRemoveSkill(L2PcInstance activeChar, int idval)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(idval, player.getSkillLevel(idval));
		if (skill != null)
		{
			String skillname = skill.getName();
			player.sendMessage("Администратор удалил вам скилл " + skillname + ".");
			player.removeSkill(skill);
			activeChar.sendMessage("Вы удалили скил " + skillname + " игроку " + player.getName() + ".");
			player.sendSkillList();
		}
		else
			activeChar.sendMessage("Ошибка: скил не существует.");
		removeSkillsPage(activeChar, 0);
	}

	private void adminAddClanSkill(L2PcInstance activeChar, int id, int level)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
		{
			showMainPage(activeChar);
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		if (!player.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(player.getName()));
			showMainPage(activeChar);
			return;
		}
		if ((id < 370) || (id > 391) || (level < 1) || (level > 3))
		{
			activeChar.sendMessage("Используйте: //add_clan_skill <id> <level>");
			showMainPage(activeChar);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill != null)
		{
			String skillname = skill.getName();
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			player.getClan().broadcastToOnlineMembers(sm);
			player.getClan().addNewSkill(skill);
			activeChar.sendMessage("Добавлен скилл " + skillname + " клану " + player.getClan().getName());

			activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));
			for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
				member.sendSkillList();

			showMainPage(activeChar);
			return;
		}

		activeChar.sendMessage("Ошибка. Скилл не существует");
	}

	public void castSkill(L2PcInstance activeChar, String val)
	{
		int skillid = Integer.parseInt(val);
		L2Skill skill = SkillTable.getInstance().getInfo(skillid, 1);
		if (skill != null)
		{
			if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
			{
				activeChar.setTarget(activeChar);
				MagicSkillUse msk = new MagicSkillUse(activeChar, skillid, 1, skill.getHitTime(), skill.getReuseDelay(), skill.isPositive());
				activeChar.broadcastPacket(msk);
			}
		}
		else
			activeChar.broadcastPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}