/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.communitybbs;

import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.communitybbs.handlers.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.HideBoard;
import ru.catssoftware.gameserver.network.serverpackets.ShowBoard;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommunityBoard
{
	private static CommunityBoard	_instance;
	private static Map<String, IBBSHandler > _handlers;
	public static final String HTMBase = "data/html/CommunityBoard/";
	public CommunityBoard()
	{
		_handlers = new FastMap<String, IBBSHandler>();
		registerBBSHandler(new BBSFavorites());
		registerBBSHandler(new GMShop());
		registerBBSHandler(new ProfManager());
		registerBBSHandler(new BBSPages());
		registerBBSHandler(new BBSRegion());
	}

	public static CommunityBoard getInstance()
	{
		if (_instance == null)
			_instance = new CommunityBoard();
		return _instance;
	}

	/**
	 * Обработка команды _bbs<br>
	 * @param activeChar as L2PcInstance - персонаж, для которого открывается BBS<br>
	 * @param command as String - полная команда (с параметрами), передаваемая обработчику
	 */
	public boolean handleCommand(L2PcInstance activeChar, String command) {
		String cmd = command.substring(4); // Стрипаем _bbs
		String params = "";
		if(cmd.contains(" ")) {
			params = cmd.substring(cmd.indexOf(" ")+1);
			cmd = cmd.substring(0,cmd.indexOf(" "));
		}
		IBBSHandler handler = getHandler(cmd);
		if (handler!=null) {
			String result = handler.handleCommand(activeChar, cmd, params);
			if(result==null) {
				activeChar.sendPacket(new HideBoard());
				return true;
			}
			if(result.endsWith(".htm")) {
				result = HtmCache.getInstance().getHtm(HTMBase+result,activeChar);
 				if(result==null) {
 					activeChar.sendPacket(new HideBoard());
 					return true;
 				}
			}
			Pattern p = Pattern.compile("bypass +-h");  // убираем -h
			Matcher m = p.matcher(result);
			if(m.find())
				result = m.replaceAll("bypass");
			separateAndSend(result, activeChar);
			
			return true;
		} 
		return false; 
	}
	/**
	 * Преобразование "общих" (т.е. не написанных специально для BBS) html<br>
	 * При преобразовании:<br>
	 * - удаляется &lt;title&gt; (приводит к падению клиента)<br>
	 * - добавляются два &lt;br&gt; для опускания содержимого<br>
	 * - все bypass [-h] заменяются на bypass -h _bbs для корректной передачи в обработчик<br>
	 * @param result as String - имя htm-файла или его содержимое<br>
	 * @return as String - переработаный результат
	 */
	public static String parseOldFormat(String result, L2PcInstance talker ) {
		if(result == null)
			return null;
		if(result.endsWith(".htm")) {
			result = HtmCache.getInstance().getHtm(HTMBase+result,talker);
		}
		if(result==null)
			result = "<html><body><br><br><center>Команда не реализована.</center></body></html>";
		Pattern p = Pattern.compile("<title>.*?</title>");  // убираем <tile> иначе клиент вылетает 
		Matcher m = p.matcher(result);
		if(m.find())
			result = m.replaceAll("");
		
		result = result.replace("<body>", "<body><br><br>"); // Опустить в центр доски
		p = Pattern.compile("bypass +(-h)? ?");
		m = p.matcher(result);
		if(m.find())
			result = m.replaceAll("bypass -h _bbs");
		return result;
	}
	/**
	 * Получить обработчик по имени команды<br>
	 * @param command as String - имя команды<br>
	 * @return as IBBSHandler - обработчик или null
	 */
	public IBBSHandler getHandler(String command) {
		return _handlers.get(command);
	} 
	/**
	 * Зарегистрировать обработчик<br>
	 * @param handler as IBBSHandler - регистрируемый обработчик
	 */
	public void registerBBSHandler(IBBSHandler handler) {
		for(String s: handler.getCommands())
			_handlers.put(s, handler);
	}
	
	public void handleCommands(L2GameClient client, String command)
	{
		L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
			return;
		switch (Config.COMMUNITY_TYPE)
		{
		default:
		case 0: //disabled
			activeChar.sendPacket(SystemMessageId.CB_OFFLINE);
			break;
		case 1: // old
		case 2: // new
			for(String s : Config.BBS_DISABLED_PAGES)
				if(s.length() > 0 && command.startsWith(s)) {
					separateAndSend(HtmCache.getInstance().getHtm(HTMBase+"disabled.htm", activeChar), activeChar);
					return;
				}
			if(!GMShop.checkMagicCondition(activeChar)) {
				separateAndSend(HtmCache.getInstance().getHtm(HTMBase+"disabled.htm", activeChar), activeChar);
				return;
			}
			
			if (!handleCommand(activeChar, command) )  {
				separateAndSend(HtmCache.getInstance().getHtm(HTMBase+"disabled.htm", activeChar), activeChar);
			}
		} 
	}

	/**
	 * @param client
	 * @param url
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 */
	public static void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.setLastPage(html);
		html = html.replace("%username%", acha.getName());
		html = html.replace("%servername%",Config.SERVER_NAME);
		
		if (html.length() < 4090)
		{
			acha.sendPacket(new ShowBoard(html, "101"));
			acha.sendPacket(new ShowBoard(null, "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < 8180)
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			acha.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < 12270)
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			acha.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
			acha.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
		}
	}

	public void write(L2GameClient client, String...args) {
		
	}
	
}
