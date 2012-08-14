package ru.catssoftware.gameserver.communitybbs;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
/**
 * 
 * @author Azagthtot
 * Базовый интерфейс обработчика BBS
 */
public interface IBBSHandler {
	/**
	 * Список обрабатываемых команд<br>
	 * @return as String
	 */
	public String [] getCommands();

	/**
	 * Обработка команды<br>
	 * @param activeChar as L2PcIntsance - персонаж, для которого формируется доска<br>
	 * @param command as String - команда<br>
	 * @param params as String - параметры<br>
	 * @return as String - или содержимое (html) или имя файла относительно data/html/CommunityBoard
	 */
	public String handleCommand(L2PcInstance activeChar, String command, String params); 
}
