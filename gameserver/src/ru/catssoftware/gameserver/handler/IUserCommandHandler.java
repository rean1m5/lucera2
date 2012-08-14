package ru.catssoftware.gameserver.handler;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public interface IUserCommandHandler
{
	public static Logger	_log	= Logger.getLogger(IUserCommandHandler.class.getName());

	public boolean useUserCommand(int id, L2PcInstance activeChar);

	public int[] getUserCommandList();
}