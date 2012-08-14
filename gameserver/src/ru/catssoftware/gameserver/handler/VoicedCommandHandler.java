package ru.catssoftware.gameserver.handler;

import javolution.util.FastMap;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.voicedcommandhandlers.*;

public class VoicedCommandHandler
{
	private static VoicedCommandHandler				_instance;
	private FastMap<String, IVoicedCommandHandler>	_datatable;
	public IVoicedCommandHandler _classMasterHandler;
	
	public static VoicedCommandHandler getInstance()
	{
		if (_instance == null)
			_instance = new VoicedCommandHandler();
		return _instance;
	}

	private VoicedCommandHandler()
	{
		_datatable = new FastMap<String, IVoicedCommandHandler>();
		Help h = new Help();
		registerVoicedCommandHandler(h);
		_datatable.put("devinfo", h);
	}

	public void registerVoicedCommandHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();
		for (String element : ids)
			_datatable.put(element, handler);
	}

	public IVoicedCommandHandler getVoicedCommandHandler(String voicedCommand)
	{
		String command = voicedCommand;
		if (voicedCommand.indexOf(" ") != -1)
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));
		if (Config.DISABLED_COMMANDS.contains(command))
			return null;
		return _datatable.get(command);
	}

	public FastMap<String, IVoicedCommandHandler> getVoicedCommandHandlers()
	{
		return _datatable;
	}
}