package ru.catssoftware.gameserver.util;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author durgus
 * rework Visor123
 * update by Visor123 04/19/09
 */
public class FloodProtector
{
	public static enum Protected
	{
		USEITEM(100),
		ROLLDICE(4200),
		CASTSKILL(Config.SKILL_DELAY),
		FIREWORK(4200),
		GLOBAL_CHAT(Config.GLOBAL_CHAT_TIME * 1000),
		TRADE_CHAT(Config.TRADE_CHAT_TIME * 1000),
		ITEMPETSUMMON(1600),
		HEROVOICE(Config.HERO_CHAT_TIME * 1000),
		SOCIAL(4200),
		SUBCLASS(10000),
		DROPITEM(1000), 
		BYPASSTOSERVER(100),
		UNKNOWNPACKET(1000),
		BOT_REPORT(15000),
		VOICE_CMD(10000),
		USER_CMD(5000),
		CL_PACKET(30000),
		HTML_UPDATE(5000),
		USE_POTION(100),
		ENCHANT(Config.ENCHAT_TIME * 1000);

		private final int _reuseDelay;

		private Protected(int reuseDelay)
		{
			_reuseDelay = reuseDelay;
		}

		private int getReuseDelay()
		{
			return _reuseDelay;
		}
	}

	public static void registerNewPlayer(L2PcInstance player)
	{
		if (player != null)
			player.initFloodCount();
	}

	public static boolean tryPerformAction(L2PcInstance player, Protected action)
	{
		if (player == null)
			return false;

		if (player.getFloodCount(action) < System.currentTimeMillis())
		{
			player.setFloodCount(action, System.currentTimeMillis() +  action.getReuseDelay());
			return true;
		}
		return false;
	}

	public static boolean tryPerformAction(L2PcInstance player, Protected action, long delay)
	{
		if (player == null)
			return false;
		if(delay<=0)
			delay = 5;
		if (player.getFloodCount(action) < System.currentTimeMillis())
		{
			player.setFloodCount(action, System.currentTimeMillis() +  delay);
			return true;
		}
		return false;
	}
	
}