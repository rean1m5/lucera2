package ru.catssoftware.gameserver;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.script.DateRange;
import ru.catssoftware.gameserver.taskmanager.AutoAnnounceTaskManager;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class Announcements
{
	private final static Logger		_log				= Logger.getLogger(Announcements.class.getName());

	private static Announcements	_instance;
	private List<String>			_announcements		= new FastList<String>();
	private List<List<Object>>		_eventAnnouncements	= new FastList<List<Object>>();

	public Announcements()
	{
		loadAnnouncements();
		if (Config.LOAD_AUTOANNOUNCE_AT_STARTUP)
			AutoAnnounceTaskManager.getInstance();
		else
			_log.info("AnnounceManager: Auto announce disabled");
	}

	public static Announcements getInstance()
	{
		if (_instance == null)
			_instance = new Announcements();
		return _instance;
	}

	public void loadAnnouncements()
	{
		_announcements.clear();
		File file = new File(Config.DATAPACK_ROOT, "data/announcements.txt");
		if (file.exists())
			readFromDisk(file);
		else
			_log.info("AnnounceManager: File is not exist");
	}

	public void showAnnouncements()
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player != null)
				showAnnouncements(player);
		}
	}

	public void showAnnouncements(L2PcInstance activeChar)
	{
		for (int i = 0; i < _announcements.size(); i++)
		{
			CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Announce, activeChar.getName(), _announcements.get(i).replace("%name%", activeChar.getName()));
			activeChar.sendPacket(cs);
		}
		for (int i = 0; i < _eventAnnouncements.size(); i++)
		{
			List<Object> entry = _eventAnnouncements.get(i);

			DateRange validDateRange = (DateRange) entry.get(0);
			String[] msg = (String[]) entry.get(1);
			Date currentDate = new Date();

			if (validDateRange.isValid() && validDateRange.isWithinRange(currentDate))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1);
				for (String element : msg)
					sm.addString(element);

				activeChar.sendPacket(sm);
			}
		}
	}

	public void addEventAnnouncement(DateRange validDateRange, String[] msg)
	{
		FastList<Object> entry = new FastList<Object>();
		entry.add(validDateRange);
		entry.add(msg);
		_eventAnnouncements.add(entry);
	}

	public void listAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtm("data/html/admin/menus/submenus/announce_menu.htm",activeChar);
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		String temp = "";
		TextBuilder replyMSG = new TextBuilder("<br>");
		for (int i = 0; i < _announcements.size(); i++)
		{
			temp = _announcements.get(i).length() > 27 ? (_announcements.get(i).substring(0, 26) + "...") : _announcements.get(i);
			replyMSG.append("<table width=260><tr><td width=220>" + temp + "</td><td width=40>");
			replyMSG.append("<button value=\"удалить\" action=\"bypass -h admin_announce_del " + i + "\" width=60 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr></table>");
		}
		String html = replyMSG.toString();
		adminReply.replace("%announces%", (html.length() > 30 ? html : "<br><center>Анонсов не найдено</center>"));
		activeChar.sendPacket(adminReply);
	}

	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk();
	}

	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk();
	}

	private void readFromDisk(File file)
	{
		FileInputStream fStream=null;
		LineNumberReader lnr = null;
		try
		{
			int i = 0;
			String line = null;
			fStream=new FileInputStream(file);
			lnr = new LineNumberReader(new InputStreamReader(fStream, "UTF-8"));
			while ((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if (st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_announcements.add(announcement);
					i++;
				}
			}
			_log.info("AnnounceManager: Loaded " + i + " announce");
		}
		catch (IOException e1)
		{
			_log.fatal("Error reading announcements", e1);
		}
		finally
		{
			IOUtils.closeQuietly(lnr);
			IOUtils.closeQuietly(fStream);
		}
	}

	private void saveToDisk()
	{
		File file = new File(Config.DATAPACK_ROOT, "data/announcements.txt");
		FileWriter save = null;

		try
		{
			save = new FileWriter(file);
			for (int i = 0; i < _announcements.size(); i++)
			{
				save.write(_announcements.get(i));
				save.write("\r\n");
			}
		}
		catch (IOException e)
		{
			_log.warn("saving the announcements file has failed: " + e);
		}
		finally
		{
			try
			{
				if (save != null)
					save.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public void announceToClan(L2Clan clan, String text) {
		CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Announce, "", text);
		clan.broadcastToOnlineMembers(cs);
	}
	public void announceToAlly(L2Clan clan, String text) {
		CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Announce, "", text);
		clan.broadcastToOnlineAllyMembers(cs);
	}
	
	public void announceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Announce, "", text);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(cs);
	}

	public void criticalAnnounceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Critical_Announce, "", text);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(cs);
	}

	public void announceToAll(L2GameServerPacket gsp)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(gsp);
	}
	
	public void announceToAll(SystemMessageId sm)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(sm);
	}

	public void announceToInstance(L2GameServerPacket gsp, int instanceId)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player.getInstanceId() == instanceId)
				player.sendPacket(gsp);
		}
	}

	public void handleAnnounce(String command)
	{
		try
		{
			announceToAll(command);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}

	public void handleCriticalAnnounce(String command)
	{
		try
		{
			criticalAnnounceToAll(command);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}

	public void announceToPlayers(String message)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player != null)
				player.sendMessage(message);
		}
	}
}