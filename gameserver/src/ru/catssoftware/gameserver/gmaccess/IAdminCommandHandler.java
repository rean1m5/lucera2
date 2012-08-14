package ru.catssoftware.gameserver.gmaccess;



import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public interface IAdminCommandHandler
{
	public static Logger	_log	= Logger.getLogger(IAdminCommandHandler.class.getName());

	public boolean useAdminCommand(String command, L2PcInstance activeChar);

	public String[] getAdminCommandList();
}
