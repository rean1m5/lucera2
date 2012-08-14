package ru.catssoftware.gameserver.gmaccess;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.util.JarUtils;

 /**
  * Класс управления Gm'ами и хандлерами
  * @author m095
  * @version 1.0
  */

public class gmController
{
	public static Logger							_log			= Logger.getLogger(gmController.class.getName());
	public static Logger							_logGm			= Logger.getLogger("GmAccess");
	private Map<String, gmHandler> 			_commands 	= new HashMap<String, gmHandler>();
	private static gmController					_instance		= null;

	public static gmController getInstance()
	{
		if (_instance == null)
			_instance =  new gmController();
		return _instance;
	}
	
	private gmController()
	{
		_commands.clear();
		try {
			for(String handler : JarUtils.enumClasses("ru.catssoftware.gameserver.gmaccess.handlers")) try {
				Class<?> _handler = Class.forName(handler);
				if(_handler!=null && gmHandler.class.isAssignableFrom(_handler)) {
					Constructor<?> ctor = _handler.getConstructor();
					if(ctor!=null) 
						regCommand((gmHandler)ctor.newInstance());
				}
			} catch(Exception e) {
				continue;
			}
		} catch(Exception e) {
			
		}
		
		_log.info("GmController: Loaded " + _commands.size() + " handlers.");
	}

	/**
	 * Регистрация хандлера
	 * @param handler
	 */
	public void regCommand(gmHandler handler)
	{
		String[] cmd = handler.getCommandList();
		for (String name : cmd)
		{
			if(name.startsWith("admin_"))
				name = name.substring(6);
			_commands.put(name, handler);
		}
	}
	
	/**
	 * Метод проверки существования хандлера
	 * @param command
	 * @return
	 */
	public boolean cmdExists(String command)
	{
		if (_commands.get(command) != null)
			return true;
		return false;
	}

	/**
	 * Запрос использования команды Gm'a
	 * @param player
	 * @param params
	 */
	public void useCommand(L2PcInstance player, String... params)
	{
		/* Для начала простая проверка GM / Player */
		if (!player.isGM())
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_INSUFFICIENT_RIGHT));
			return;
		}
		/* Проверка существования прав для данного гма */
		GmPlayer gm = gmCache.getInstance().getGmPlayer(player.getObjectId());
		if(gm == null)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_INSUFFICIENT_RIGHT));
			return;
		}

		final String command = params[0];
		
		if (command.equals("info"))
		{
			showInfo(player);
			return;
		}

		/* Проверка существования текущей команды гма */
		gmHandler hadler = _commands.get(command);
		if(hadler == null)
		{
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_GM_COMMAND_NOT_EXIST),command));
			return;
		}

		if(!gm.isRoot())
		{
			boolean result = false;
			for(String cmd : gm.getCommands())
			{
				if(command.equals(cmd))
				{
					result = true;
					break;
				}
			}

			if(!result)
			{
				player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_NO_RIGHTS_USE_COMMAND),command));
				return;
			}
		}
		
		if (Config.GM_AUDIT)
		{
			String para = "Used: "+params[0];
			for(int x=1;x<params.length;x++)
				para+=(" "+params[x]);
			_logGm.info("GM: " + player.getName() + " use admin command: " + params[0] + " to target: " + player.getTarget() + ". Full cmd (" + para + ").");		
		}
		hadler.runCommand(player, params);
	}
	
	/**
	 * Вывод доступных команд
	 * @param admin
	 */
	private void showInfo(L2PcInstance admin)
	{
		GmPlayer gm = gmCache.getInstance().getGmPlayer(admin.getObjectId());
		if (gm == null)
			return;
		
		String text = "";
		text += "<html><title>Gm Help</title><body>";
		
		if (gm.isRoot())
		{
			text += "<center><font color=\"LEVEL\">";
			text += "У Вас привелегии Root админа!<br1>";
			text += "Вам доступны все команды!";
			text += "</font></center>";
		}
		else if (gm.isGm())
		{
			int id = 1;
			text += "<center><font color=\"LEVEL\">";
			text += "Список команд:";
			text += "</font></center><br1>";
			for(String cmd : gm.getCommands())
			{
				text += "<font color=\"LEVEL\">№ " + id + ":</font> //" + cmd + "<br1>";
				id++;
			}
		}
		else
		{
			text += "<center><font color=\"LEVEL\">";
			text += "Уровень Ваших прав не определен!<br1>";
			text += "Не могу составить список!";
			text += "</font></center>";
		}
		text += "</body></html>";
		
		NpcHtmlMessage htm = new NpcHtmlMessage(5);
		htm.setHtml(text);
		admin.sendPacket(htm);
	}
	/**
	 * Проверка доступа к команде
	 * @param command
	 * @param objId
	 * @return
	 */
	public boolean hasAccess(String command, int objId)
	{
		/* Проверка существования прав для данного гма */
		GmPlayer gm = gmCache.getInstance().getGmPlayer(objId);
		if(gm == null)
			return false;
		
		/* Проверка существования текущей команды гма */
		if(_commands.get(command) == null)
			return false;

		/* Root админу все доступно */
		if(gm.isRoot())
			return true;
		else
		{
			for(String cmd : gm.getCommands())
				if(command.equals(cmd))
					return true;
		}
		return false;
	}
	
	/**
	 * Проверка и установка прав
	 * @param player
	 * @return
	 */
	public boolean checkPrivs(L2PcInstance player)
	{
		/* Проверка существования гма */
		final GmPlayer gm = gmCache.getInstance().getGmPlayer(player.getObjectId());
		if(gm != null)
		{
			if(gm.getIsTemp()) {
				gmCache.getInstance().removeGM(player.getObjectId());
				return false;
			}
			/* Проверка по IP */
			if (gm.checkIp())
			{
				final String gmAddress = player.getHost();
				final String gmHost = player.getClient().getSocket().getInetAddress().getHostName();
				for (String val : gm.secureIp())
				{
					if (gmAddress.compareTo(val)==0 || gmHost.compareTo(val)==0)
					{
						/* Ставим права */
						player.setGmSetting(gm.isGm(), gm.allowFixRes(), gm.allowAltG(), gm.allowPeaceAtk());
						return true;
					}
				}
			}
			else
			{
				/* Ставим права */
				player.setGmSetting(gm.isGm(), gm.allowFixRes(), gm.allowAltG(), gm.allowPeaceAtk());
				return true;
			}
		}
		return false;
	}
	
	public void checkAdmins()
	{
		int deleted = 0;
		GmPlayer gm = null;
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player == null || !player.isGM())
				continue;
			
			gm = gmCache.getInstance().getGmPlayer(player.getObjectId());
			if (gm == null)
			{
				player.setGmSetting(false, false, false, false);
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_LOST_GM_RIGHTS));
				deleted++;
			}
			else
			{
				player.setGmSetting(gm.isGm(), gm.allowFixRes(), gm.allowAltG(), gm.allowPeaceAtk());
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EARN_GM_RIGHTS));
			}
		}
		
		if(deleted > 0)
			_log.info("GmController: removed " + deleted + " GM player(s)");
	}

	public void showCommands(L2PcInstance pc) {
		GmPlayer gm = gmCache.getInstance().getGmPlayer(pc.getObjectId());
		for(String cmd : _commands.keySet()) {
			String state = "disabled";
			if(!gm.isRoot()) {
				if(gm.getCommands().contains(cmd)) {
					state = "enabled";
				}
			} else 
				state = "enabled";
			_logGm.info("//"+cmd+", "+state+" for "+pc.getName());
		}
		pc.sendMessage("All GM commands send to GM log");
	}
}