package ru.catssoftware.gameserver.network.clientpackets;

import java.util.regex.Pattern;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.ChatHandler;
import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class Say2 extends L2GameClientPacket
{
	private static final String		_C__38_SAY2		= "[C] 38 Say2";
	private static Logger				_logChat		= Logger.getLogger("chat");
	private SystemChatChannelId		_chat;
	private String					_text;
	private String					_target;
	private int						_type;

	@Override
	protected void readImpl()
	{
		_text = readS();
		_type = readD();
		_chat = SystemChatChannelId.getChatType(_type);
		_target = _chat == SystemChatChannelId.Chat_Tell ? readS() : null;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		// Проверяем activeChar
		if (activeChar == null)
			return;

		// Проверка длинны текста
		if (_text.length() > Config.CHAT_LENGTH)
		{
			activeChar.sendMessage("Превышена длинна сообщения!");
			_text = _text.substring(0, Config.CHAT_LENGTH);
		}

		// Проверка допустимого типа чата
		switch (_chat)
		{
			case Chat_None:
			case Chat_Announce:
			case Chat_Critical_Announce:
			case Chat_System:
			case Chat_Custom:
			case Chat_GM_Pet:
			{
				if (!activeChar.isGM())
					_chat =  SystemChatChannelId.Chat_Normal;
			}
		}

		// Проверка на VoiceHandler, парсер Voice команд
		if (_chat == SystemChatChannelId.Chat_Normal && (_text.startsWith(".") && !_text.startsWith("..")))
		{
			String[] _commandParams		= _text.split(" ");
			String command				= _commandParams[0].substring(1);
			String params				= "";

			if (_commandParams.length > 1)
				params = _text.substring(1 + command.length()).trim();
			else if (activeChar.getTarget() != null)
				params = activeChar.getTarget().getName();

			IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);

			if (vch != null)
			{
				if (!activeChar.isGM() && !FloodProtector.tryPerformAction(activeChar, Protected.VOICE_CMD))
				{
					activeChar.sendMessage("Защита от флуда. Попробуйте позже!");
					return;
				}
				else
				{
					vch.useVoicedCommand(command, activeChar, params);
					return;
				}
			}
			else
			{
				IVoicedCommandHandler vc = VoicedCommandHandler.getInstance().getVoicedCommandHandler("menu");
				if (vc == null)
					return;
				
				vc.useVoicedCommand("menu", activeChar, "");
				activeChar.sendMessage("Wrong command: [." + command + "].");
			}
			return;
		}

		// Проверка блокировки чата
		if (!activeChar.isGM() && (activeChar.isChatBanned() || activeChar.isInsideZone(L2Zone.FLAG_NOCHAT)))
		{
			if (_chat != SystemChatChannelId.Chat_User_Pet && _chat != SystemChatChannelId.Chat_Tell)
			{
				activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
				return;
			}
		}

		// Проверка типа чата. Если чар ГМ, то меняем тип чата
		if (_chat == SystemChatChannelId.Chat_User_Pet && activeChar.isGM())
			_chat = SystemChatChannelId.Chat_GM_Pet;


		if (!getClient().checkKeyProtection())
		{
			activeChar.sendMessage("Чат будет доступен только после ввода пароля от вашего чара.");
			activeChar.sendMessage("Используйте voice-команду: .access");
			return;
		}

		// Фильтр разрешенного типа чата при наличии Зарича/Акаманаха
		if (activeChar.isCursedWeaponEquipped())
		{
			switch (_chat)
			{
				case Chat_Shout:
				case Chat_Market:
					activeChar.sendMessage("Чат недоступен");
					return;
			}
		}

		// Проверка абзацов в чате, если абзацы запрещены
		if (!Config.ALLOW_MULTILINE_CHAT)
			_text = _text.replaceAll("\\\\n", "");

		// Запись чата в лог файл
		if (Config.LOG_CHAT)
		{
			if (_chat == SystemChatChannelId.Chat_Tell)
				_logChat.info(_chat.getName() + "[" + activeChar.getName() + " to " + _target + "] " + _text);
			else
				_logChat.info(_chat.getName() + "[" + activeChar.getName() + "] " + _text);
		}

		// Проверка текста на запрещенные слова
		if (Config.USE_SAY_FILTER)
		{
			switch (_chat)
			{
				case Chat_Normal:
				case Chat_Shout:
				case Chat_Market:
				case Chat_Tell:
				case Chat_Hero:
					checkText(activeChar);
			}
		}

		IChatHandler ich = ChatHandler.getInstance().getChatHandler(_chat);
		if (ich != null)
			ich.useChatHandler(activeChar, _target, _chat, _text);
	}

	private void checkText(L2PcInstance activeChar)
	{
		String filteredText = _text;
		for (Pattern pattern : Config.FILTER_LIST)
		{
			filteredText = pattern.matcher(filteredText).replaceAll(Config.CHAT_FILTER_CHARS);
		}

		if (Config.KARMA_ON_OFFENSIVE > 0 && !_text.equals(filteredText))
			activeChar.setKarma(activeChar.getKarma() + Config.KARMA_ON_OFFENSIVE);

		_text = filteredText;
	}

	@Override
	public String getType()
	{
		return _C__38_SAY2;
	}
}