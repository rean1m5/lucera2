package ru.catssoftware.gameserver.datatables;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import javolution.util.FastList;
import javolution.util.FastMap;


public class GmListTable
{
	private static GmListTable				_instance;
	private FastMap<L2PcInstance, Boolean>	_gmList;

	public static GmListTable getInstance()
	{
		if (_instance == null)
			_instance = new GmListTable();
		return _instance;
	}

	private GmListTable()
	{
		_gmList = new FastMap<L2PcInstance, Boolean>().setShared(true);
	}

	/**
	 * Список всех гмов
	 * @param includeHidden
	 * @return
	 */
	public FastList<L2PcInstance> getAllGms(boolean includeHidden)
	{
		FastList<L2PcInstance> tmpGmList = new FastList<L2PcInstance>();
		for (FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
		{
			if (includeHidden || !n.getValue())
				tmpGmList.add(n.getKey());
		}
		return tmpGmList;
	}

	/**
	 * Список всех гмов по имени
	 * @param includeHidden
	 * @return
	 */
	public FastList<String> getAllGmNames(boolean includeHidden)
	{
		FastList<String> tmpGmList = new FastList<String>();
		for (FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
		{
			if (!n.getValue())
				tmpGmList.add(n.getKey().getName());
			else if (includeHidden)
				tmpGmList.add(n.getKey().getName() + " (invis)");
		}
		return tmpGmList;
	}

	/**
	 * Добавление гма к списку
	 * @param player
	 * @param hidden
	 */
	public void addGm(L2PcInstance player, boolean hidden)
	{
		_gmList.put(player, hidden);
	}

	/**
	 * Удаление гма из списка
	 * @param player
	 */
	public void deleteGm(L2PcInstance player)
	{
		_gmList.remove(player);
	}

	/**
	 * Установка значения гму, vis/invis
	 * @param player
	 * @param val
	 */
	public void setGmOption(L2PcInstance player, boolean val)
	{
		FastMap.Entry<L2PcInstance, Boolean> gm = _gmList.getEntry(player);
		if (gm != null)
			gm.setValue(val);
	}

	/**
	 * Проверка Gm'a
	 * @param includeHidden
	 * @return
	 */
	public boolean isGmOnline(boolean includeHidden)
	{
		for (boolean b : _gmList.values())
		{
			if (includeHidden || !b)
				return true;
		}
		return false;
	}

	/**
	 * Отправка списка гмов игроку
	 * @param player
	 */
	public void sendListToPlayer(L2PcInstance player)
	{
		if (!isGmOnline(player.isGM()))
			player.sendPacket(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW);
		else
		{
			player.sendPacket(SystemMessageId.GM_LIST);
			for (String name : getAllGmNames(player.isGM()))
				player.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString(name));
			player.sendPacket(SystemMessageId.GM_LIST);
		}
	}

	/**
	 * Анонс пакета всем гмам
	 * @param packet
	 */
	public static void broadcastToGMs(L2GameServerPacket packet)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
		{
			if (gm != null)
				gm.sendPacket(packet);
		}		
	}

	/**
	 * Анонс сообщения всем гмам
	 * @param message
	 */
	public static void broadcastMessageToGMs(String message)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
		{
			if (gm != null)
				gm.sendPacket(SystemMessage.sendString(message));
		}
	}
}