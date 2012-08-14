package ru.catssoftware.gameserver.instancemanager;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SetupGauge;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;

public final class AwayManager
{
	private final static Logger				_log			= Logger.getLogger(AwayManager.class.getName());
	private static AwayManager				_instance;
	Map<L2PcInstance, RestoreData>			_awayPlayers;

	public static final AwayManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new AwayManager();
			_log.info("AwayService: Initialized.");
		}
		return _instance;
	}

	private final class RestoreData
	{
		private final String	_originalTitle;
		private final boolean	_sitForced;

		public RestoreData(L2PcInstance activeChar)
		{
			_originalTitle = activeChar.getTitle();
			_sitForced = !activeChar.isSitting();
		}

		@SuppressWarnings("unused")
		public boolean isSitForced()
		{
			return _sitForced;
		}

		public void restore(L2PcInstance activeChar)
		{
			activeChar.setTitle(_originalTitle);
		}
	}

	private AwayManager()
	{
		_awayPlayers = Collections.synchronizedMap(new WeakHashMap<L2PcInstance, RestoreData>());
	}

	public void setAway(L2PcInstance activeChar, String text)
	{
		activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 9));
		// Send html page start
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile("data/html/mods/away/away-start.htm");
		html.replace("%time%", Config.ALT_AWAY_TIMER);
		activeChar.sendPacket(html);
		// Send html page end
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		if (activeChar.isGM())
		{
			SetupGauge sg = new SetupGauge(SetupGauge.BLUE, 1000);
			activeChar.sendPacket(sg);
			activeChar.setIsImmobilized(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new setPlayerAwayTask(activeChar, text), 1000);
		}
		else
		{
			SetupGauge sg = new SetupGauge(SetupGauge.BLUE, Config.ALT_AWAY_TIMER * 1000);
			activeChar.sendPacket(sg);
			activeChar.setIsImmobilized(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new setPlayerAwayTask(activeChar, text), Config.ALT_AWAY_TIMER * 1000);
		}
	}

	public void setBack(L2PcInstance activeChar)
	{
		// Send html page start
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile("data/html/mods/away/away-stop.htm");
		html.replace("%time%", Config.ALT_BACK_TIMER);
		activeChar.sendPacket(html);
		// Send html page end
		if (activeChar.isGM())
		{
			SetupGauge sg = new SetupGauge(SetupGauge.BLUE, 1000);
			activeChar.sendPacket(sg);
			ThreadPoolManager.getInstance().scheduleGeneral(new setPlayerBackTask(activeChar), 1000);
		}
		else
		{
			SetupGauge sg = new SetupGauge(SetupGauge.BLUE, Config.ALT_BACK_TIMER * 1000);
			activeChar.sendPacket(sg);
			ThreadPoolManager.getInstance().scheduleGeneral(new setPlayerBackTask(activeChar), Config.ALT_BACK_TIMER * 1000);
		}
	}

	class setPlayerAwayTask implements Runnable
	{

		private final L2PcInstance	_activeChar;
		private final String		_awayText;

		setPlayerAwayTask(L2PcInstance activeChar, String awayText)
		{
			_activeChar = activeChar;
			_awayText = awayText;
		}

		public void run()
		{
			if (_activeChar == null)
				return;
			if (_activeChar.isAttackingNow() || _activeChar.isCastingNow())
				return;

			_awayPlayers.put(_activeChar, new RestoreData(_activeChar));
			_activeChar.disableAllSkills();
			_activeChar.abortAttack();
			_activeChar.abortCast();
			_activeChar.setTarget(null);
			_activeChar.setIsImmobilized(false);
			_activeChar.sitDown();
 			// Send html page start
			NpcHtmlMessage html = new NpcHtmlMessage(_activeChar.getObjectId());
			html.setFile("data/html/mods/away/away-on.htm");
			_activeChar.sendPacket(html);
			// Send html page end

			if (_awayText.length() <= 1)
				_activeChar.setTitle("*Away*");
			else
				_activeChar.setTitle("Away*" + _awayText + "*");
			_activeChar.broadcastUserInfo();
			_activeChar.setIsParalyzed(true);
			_activeChar.setIsAway(true);
		}
	}

	class setPlayerBackTask implements Runnable
	{
		private final L2PcInstance	_activeChar;

		setPlayerBackTask(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}

		public void run()
		{
			if (_activeChar == null)
				return;
			RestoreData rd = _awayPlayers.get(_activeChar);
			if (rd == null)
				return;
			_activeChar.setIsParalyzed(false);
			_activeChar.enableAllSkills();
			_activeChar.setIsAway(false);
			_activeChar.standUp();
			rd.restore(_activeChar);
			_awayPlayers.remove(_activeChar);
			_activeChar.broadcastUserInfo();
 			// Send html page start
			NpcHtmlMessage html = new NpcHtmlMessage(_activeChar.getObjectId());
			html.setFile("data/html/mods/away/away-off.htm");
			_activeChar.sendPacket(html);
			// Send html page end
		}
	}
}