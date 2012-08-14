package ru.catssoftware.gameserver.handler;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public interface IVoicedCommandHandler
{
	public static Logger	_log	= Logger.getLogger(IVoicedCommandHandler.class.getName());

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target);

	public String[] getVoicedCommandList();

	public String getDescription(String command);
}