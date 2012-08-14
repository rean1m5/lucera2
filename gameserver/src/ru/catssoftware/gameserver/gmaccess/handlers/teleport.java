package ru.catssoftware.gameserver.gmaccess.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.BoatManager;
import ru.catssoftware.gameserver.instancemanager.RaidBossSpawnManager;
import ru.catssoftware.gameserver.instancemanager.FourSepulchersManager.FourSepulchersMausoleum;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BossLair;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2MinionInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RaidBossInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2BossZone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import javolution.text.TextBuilder;


public class teleport extends gmHandler
{
	private static final String[] commands =
	{ 
		"recall_offline",
		"telemode",
		"show_moves",
		"show_moves_other",
		"show_teleport",
		"recall_npc",
		"teleport_to_character",
		"walk",
		"move_to",
		"teleportto",
		"recall",
		"recall_all",
		"tele",
		"go",
		"tt",
		"recallparty",
		"recallclan",
		"recallally",
		"instant_move",
		"sendhome",
		"rblist",
		"startroom"
		
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		
		String command = params[0];

		if (command.equals("rblist")) {
			String htm= HtmCache.getInstance().getHtm("data/html/admin/tele/raid/header.htm", admin);
			if(params[1].equals("grand")) {
				/*for( L2Spawn spawn : GrandBossSpawnManager.getInstance().getSpawns().values()) {
					if(spawn.getTemplate().getType().equalsIgnoreCase("L2GrandBoss")) {
						htm+="<a action=\"bypass -h admin_move_to "+spawn.getLocx()+" "+spawn.getLocy()+" "+spawn.getLocz()+"\">"+spawn.getTemplate().getName()+" lvl "+spawn.getTemplate().getLevel()+" ("+
						GrandBossSpawnManager.getInstance().getRaidBossStatusId(spawn.getTemplate().getIdTemplate())+
						")</a><br1>";
					}
				}*/
				for(BossLair lair : BossLair.getLairs()) {
						if(lair._bossSpawn!=null) {
							int x = lair._bossSpawn.getLocx(), y = lair._bossSpawn.getLocy(),z = lair._bossSpawn.getLocz();
							if(lair._bossSpawn.getLastSpawn()!=null) {
								x = lair._bossSpawn.getLastSpawn().getX();
								y = lair._bossSpawn.getLastSpawn().getY();
								z = lair._bossSpawn.getLastSpawn().getZ();
							}
							htm+="<a action=\"bypass -h admin_move_to "+x+" "+y+" "+z+"\">"+lair._bossSpawn.getTemplate().getName()+" lvl "+lair._bossSpawn.getTemplate().getLevel()+" ("+
							lair.getState()+")</a><br1>";
						}
				}
				
				htm+="</font></body></html>";
				NpcHtmlMessage msg = new NpcHtmlMessage(5);
				msg.setHtml(htm);
				msg.replace("%raidinfo%", "Grand Boss");
				admin.sendPacket(msg);
			}
			else {
				int level = Integer.parseInt(params[1]);
				for( L2Spawn spawn : RaidBossSpawnManager.getInstance().getSpawns().values()) {
					if(spawn.getTemplate().getLevel()>=level && spawn.getTemplate().getLevel() < level+10) {
						htm+="<a action=\"bypass -h admin_move_to "+spawn.getLocx()+" "+spawn.getLocy()+" "+spawn.getLocz()+"\">"+spawn.getTemplate().getName()+" lvl "+spawn.getTemplate().getLevel()+" ("+
						RaidBossSpawnManager.getInstance().getRaidBossStatusId(spawn.getTemplate().getIdTemplate())+
						")</a><br1>";
					}
				}
				htm+="</font></body></html>";
				NpcHtmlMessage msg = new NpcHtmlMessage(5);
				msg.setHtml(htm);
				msg.replace("%raidinfo%", "Raid Boss " + level + "-" + (level + 9));
				admin.sendPacket(msg);
			}
		}
		else if (command.equals("sendhome"))
		{
			if (params.length > 1)
				handleSendhome(admin, params[1]);
			else
				handleSendhome(admin, null);
		}
		else if (command.equals("recallparty"))
		{
			L2PcInstance player = null;
			if (params.length > 1)
			{
				player = L2World.getInstance().getPlayer(params[1]);
			}
			if (player == null)
			{
				L2Object obj = admin.getTarget();
				if (obj == null)
					obj = admin;
				
				if (obj instanceof L2PcInstance)
					player = (L2PcInstance)obj;
			}

			if (player != null)
			{
				if (player.getParty() == null)
				{
					admin.sendMessage("Игрок не в группе");
					return;
				}
				int x = admin.getX(), y = admin.getY(), z = admin.getZ(), count = 0;
				L2Party party = player.getParty();
				if (party == null)
				{
					admin.sendMessage("Группа игрока " + player.getName() + " не найдена");
					return;
				}
				for (L2PcInstance pc : party.getPartyMembers())
				{
					if (pc == null || pc.isInJail() || pc.isOfflineTrade())
						continue;
					
					pc.teleToLocation(x,y,z);
					pc.sendMessage("Ваша группа призвана Gm'ом");
					count++;
				}
				if (count > 0)
					admin.sendMessage("Группа игрока "+player.getName()+" призвана к Вам");
				else
					admin.sendMessage("Игроки в группе не обнаружены");
			}
			else
			{
				admin.sendMessage("Укажите правильную цель");
				return;
			}
		}
		else if (command.equals("recallclan"))
		{
			L2PcInstance player = null;
			if (params.length > 1)
			{
				player = L2World.getInstance().getPlayer(params[1]);
			}
			if (player == null)
			{
				L2Object obj = admin.getTarget();
				if (obj == null)
					obj = admin;
				
				if (obj instanceof L2PcInstance)
					player = (L2PcInstance)obj;
			}

			if (player != null)
			{
				if (player.getClan() == null)
				{
					admin.sendMessage("Игрок не в клане");
					return;
				}
				int x = admin.getX(), y = admin.getY(), z = admin.getZ(), count = 0;
				L2Clan clan = player.getClan();
				if (clan == null)
				{
					admin.sendMessage("Клан игрока " + player.getName() + " не найден");
					return;
				}
				for (L2PcInstance pc : clan.getOnlineMembersList())
				{
					if (pc == null || pc.isInJail() || pc.isOfflineTrade())
						continue;
					
					pc.teleToLocation(x,y,z);
					pc.sendMessage("Ваш клан призван Gm'ом");
					count++;
				}
				if (count > 0)
					admin.sendMessage("Клан игрока "+player.getName()+" призван к Вам");
				else
					admin.sendMessage("Игроки в клане не обнаружены");
			}
			else
			{
				admin.sendMessage("Укажите правильную цель");
				return;
			}
		}
		else if (command.equals("recallally"))
		{
			L2PcInstance player = null;
			if (params.length > 1)
			{
				player = L2World.getInstance().getPlayer(params[1]);
			}
			if (player == null)
			{
				L2Object obj = admin.getTarget();
				if (obj == null)
					obj = admin;
				
				if (obj instanceof L2PcInstance)
					player = (L2PcInstance)obj;
			}

			if (player != null)
			{
				if (player.getClan() == null || player.getClan().getAllyId() == 0)
				{
					admin.sendMessage("Игрок не в альянсе");
					return;
				}
				int x = admin.getX(), y = admin.getY(), z = admin.getZ(), count = 0;
				L2Clan clan = player.getClan();
				if (clan == null)
				{
					admin.sendMessage("Альянс игрока " + player.getName() + " не найден");
					return;
				}
				for (L2PcInstance pc : clan.getOnlineAllyMembers())
				{
					if (pc == null || pc.isInJail() || pc.isOfflineTrade())
						continue;
					
					pc.teleToLocation(x,y,z);
					pc.sendMessage("Ваш Альянс призван Gm'ом");
					count++;
				}
				if (count > 0)
					admin.sendMessage("Альянс игрока "+player.getName()+" призван к Вам");
				else
					admin.sendMessage("Игроки в альянсе не обнаружены");
			}
			else
			{
				admin.sendMessage("Укажите правильную цель");
				return;
			}
		}
		else if (command.equals("recall_offline"))
		{
			try
			{
				if (params.length > 1)
					changeCharacterPosition(admin, params[1]);
				else
					admin.sendMessage("Используйте: //recall_offline [name]");
			}
			catch (Exception e)
			{
			}
			return;
		}
		else if (command.equals("telemode"))
		{
			if (params.length > 1)
			{
				String cmd = params[1];
				if (cmd.equals("demon"))
					admin.setTeleMode(1);
				else if (cmd.equals("norm"))
					admin.setTeleMode(0);
			}
			showTeleportWindow(admin);
			return;
		}
		else if (command.equals("startroom")) {
			L2BossZone z = (L2BossZone)admin.getZone("Boss");
			if(z!=null && z._lair instanceof FourSepulchersMausoleum) {
				((FourSepulchersMausoleum)z._lair).startRoom(Integer.parseInt(params[1]));
				return;
			}
			admin.sendMessage("Not in Four Sepluchers");
				
		}
		else if (command.equals("show_moves"))
		{
			methods.showTeleMenuPage(admin, "teleports.htm");
			return;
		}
		else if (command.equals("show_moves_other"))
		{
			methods.showTeleMenuPage(admin, "areas/areas.html");
			return;
		}
		else if (command.equals("show_teleport"))
		{
			showTeleportCharWindow(admin);
			return;
		}
		else if (command.equals("recall_npc"))
		{
			recallNPC(admin);
			return;
		}
		else if (command.equals("teleport_to_character"))
		{
			if (admin.getTarget() instanceof L2PcInstance)
				teleportToCharacter(admin, ((L2PcInstance)admin.getTarget()));
			else
				admin.sendMessage("Цель не является игроком.");
			return;
		}
		else if (command.equals("walk"))
		{
			try
			{
				if (params.length >= 4)
				{
					L2CharPosition pos = new L2CharPosition(Integer.parseInt(params[1]), Integer.parseInt(params[2]), Integer.parseInt(params[3]), 0);
					admin.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
				}
				else
					admin.sendMessage("Используйте: //walk [x] [y] [z]");
			}
			catch (Exception e)
			{
			}
			return;
		}
		else if (command.equals("instant_move"))
		{
			if (admin.getTeleMode() > 0)
				admin.setTeleMode(0);
			else	
				admin.setTeleMode(1);

			admin.sendMessage("Режим перемещения изменен");
			return;
		}
		else if (command.equals("move_to"))
		{
			try
			{
				if (params.length > 2)
				{
					String val = params[1];
					if (val.equals("boat"))
						teleportToBoat(admin, Integer.parseInt(params[2]));
					else
					{
						if (params.length >= 4)
							teleportTo(admin, Integer.parseInt(params[1]), Integer.parseInt(params[2]), Integer.parseInt(params[3]));
						else
						{
							admin.sendMessage("Используйте: //move_to [x] [y] [z]");
							methods.showTeleMenuPage(admin, "teleports.htm");
						}
					}
				}
				else
				{
					admin.sendMessage("Используйте: //move_to [x] [y] [z]");
					methods.showTeleMenuPage(admin, "teleports.htm");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				admin.sendMessage("Используйте: //move_to [x] [y] [z]");
				methods.showTeleMenuPage(admin, "teleports.htm");
			}
			catch (NoSuchElementException nsee)
			{
				admin.sendMessage("Используйте: //move_to [x] [y] [z]");
				methods.showTeleMenuPage(admin, "teleports.htm");
			}
			catch (NumberFormatException nfe)
			{
				admin.sendMessage("Используйте: //move_to [x] [y] [z]");
				methods.showTeleMenuPage(admin, "teleports.htm");
			}
			return;
		}
		else if (command.equals("teleportto") || command.equals("tt"))
		{
			try
			{
				if (params.length > 1)
				{
					L2PcInstance player = L2World.getInstance().getPlayer(params[1]);
					if (player != null)
						teleportToCharacter(admin, player);
					else
						admin.sendMessage("Игрок не найден на сервере");
				}
				else
					admin.sendMessage("Используйте: //teleportto [name]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				admin.sendMessage("Используйте: //teleportto [name]");
			}
			return;
		}
		else if (command.equals("recall"))
		{
			if (params.length < 1)
			{
				admin.sendMessage("Используйте: //recall [name]");
				return;
			}
			
			L2PcInstance player = L2World.getInstance().getPlayer(params[1]);
			if (player != null)
			{
				try
				{
					if (player.isInJail())
					{
						admin.sendMessage("Игрок " + player.getName() + " в тюрьме.");
						return;
					}
					teleportCharacter(player, admin.getX(), admin.getY(), admin.getZ());
					admin.sendMessage("Игрок " + player.getName() + " призван к Вам.");
				}
				catch (StringIndexOutOfBoundsException e)
				{
					admin.sendMessage("Используйте: //recall [name]");
					return;
				}
			}
			else
			{
				try
				{
					String param = params[1];
					if (changeCharacterPosition(admin, param))
						admin.sendMessage("Игрок " + param + " не в игре. Обновлено в базе.");
					else
						admin.sendMessage("Игрок " + param + " не в игре. Не найден в базе.");
				}
				catch (Exception e)
				{
				}
			}
			return;
		}
		else if (command.equals("recall_all"))
		{
			int count = 0;
			int x = admin.getX();
			int y = admin.getY();
			int z = admin.getZ();
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (player == null)
					continue;
				if (player.isInJail())
					continue;
				if (player == admin)
					continue;
				teleportCharacter(player, x, y, z);
				count++;
			}
			admin.sendMessage("Призвано " + count + " игроков.");
			return;
		}
		else if (command.equals("tele"))
		{
			showTeleportWindow(admin);
			return;
		}
		else if (command.startsWith("go"))
		{
			int intVal = 150;
			int x = admin.getX();
			int y = admin.getY();
			int z = admin.getZ();

			try
			{
				if (params.length > 1)
				{
					String dir = params[1];
					intVal = params.length > 2 ? Integer.parseInt(params[2]) : 0;

					if (dir.equals("east"))
						x += intVal;
					else if (dir.equals("west"))
						x -= intVal;
					else if (dir.equals("north"))
						y -= intVal;
					else if (dir.equals("south"))
						y += intVal;
					else if (dir.equals("up"))
						z += intVal;
					else if (dir.equals("down"))
						z -= intVal;
					admin.teleToLocation(x, y, z, false);
					showTeleportWindow(admin);
				}
				else
					admin.sendMessage("Используйте: //go<north|south|east|west|up|down> [offset]");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //go<north|south|east|west|up|down> [offset]");
			}
			return;
		}
	}

	private void teleportTo(L2PcInstance activeChar, int x, int y, int z)
	{
		if (activeChar != null)
		{
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			activeChar.setInstanceId(0);
			activeChar.teleToLocation(x, y, z, false);
			activeChar.sendMessage("Вы перемещены по координатам: " + x + " " + y + " " + z);
		}
	}

	private void teleportCharacter(L2PcInstance player, int x, int y, int z)
	{
		if (player != null)
		{
			player.sendMessage("Вы перемещены администрацией.");
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			player.teleToLocation(x, y, z);
		}
	}

	private void teleportToCharacter(L2PcInstance activeChar, L2PcInstance target)
	{
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		if (target.getInstanceId() != activeChar.getInstanceId())
		{
			activeChar.setInstanceId(target.getInstanceId());
			activeChar.sendMessage("Вход в инстанс " + target.getInstanceId()+".");
		}
		if (target.getObjectId() == activeChar.getObjectId())
		{
			target.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			activeChar.teleToLocation(target.getX(), target.getY(), target.getZ());
			activeChar.sendMessage("Вы перемещены к персонажу " + target.getName() + ".");
		}
	}

	private void teleportToBoat(L2PcInstance activeChar, int id)
	{
		try
		{
			L2BoatInstance boat=BoatManager.getInstance().getBoat(id);
			if (boat!=null)
			{
				activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				activeChar.setInstanceId(0);
				activeChar.teleToLocation(boat.getX(), boat.getY(), boat.getZ()+500, false);
			}
		}
		catch (NoSuchElementException nsee)
		{
			activeChar.sendMessage("Неверные коордианты.");
		}
	}

	private void showTeleportWindow(L2PcInstance activeChar)
	{
		methods.showSubMenuPage(activeChar, "move_menu.htm");
	}

	private void showTeleportCharWindow(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><title>Переещение персонажа</title>");
		replyMSG.append("<body>");
		replyMSG.append("The character you will teleport is " + player.getName() + ".");
		replyMSG.append("<br>");
		replyMSG.append("Coordinate x");
		replyMSG.append("<edit var=\"char_cord_x\" width=110>");
		replyMSG.append("Coordinate y");
		replyMSG.append("<edit var=\"char_cord_y\" width=110>");
		replyMSG.append("Coordinate z");
		replyMSG.append("<edit var=\"char_cord_z\" width=110>");
		replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		replyMSG.append("<button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=115 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void recallNPC(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if (obj instanceof L2NpcInstance && !(obj instanceof L2MinionInstance) && !(obj instanceof L2RaidBossInstance) && !(obj instanceof L2GrandBossInstance))
		{
			L2NpcInstance target = (L2NpcInstance) obj;

			int monsterTemplate = target.getTemplate().getNpcId();
			L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
			if (template1 == null)
			{
				activeChar.sendMessage("Неверный монстр.");
				return;
			}

			L2Spawn spawn = target.getSpawn();

			if (spawn == null)
			{
				activeChar.sendMessage("Неверные координаты спавна.");
				return;
			}

			target.decayMe();
			spawn.setLocx(activeChar.getX());
			spawn.setLocy(activeChar.getY());
			spawn.setLocz(activeChar.getZ());
			spawn.setHeading(activeChar.getHeading());
			spawn.respawnNpc(target);
			spawn.setInstanceId(activeChar.getInstanceId());
			SpawnTable.getInstance().updateSpawn(spawn);
		}
		else if (obj instanceof L2RaidBossInstance)
		{
			L2RaidBossInstance target = (L2RaidBossInstance) obj;
			RaidBossSpawnManager.getInstance().updateSpawn(target.getNpcId(), activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading());
		}
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}

	private boolean changeCharacterPosition(L2PcInstance activeChar, String name)
	{
		boolean result = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=? WHERE char_name=?");
			statement.setInt(1, activeChar.getX());
			statement.setString(2, name);
			statement.execute();
			statement = con.prepareStatement("UPDATE characters SET y=? WHERE char_name=?");
			statement.setInt(1, activeChar.getY());
			statement.setString(2, name);
			statement.execute();
			statement = con.prepareStatement("UPDATE characters SET z=? WHERE char_name=?");
			statement.setInt(1, activeChar.getZ());
			statement.setString(2, name);
			if (statement.execute())
				result = true;
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private void handleSendhome(L2PcInstance activeChar, String player)
	{
		L2Object  obj = null;
		
		if (player == null)
			obj = activeChar;
		else
			obj = L2World.getInstance().getPlayer(player);

		if (obj != null && obj instanceof L2PcInstance)
			((L2PcInstance)obj).teleToLocation(TeleportWhereType.Town);
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}