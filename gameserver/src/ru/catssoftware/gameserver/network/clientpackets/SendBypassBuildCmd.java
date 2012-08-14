package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class SendBypassBuildCmd extends L2GameClientPacket
{
	private String				_command;

	@Override
	protected void readImpl()
	{
		_command = readS();
		if(_command != null)
			_command = _command.trim();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		gmController.getInstance().useCommand(activeChar, _command.split(" "));
	}

	@Override
	public String getType()
	{
		return "[C] 5b SendBypassBuildCmd";
	}
}