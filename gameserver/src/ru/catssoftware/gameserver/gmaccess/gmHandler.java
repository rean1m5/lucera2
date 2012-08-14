package ru.catssoftware.gameserver.gmaccess;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author m095
 * @version 1.0
 * Интерфейс GM хандлеров
 */
	
public abstract class gmHandler
{
	/* Стандартный Logger хандлер, выводит в консоль */
	public static Logger	_log	= Logger.getLogger(gmHandler.class.getName());

	/**
	 * Использование Gm команды
	 * @param admin
	 * @param params
	 */
	public abstract void runCommand(L2PcInstance admin, String... params);
	
	/**
	 * Список команд хандлера
	 * @return
	 */
	public abstract String[] getCommandList();
}
