package ru.catssoftware.gameserver.gmaccess.handlers;

import javolution.text.TextBuilder;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.CharNameTable;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.util.PcAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class editchar extends gmHandler
{
	private static final String[] commands =
	{
		"current_player",
		"edit_char",
		"setclass",
		"save_modifications",
		"changename",
		"setname",
		"setcolor",
		"settitle",
		"skills",
		"rec",
		"setsex",
		"character_info",
		"show_characters",
		"find_character",
		"find_ip",
		"find_account",
		"debug"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		if (command.equals("current_player"))
		{
			showCharacterInfo(admin, null);
			return;
		}
		else if (command.equals("edit_char"))
		{
			editChar(admin);
			return;
		}
		else if (command.equals("skills")) {
			try {
				int nStart = 0;
				int cnt = 0;
				if(params.length>=3)
					nStart = Integer.parseInt(params[2]);
				String html = "";
				Connection con = L2DatabaseFactory.getInstance().getConnection();
				String sql = "select distinct skill_id,sk.name from skill_trees sk inner join class_list cl on cl.id = sk.class_id and cl.class_name like '"+params[1].replace("_","\\_")+"%' ";
				if (params.length==4)
					sql+=" where sk.name like '"+params[3]+"%'";
				sql+=" order by sk.name";
				PreparedStatement stm = con.prepareStatement(sql);
				ResultSet rs = stm.executeQuery();
				boolean endReached = true;
				while (rs.next()) {
					if(++cnt<nStart) 
						continue;
					L2Skill skill = SkillTable.getInstance().getInfo(rs.getInt(1), 1);
					if(skill!=null) {
						int maxlevel = SkillTable.getInstance().getMaxLevel(rs.getInt(1));
						html+="<tr><td>"+skill.getName()+" (max "+maxlevel+" lvl )</td><td><button action=\"bypass -h admin_add_skill "+rs.getInt(1)+" $level\" value=\"Add\" width=35 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>";
					}
					if(html.length()>7000) {
						endReached = false;
						break;
					}
				}
				String more="";
				if(!endReached || nStart!=0) {
					if(nStart>0) {
						nStart-=cnt;
						if(nStart<0) nStart = 0;
						more +="<a action=\"bypass -h admin_skills "+params[1]+" "+nStart+"\">Назад</a>  ";
					}
					if(!endReached)
						more +="<a action=\"bypass -h admin_skills "+params[1]+" "+(cnt+1)+"\">Дальше</a>";
				}
				
				rs.close();
				stm.close();
				con.close();
				NpcHtmlMessage msg = new NpcHtmlMessage(5);
				msg.setFile("data/html/admin/menus/skills.htm");
				msg.replace("%race%",params[1]);
				msg.replace("%skills%", html);
				msg.replace("%more%",more);
				admin.sendPacket(msg);
			} catch(SQLException e) {
				
			}
		}
		else if (command.equals("setclass"))
		{
			if (params.length > 1)
			{
				try
				{
					L2Object target = admin.getTarget();
					L2PcInstance player = null;
					int classidval = 0;
					if (!(target.isPlayer()))
						return;

					player = (L2PcInstance) target;
					classidval = Integer.parseInt(params[1]);
					boolean valid = false;

					for (ClassId classid : ClassId.values())
					{
						if (classidval == classid.getId())
							valid = true;
					}
					if (valid && (player.getClassId().getId() != classidval))
					{
						player.setClassId(classidval);
						if (!player.isSubClassActive())
							player.setBaseClass(classidval);
						String newclass = player.getTemplate().getClassName();
						player.store();
						if (player != admin)
							player.sendMessage("Ваш класс изменен на " + newclass);
						player.broadcastUserInfo(true);
						admin.sendMessage("Игроку " + player.getName() + " изменен класс на " + newclass);
					}
					else
					{
						admin.sendMessage("Указан неверный Id класса");
						methods.showSubMenuPage(admin, "charclasses_menu.htm");
						return;
					}
				}
				catch (Exception e)
				{
					admin.sendMessage("Указан неверный Id класса");
					methods.showSubMenuPage(admin, "charclasses_menu.htm");
					return;
				}				
			}
			else
			{
				methods.showSubMenuPage(admin, "charclasses_menu.htm");
				return;
			}
		}
		else if(command.equals("setcolor")) {
			try {
				int newColor = Integer.parseInt(params[1],16);
				if(admin.getTarget().isPlayer()) {
					((L2PcInstance)admin.getTarget()).getAppearance().setNameColor(newColor);
					((L2PcInstance)admin.getTarget()).broadcastFullInfo();
				}
			} catch(Exception e) {
				
			}
		}
		else if (command.equals("save_modifications"))
		{
			try
			{
				adminModifyCharacter(admin, params);
			}
			catch (Exception e)
			{
				admin.sendMessage("Параметры заданы неверно.");
				editChar(admin);
			}
		}
		else if (command.equals("changename") || command.equals("setname"))
		{
			if (params.length < 2)
			{
				admin.sendMessage("Вы не указали имя персонажа");
				return;
			}
			try
			{
				L2Object target = admin.getTarget();
				if (target == null)
					target = admin;
				String newName = params[1];
				String oldName = null;
				if (target.isPlayer())
				{
					L2PcInstance player = (L2PcInstance) target;
					oldName = player.getName();

					if (CharNameTable.getInstance().getByName(newName) != null)
					{
						admin.sendMessage("Указаное имя уже используется");
						return;
					}
	
					L2World.getInstance().removeFromAllPlayers(player);
					player.changeName(newName);
					player.store();
					L2World.getInstance().addToAllPlayers(player);
					player.sendMessage("Ваше имя изменено Gm'ом");
					player.broadcastUserInfo(true);
					if (player.isInParty())
						player.getParty().refreshPartyView();
					if (player.getClan() != null)
						player.getClan().broadcastClanStatus();
				}
				else if (target instanceof L2NpcInstance)
				{
					L2NpcInstance npc = (L2NpcInstance) target;
					oldName = npc.getName();
					npc.setName(newName);
					npc.updateAbnormalEffect();
				}
				
				/* Notify */
				if (oldName == null)
					admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else
					admin.sendMessage("Имя изменено на " + newName);
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //setname [name]");
			}
			return;
		}
		else if (command.equals("settitle"))
		{
			String val = "";
			L2Object target = admin.getTarget();
			if (target == null)
				target = admin;
			
			if (params.length > 1)
			{
				for (int i=1; i<params.length; i++)
					val += (" " + params[i]);
			}

			if (target.isPlayer())
			{
				L2PcInstance player = (L2PcInstance) target;
				player.setTitle(val);
				if (player != admin)
					player.sendMessage("Ваш титул изменен Gm'ом");
				player.broadcastTitleInfo();
				admin.sendMessage("Титул успешно изменен");
				return;
			}
			else if (target instanceof L2NpcInstance)
			{
				L2NpcInstance npc = (L2NpcInstance) target;
				npc.setTitle(val);
				npc.updateAbnormalEffect();
				admin.sendMessage("Титул успешно изменен");
				return;
			}
			else
				return;
		}
		else if (command.equals("rec"))
		{
			int val = 0;
			if (params.length > 1)
			{
				try
				{
					val = Integer.parseInt(params[1]);
					if (val > 255)
						val = 255;
					if (val < 0)
						val = 0;
				}
				catch (Exception e)
				{
				}
			}

			L2Object target = admin.getTarget();
			if (target == null)
				target = admin;
			
			if (target.isPlayer())
			{
				L2PcInstance player = (L2PcInstance) target;
				if (val == 0)
					player.setRecomHave(player.getRecomHave() + 1);
				else if (val == 255)
					player.setRecomHave(255);
				else
					player.setRecomHave(player.getRecomHave() + val);
				
				player.broadcastUserInfo(true);
				if (player != admin)
					player.sendMessage("Вас рекомендовал администратор");
				admin.sendMessage("Вы рекомендовали игрока " + player.getName());
			}
		}
		else if (command.equals("setsex"))
		{
			L2Object target = admin.getTarget();
			if (target == null)
				target = admin;
			
			if (target.isPlayer())
			{
				L2PcInstance player = (L2PcInstance) target;
				player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
				player.broadcastUserInfo(true);
				player.decayMe();
				player.spawnMe(player.getX(), player.getY(), player.getZ());
				PcAction.storeCharSex(player, 0);
				if (player != admin)
					player.sendMessage("Ваш пол сменен администратором");
				admin.sendMessage("Пол успешно изменен");
			}
			else
				return;
		}
		else if (command.equals("character_info"))
		{
			try
			{
				if (params.length > 1)
				{
					L2PcInstance target = L2World.getInstance().getPlayer(params[1]);
					if (target != null)
						showCharacterInfo(admin, target);
					else
						admin.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
				}
				else
					admin.sendMessage("Используйте: //character_info <name>");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //character_info <name>");
			}
		}
		else if (command.equals("debug"))
		{
			try
			{
				if (params.length > 1)
				{
					L2PcInstance target = L2World.getInstance().getPlayer(params[1]);
					if (target != null)
						showCharacterInfo(admin, target);
					else
						showCharacterInfo(admin, admin);
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.equals("show_characters"))
		{
			try
			{
				if (params.length > 1)
					listCharacters(admin, Integer.parseInt(params[1]));
				else
					listCharacters(admin, 0);
			}
			catch (Exception e)
			{
			}
		}
		else if (command.equals("find_character"))
		{
			try
			{
					findCharacter(admin, params[1]);
			}
			catch (Exception e)
			{
				listCharacters(admin, 0);
			}
		}
		else if (command.equals("find_ip"))
		{
			try
			{
				findCharactersPerIp(admin, params[1]);
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //find_ip <w.x.y.z>");
				findCharactersPerIp(admin, "0.0.0.0");
			}
			return;
		}
		else if (command.equals("find_account"))
		{
			try
			{
				findCharactersPerAccount(admin, params[1] );
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //find_account <name>");
				findCharactersPerAccount(admin, null);
			}
			return;
		}
	}

	/**
	 * Список всех игроков
	 * @param activeChar
	 * @param page
	 */
	private void listCharacters(L2PcInstance activeChar, int page)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;

		if (players.length > MaxCharactersPerPage * MaxPages)
			MaxPages++;
		if (page > MaxPages)
			page = MaxPages;

		int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
			CharactersEnd = CharactersStart + MaxCharactersPerPage;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/charlist_menu.htm");
		TextBuilder replyMSG = new TextBuilder();
		for (int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			replyMSG.append("<center><button value=\"Page " + pagenr + "\" action=\"bypass -h admin_show_characters " + x +"\" width=60 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></center>");
		}
		adminReply.replace("%pages%", replyMSG.toString());
		replyMSG.clear();
		for (int i = CharactersStart; i < CharactersEnd; i++)
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info " + players[i].getName() + "\">" + players[i].getName()+ "</a></td><td width=110>" + players[i].getTemplate().getClassName() + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void findCharacter(L2PcInstance activeChar, String CharacterToFind)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int found = 0;
		String name;
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/charfind_menu.htm");
		TextBuilder replyMSG = new TextBuilder();
		
	
		for (int i = 0; i < players.length; i++)
		{
			name = players[i].getName();
			if (name.toLowerCase().contains(CharacterToFind.toLowerCase()))
			{
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().getClassName() + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
				found++;
			}
		}
		if (found == 0)
			replyMSG.append("<tr><td>Не найдено</td></tr>");
		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	/**
	 * @param activeChar
	 * @param IpAdress
	 * @throws IllegalArgumentException
	 */
	private void findCharactersPerIp(L2PcInstance activeChar, String IpAdress) throws IllegalArgumentException
	{
		if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
			throw new IllegalArgumentException("Malformed IPv4 number");

		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		int CharactersFound = 0;
		String name = "", ip = "0.0.0.0";
		TextBuilder replyMSG = new TextBuilder();
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/ipfind_menu.htm");
		for (L2PcInstance player : players)
		{
			if (player == null)
				continue;

			ip = player.getHost();
			if (!ip.equals(IpAdress))
				continue;

			name = player.getName();
			CharactersFound++;
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().getClassName() + "</td><td width=40>" + player.getLevel() + "</td></tr>");

			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());
		adminReply.replace("%ip%", ip);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		activeChar.sendPacket(adminReply);
	}

	/**
	 * @param activeChar
	 * @param characterName
	 * @throws IllegalArgumentException
	 */
	private void findCharactersPerAccount(L2PcInstance activeChar, String characterName) throws IllegalArgumentException
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/accountinfo_menu.htm");

		if (characterName == null)
		{
			adminReply.replace("%characters%", "");
			adminReply.replace("%account%", "N/A");
			adminReply.replace("%player%", "N/A");
			activeChar.sendPacket(adminReply);
			return;
		}
		if (characterName.matches(Config.CNAME_PATTERN.pattern()))
		{
			String account = null;
			Map<Integer, String> chars;
			L2PcInstance player = L2World.getInstance().getPlayer(characterName);
			if (player == null)
				throw new IllegalArgumentException("Player doesn't exist");
			chars = player.getAccountChars();
			account = player.getAccountName();
			TextBuilder replyMSG = new TextBuilder();
			for (String charname : chars.values())
				replyMSG.append(charname + "<br1>");
			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);
			activeChar.sendPacket(adminReply);
		}
		else
			throw new IllegalArgumentException("Malformed character name");
	}
	
	/* ----------- Методы изменения, поиска персонажей и вывод Html ------------ */
	
	/**
	 * Метод модификации статов чара 
	 */
	private void adminModifyCharacter(L2PcInstance activeChar, String[] params)
	{
		L2Object target = activeChar.getTarget();
		if (!(target.isPlayer()))
			return;
		L2PcInstance player = (L2PcInstance) target;

		if (params.length < 7)
		{
			activeChar.sendMessage("Параметры заданы неверно");
			editChar(player);
			return;
		}
		try
		{
			int hpval = Integer.parseInt(params[1]);
			int mpval = Integer.parseInt(params[2]);
			int cpval = Integer.parseInt(params[3]);
			int pvpflagval = Integer.parseInt(params[4]);
			int pvpkillsval = Integer.parseInt(params[5]);
			int pkkillsval = Integer.parseInt(params[6]);
			
			player.getStatus().setCurrentHp(hpval);
			player.getStatus().setCurrentMp(mpval);
			player.getStatus().setCurrentCp(cpval);
			player.setPvpFlag(pvpflagval);
			player.setPvpKills(pvpkillsval);
			player.setPkKills(pkkillsval);
			player.store();
			
			player.broadcastStatusUpdateImpl();	
			player.broadcastUserInfo(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			activeChar.sendMessage("Параметры заданы неверно");
			editChar(player);
			return;
		}
		if (player != activeChar)
			player.sendMessage("Ваши характеристики изменены Gm'ом");
		activeChar.sendMessage("Характеристики успешно изменены");
		editChar(player);
	}

	/* ----------- Методы сбора информации о персонажах и вывод HTML ----------- */
	
	/**
	 * Определяет цель и отдает команду на сбор инфы и вывод HTML
	 */
	private void showCharacterInfo(L2PcInstance activeChar, L2PcInstance player)
	{
		if (player == null)
		{
			L2Object target = activeChar.getTarget();
			if (target!= null && target.isPlayer())
				player = (L2PcInstance) target;
			else
				return;
		}
		else
			activeChar.setTarget(player);
		sendHtml(activeChar, player, "charinfo_menu.htm");
	}
	

	/**
	 * Определение таргета и отправка Html
	 */
	private void editChar(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		if (target == null || !(target.isPlayer()))
			return;
		sendHtml(activeChar, ((L2PcInstance) target), "charedit_menu.htm");
	}
	
	/**
	 * Собирает инфу о чаре и отправляет заданную страницу HTML
	 */
	public static void sendHtml(L2PcInstance activeChar, L2PcInstance player, String filename)
	{
		String ip = "unknown";
		String account = "unknown";
		try
		{
			account = player.getAccountName();
			ip = player.getClient().getHostAddress();
		}
		catch (Exception e)
		{
		}
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/" + filename);
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%clan%", String.valueOf(ClanTable.getInstance().getClan(player.getClanId())));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", player.getTemplate().getClassName());
		adminReply.replace("%currenthp%", String.valueOf((int) player.getStatus().getCurrentHp()));
		adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
		adminReply.replace("%karma%", String.valueOf(player.getKarma()));
		adminReply.replace("%currentmp%", String.valueOf((int) player.getStatus().getCurrentMp()));
		adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
		adminReply.replace("%currentcp%", String.valueOf((int) player.getStatus().getCurrentCp()));
		adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
		adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
		adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
		adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
		adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
		adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
		adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
		adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
		adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
		adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
		adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
		adminReply.replace("%account%", account);
		adminReply.replace("%ip%", ip);
		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}