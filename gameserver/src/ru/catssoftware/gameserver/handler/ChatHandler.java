package ru.catssoftware.gameserver.handler;

import java.lang.reflect.Constructor;
import java.util.Map;
import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.util.JarUtils;

public class ChatHandler
{
	private final static Logger						_log		= Logger.getLogger(ChatHandler.class.getName());
	private static ChatHandler						_instance	= null;

	private Map<SystemChatChannelId, IChatHandler>	_datatable;

	public static ChatHandler getInstance()
	{
		if (_instance == null)
			_instance = new ChatHandler();
		return _instance;
	}

	public ChatHandler()
	{
		_datatable = new FastMap<SystemChatChannelId, IChatHandler>();
		try {
			for(String handler : JarUtils.enumClasses("ru.catssoftware.gameserver.handler.chathandlers")) try {
				Class<?> _handler = Class.forName(handler);
				if(_handler!=null && IChatHandler.class.isAssignableFrom(_handler)) {
					Constructor<?> ctor = _handler.getConstructor();
					if(ctor!=null) 
						registerChatHandler((IChatHandler)ctor.newInstance());
				}
			} catch(Exception e) {
				continue;
			}
		} catch(Exception e) {
		
		}
		
		_log.info("ChatHandler: Loaded " + _datatable.size() + " handlers.");
	}

	public void registerChatHandler(IChatHandler handler)
	{
		SystemChatChannelId chatId[] = handler.getChatTypes();

		for (SystemChatChannelId chat : chatId)
			_datatable.put(chat, handler);
	}

	public IChatHandler getChatHandler(SystemChatChannelId chatId)
	{
		return _datatable.get(chatId);
	}
}
