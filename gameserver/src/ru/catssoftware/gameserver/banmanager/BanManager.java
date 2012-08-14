package ru.catssoftware.gameserver.banmanager;

import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.LoginServerThread;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.model.restriction.RestrictionBindClassException;
import ru.catssoftware.gameserver.network.Disconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * @author m095
 * @version 1.0
 * 
 * Данный класс содержит основные методы ограничения игроков<br>
 * В данный момент можно:<br>
 * - блокировать/разблокировать чат игрока<br>
 * - блокировать/разблокировать все чаты всем игрокам<br>
 * - блокировать/разблокировать аккаунт игрока<br>
 * - отправлять игрока тюрьму и освобождать
 */

public class BanManager
{
	public static Logger				_logBan					= Logger.getLogger("ban");
	private static BanManager		_instance				= null;
	
	public static BanManager getInstance()
	{
		if (_instance == null)
			_instance = new BanManager();
		return _instance;
	}

	/**
	 * Метод блокировки персонажа
	 * Блокирует онлайн и оффлайн игроков
	 * @param admin
	 * @param target
	 * @return
	 */
	public boolean banChar(L2PcInstance admin, String target)
	{
		if (admin == null || target == null)
			return false;

		L2PcInstance player = L2World.getInstance().getPlayer(target);
		if (player != null)
		{
			if (!player.banChar())
				return false;
		}
		else
		{
			if (!banPlayer(target, true))
				return false;
		}
		/* Logging */
		if (Config.BAN_CHAR_LOG)
			_logBan.info("Gm " + admin.getName()+ " заблокировал персонажа [" + target + "].");
		/* Announce */
		if (Config.ANNOUNCE_BAN_CHAR)
			announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_BLOCKED),target));
		/* Admin notify */
		admin.sendMessage(String.format(Message.getMessage(admin, Message.MessageId.MSG_CHAR_SUCCESS_BAN),target));
		return true;
	}
	
	/**
	 * Метод разблокировки персонажа
	 * Снимает блокировку с онлайн и оффлайн игроков
	 * @param admin
	 * @param target
	 * @return
	 */
	public boolean unBanChar(L2PcInstance admin, String target)
	{
		if (admin == null || target == null)
			return false;
		if (!banPlayer(target, false))
			return false;
		/* Logging */
		if (Config.BAN_CHAR_LOG)
			_logBan.info("Gm " + admin.getName()+ " разблокировал персонажа [" + target + "].");
		/* Announce */
		if (Config.ANNOUNCE_UNBAN_CHAR)
			announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_UN_BLOCKED),target));
		/* Admin notify */
		admin.sendMessage(String.format(Message.getMessage(admin, Message.MessageId.MSG_CHAR_SUCCESS_UNBAN),target));
		return true;
	}
	
	/**
	 * Назначить на аккаунт права GM'a
	 * @param admin
	 * @param target
	 * @return
	 */
	public boolean gmAccess(L2PcInstance admin, String target)
	{
		if (admin == null || target == null)
			return false;

		try
		{
			LoginServerThread.getInstance().sendAccessLevel(target, 1);
			/* Admin notify */
			admin.sendMessage(String.format(Message.getMessage(admin, Message.MessageId.MSG_ACC_RECIVE_GM_ACCESS),target));
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Метод блокировки чата для цензор системы
	 * @param censor
	 * @param player
	 * @param time
	 * @return
	 */
	public boolean banChat(String censor, L2PcInstance player, int time, String type)
	{
		if (player == null)
			return false;
		
		try
		{
			ObjectRestrictions.getInstance().addRestriction(player, AvailableRestriction.PlayerChat);
			ObjectRestrictions.getInstance().timedRemoveRestriction(player.getObjectId(), AvailableRestriction.PlayerChat, time * 60000, "Блокировка чата снята.");
		}
		catch (RestrictionBindClassException e)
		{
			return false;
		}
		
                if (Config.BAN_CHAT_LOG)
                    _logBan.info("Цензор [" + censor + "] забанил чат игроку [" + player.getName() + "] на " + time + " минут, причина: " + type + ".");

                if (Config.ANNOUNCE_BAN_CHAT){
                    announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_CHAT_BLOCK),player.getName(),time));
                    announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_BAN_REASON),type));
                }
		return true;
	}

	/**
	 * Метод блокировки чата персонажу, отключение блокировки по таймеру
	 * @param activeChar
	 * @param player
	 * @param reason
	 * @param time
	 * @return
	 */
	public boolean banChat(L2PcInstance activeChar, L2PcInstance player, String reason, int time)
	{
		if (activeChar == null || player == null || activeChar == player)
			return false;
		String message = null;
		if (time > 0)
		{
			message = String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAT_BLOCK_WITH_REASON),player.getName(),time,reason);
			try
			{
				ObjectRestrictions.getInstance().addRestriction(player, AvailableRestriction.PlayerChat);
				ObjectRestrictions.getInstance().timedRemoveRestriction(player.getObjectId(), AvailableRestriction.PlayerChat, time * 60000, player.getName() + " , Вам снята блокировка чата");
			}
			catch (RestrictionBindClassException e)
			{
				e.printStackTrace();
				return false;
			}
		}
		else
		{
			message = String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAT_BLOCK_FOREVER_WITH_REASON),player.getName(),reason);
			try
			{
				ObjectRestrictions.getInstance().addRestriction(player, AvailableRestriction.PlayerChat);
			}
			catch (RestrictionBindClassException e)
			{
				e.printStackTrace();
				return false;
			}
		}
		if (Config.BAN_CHAT_LOG)
			_logBan.info("Модератор [" + activeChar.getName() + "] забанил чат игроку [" + player.getName() + (time > 0 ? "] на " + time + " минут." : "] бессрочно!"));
		if (Config.ANNOUNCE_BAN_CHAT)
			announce(message);
		String inf = time > 0 ? " for " + time + " minutes." : " forever!";
		activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CHAT_BLOCK_FOR),player.getName() + inf));
		player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_YOUR_CHAT_BLOCK_FOR),time));
		return true;
	}
	
	/**
	 * Метод разблокировки чата персонажу
	 * @param activeChar
	 * @param player
	 * @return
	 */
	public boolean unBanChat(L2PcInstance activeChar, L2PcInstance player)
	{
		if (activeChar == null || player == null)
			return false;

		try
		{
			ObjectRestrictions.getInstance().removeRestriction(player, AvailableRestriction.PlayerChat);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (Config.BAN_CHAT_LOG)
			_logBan.info("Модератор [" + activeChar.getName() + "] разбанил чат игроку [" + player.getName() + "].");
		if (Config.ANNOUNCE_UNBAN_CHAT)
			announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_CHAT_UNBLOCK),player.getName()));
		activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CHAT_UNBLOCK),player.getName()));
		player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_YOUR_CHAT_UNBLOCK));
		return true;
	}

	/**
	 * Метод блокировки всех игровых чатов, касается всех кроме гмов
	 * @param activeChar
	 * @return
	 */
	public boolean banChatAll(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;

		try
		{
			ObjectRestrictions.getInstance().addGlobalRestriction(AvailableRestriction.GlobalPlayerChat);
			ObjectRestrictions.getInstance().timedRemoveRestriction(0, AvailableRestriction.GlobalPlayerChat, Config.GLOBAL_BAN_CHAT_TIME * 60000, null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (Config.BAN_CHAT_LOG)
			_logBan.info("Модератор [" + activeChar.getName() + "] установил глобальную блокировку чата на " + Config.GLOBAL_BAN_CHAT_TIME + " минут.");
                if (Config.ANNOUNCE_BAN_CHAT)
                    announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_ADMIN_BLOCK_ALL_CHAT),Config.GLOBAL_BAN_CHAT_TIME));
		activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_ALL_CHAT_BLOCK),Config.GLOBAL_BAN_CHAT_TIME));
		return true;
	}

	/**
	 * Метод отключения глобальной блокировки чата
	 * @param activeChar
	 * @return
	 */
	public boolean unBanChatAll(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;

		try
		{
			ObjectRestrictions.getInstance().removeGlobalRestriction(AvailableRestriction.GlobalPlayerChat);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (Config.BAN_CHAT_LOG)
			_logBan.info("Модератор [" + activeChar.getName() + "] снял глобальную блокировку чата.");
                if (Config.ANNOUNCE_UNBAN_CHAT)
                    announce(Message.getMessage(null, Message.MessageId.ANNOUNCE_ADMIN_UNBLOCK_ALL_CHAT));
		activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_ALL_CHAT_UNBLOCK));
		return true;
	}

	/**
	 * Метод блокировки аккаунта персонажа
	 * @param activeChar
	 * @param player
	 * @return
	 */
	public boolean banAccount(L2PcInstance activeChar, L2PcInstance player)
	{
		if ( player == null)
			return false;
		
		if (activeChar == player)
			return false;
		
		String plName = player.getName();
		String acName = player.getAccountName();

		player.setAccountAccesslevel(-100);
		try
		{
			// Фикс кика из игры для Оффлайн Трэйдеров
			if (player.isOfflineTrade())
			{
				player.setOfflineTrade(false);
				player.standUp();
			}
			new Disconnection(player).defaultSequence(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if(activeChar!=null) {
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_BAN_ACC_BREAK_RULE),acName));
			if (Config.BAN_ACCOUNT_LOG)
				_logBan.info("Модератор [" + activeChar.getName() + "] забанил аккаунт [" + acName + "], который принадлежит игроку [" + plName + "].");
		}
		if (Config.ANNOUNCE_BAN_ACCOUNT){
			announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_ACCOUNT_BLOCK),acName));
			announce("Забанен аккаунт " + acName + ". Нарушение правил");
                }
		return true;
	}
	
	/**
	 * Метод блокировки аккаунта персонажа
	 * @param activeChar
	 * @param account
	 * @return
	 */
	public boolean banAccount(L2PcInstance activeChar, String account)
	{
		if (activeChar == null)
			return false;
		
		try
		{
			LoginServerThread.getInstance().sendAccessLevel(account, -100);
			if (Config.BAN_ACCOUNT_LOG)
				_logBan.info("Модератор [" + activeChar.getName() + "] забанил аккаунт [" + account + "].");
			if (Config.ANNOUNCE_UNBAN_ACCOUNT)
				announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_ACCOUNT_UNBLOCK),account));
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_BAN_ACC_BREAK_RULE),account));
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Метод разблокировки аккаунта персонажа
	 * @param activeChar
	 * @param account
	 * @return
	 */
	public boolean unBanAccount(L2PcInstance activeChar, String account)
	{
		if (activeChar == null)
			return false;
		
		try
		{
			LoginServerThread.getInstance().sendAccessLevel(account, 0);
			if (Config.BAN_ACCOUNT_LOG)
				_logBan.info("Модератор [" + activeChar.getName() + "] снял бан с  аккаунта [" + account + "].");
			if (Config.ANNOUNCE_UNBAN_ACCOUNT)
				announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_ACCOUNT_UNBLOCK),account));
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_UNBAN_ACC),account));
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Метод отправки персонажа в тюрьму, освобождение по срабатываю таймера<br>
	 * Возможно вечное заключение при отрицательном значении таймера
	 * @param activeChar
	 * @param player
	 * @param time
	 * @return
	 */
	public boolean jailPlayer(L2PcInstance activeChar, L2PcInstance player, int time, boolean auto)
	{
		if (!auto && activeChar == null)
			return false;
		if (player == null)
			return false;

		String moderator = activeChar == null ? "ServerGuard" : activeChar.getName();
		try
		{
			String inf = time > 0 ? " for " + time + " minutes." : " forever!";
			if (player.getGameEvent() != null)
				player.getGameEvent().remove(player);
			player.setInJail(true, time);
			if (Config.JAIL_LOG)
				_logBan.info("Модератор [" + moderator + "] отправил в тюрьму игрока [" + player.getName() + (time > 0 ? "] на " + time + " минут." : "] навсегда!"));
			if (Config.ANNOUNCE_JAIL)
				announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_JAIL),player.getName(), player.getName() ,inf));
			if (!auto)
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_SEND_IN_JAIL),player.getName()+inf));
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_ADMIN_PUT_YOU_IN_JAIL),inf));
		}
		catch (NoSuchElementException e)
		{
			if (!auto)
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_SET_ALL_ARG));
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean banPlayer(String player, boolean ban)
	{
		boolean result = true;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET isBanned=? WHERE LOWER(char_name)=?");
			statement.setInt(1, ban ? 1 : 0);
			statement.setString(2, player.toLowerCase());
			statement.execute();
			if (statement.getUpdateCount() == 0)
				result = false;
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			result = false;
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
	/**
	 * Метод отправки персонажа в тюрьму, освобождение по срабатываю таймера<br>
	 * Возможно вечное заключение при отрицательном значении таймера<br>
	 * Данный метод является альтернативой первому методу<br>
	 * В отличии от первого метода, данный метод не требует присутствия игрока в игре
	 * @param activeChar
	 * @param player
	 * @param time
	 * @return
	 */
	public boolean jailPlayer(L2PcInstance activeChar, String player, int time)
	{
		if (activeChar == null)
			return false;
		
		boolean result = true;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE LOWER(char_name)=?");
			statement.setInt(1, -114356);
			statement.setInt(2, -249645);
			statement.setInt(3, -2984);
			statement.setInt(4, 1);
			statement.setLong(5, time * 60000L);
			statement.setString(6, player.toLowerCase());
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();

			if (count == 0)
			{
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAR_NOT_FOUND_IN_BASE));
				result = false;
			}
			else
			{
				String inf = time > 0 ? " for " + time + " minutes." : " forever!";
				if (Config.JAIL_LOG)
					_logBan.info("Модератор [" + activeChar.getName() + "] отправил в тюрьму игрока [" + player + (time > 0 ? "(Offline)] на " + time + " минут." : "(Offline)] навсегда!"));
				if (Config.ANNOUNCE_JAIL)
					announce(String.format(Message.getMessage(null, Message.MessageId.ANNOUNCE_CHAR_JAIL),player ,(time > 0 ? "for " + time + " minutes." : " forever!")));
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_SEND_IN_JAIL),player+inf));
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			result = false;
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
	
	/**
	 * Метод освобождения игрока из тюрьмы
	 * @param activeChar
	 * @param player
	 * @return
	 */
	public boolean unJailPlayer(L2PcInstance activeChar, L2PcInstance player)
	{
		if (activeChar == null || player == null)
			return false;
		
		try
		{
			player.setInJail(false, 0);
			if (Config.JAIL_LOG)
				_logBan.info("Модератор [" + activeChar.getName() + "] выпустил игрока [" + player.getName() + "] из тюрьмы.");
			if (Config.ANNOUNCE_UNJAIL)
				announce(String.format(Message.getMessage(null, Message.MessageId.MSG_RELEASE_FROM_JAIL),player.getName()));
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RELEASE_FROM_JAIL),player.getName()));
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_YOU_RELEASED_FROM_JAIL));
		}
		catch (NoSuchElementException e)
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_TARGET_NOT_FOUND));
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Метод освобождения игрока из тюрьмы<br>
	 * Данный метод является альтернативой первому методу<br>
	 * В отличии от первого метода, данный метод не требует присутствия игрока в игре
	 * @param activeChar
	 * @param player
	 * @return
	 */
	public boolean unJailPlayer(L2PcInstance activeChar, String player)
	{
		if (activeChar == null)
			return false;
		
		boolean result = true;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE LOWER(char_name)=?");
			statement.setInt(1, 17836);
			statement.setInt(2, 170178);
			statement.setInt(3, -3507);
			statement.setInt(4, 0);
			statement.setLong(5, 0);
			statement.setString(6, player.toLowerCase());
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
			{
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_CHAR_NOT_FOUND_IN_BASE));
				result = false;
			}
			else
			{
				if (Config.JAIL_LOG)
					_logBan.info("Модератор [" + activeChar.getName() + "] выпустил игрока [" + player + "(Offline)] из тюрьмы.");
		
				if (Config.ANNOUNCE_UNJAIL)
					announce(String.format(Message.getMessage(null, Message.MessageId.MSG_RELEASE_FROM_JAIL),player));
				
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RELEASE_FROM_JAIL),player));
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			result = false;
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
	
	/**
	 * Метод анонсирования сообщения<br>
	 * В зависимости он настроек сервера анонс идет через стандартный канал анонсов или анонс администратора
	 * @param message
	 */
	private void announce(String message)
	{
		if (Config.CLASSIC_ANNOUNCE_MODE)
			Announcements.getInstance().announceToAll(message);
		else
			Announcements.getInstance().criticalAnnounceToAll(message);
	}
}
