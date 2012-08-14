package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.handler.UserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class RequestUserCommand extends L2GameClientPacket
{
	private static final String	_C__AA_REQUESTUSERCOMMAND	= "[C] aa RequestUserCommand";
	private int					_command;

	@Override
	protected void readImpl()
	{
		_command = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;

		IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(_command);

		if (handler == null)
		{
			player.sendMessage("Команда не реализовна.");
			return;
		}
		else
		{
			if (!FloodProtector.tryPerformAction(player, Protected.USER_CMD))
				player.sendMessage("Защита от флуда, попробуйте позже!");
			else
				handler.useUserCommand(_command, player);
		}
	}

	@Override
	public String getType()
	{
		return _C__AA_REQUESTUSERCOMMAND;
	}
}