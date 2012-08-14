package ru.catssoftware.gameserver.network.daemons;

import java.io.InputStream;



/**
 * 
 * @author Azagthtot<br>
 * Интерфейс IDeamon предазначен для реализации различных обработчиков<br>
 * топов, вызываемых по расписанию.<br>
 * Дабы не придумывать какие-то механизмы инициализации, используется стандартный<br>
 * механизм сервисов.<br>
 * (см http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#The%20META-INF%20directory<br>
 *  раздел Service Provider)
 * 
 */
public interface IDeamon {
	/**
	 * Инициализация. Нужен для использования как сервис<br>
	 * Если возвращает true то демон может быть использован
	 */
	public boolean load();
	
	/**
	 * Получить URL по которому вычитать инфу<br>
	 * @return as String
	 */
	public String getUrl();
	
	/**
	 *  Имя демона для поля deamon_name в таблице character_votes<br> 
	 * @return as String
	 */
	public String getName();
	
	/**
	 *  Распарсить полученные данные<br>
	 * @param is as InputStream - поток<br>
	 * @throws Exception
	 */
	public void parse(InputStream is) throws Exception;
	
	/**
	 * Выдать игроку награду<br>
	 * @param player as L2PcInstance
	 */
	public void rewardPlayer(String  playerName);
}
