package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.banmanager.BanManager;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author m095
 * @version 3.0
 */

public class banchat extends gmHandler
{
	private static final String[] commands =
	{
		"banchat",
		"unbanchat",
		"banchat_all",
		"unbanchat_all"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		int				time		= 30;
		L2PcInstance 	player 		= null;
		String			reason		= "Нарушение";
		String 			command 	= params[0];

		if (params.length > 1)
		{
			player = L2World.getInstance().getPlayer(params[1]);
			if (player != null)
			{
				if (params.length > 2)
				{
					try
					{
						time = Integer.parseInt(params[2]);
					}
					catch (Exception e)
					{
						admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_SET_ALL_ARG));
						return;
					}
	
					// причина бана
					if (params.length > 3)
					{
						reason = params[3];
	
						if(params.length > 4)
						{
							for(int i = 4; i < params.length; i++)
								reason += " " + params[i];
						}
					}
				}
			}
			else
			{
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_CHAR_NOT_FOUND));
				return;
			}
		}
		else
		{
			admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_SET_ALL_ARG));
			return;
		}

		if (command.startsWith("banchat_all"))
		{
			if (!BanManager.getInstance().banChatAll(admin))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));

			return;
		}
		else if (command.startsWith("unbanchat_all"))
		{
			if (!BanManager.getInstance().unBanChatAll(admin))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));

			return;
		}
		if (command.startsWith("banchat"))
		{
			if (!BanManager.getInstance().banChat(admin, player, reason, time))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));

			return;
		}
		else if (command.startsWith("unbanchat"))
		{
			if (!BanManager.getInstance().unBanChat(admin, player))
				admin.sendMessage(Message.getMessage(admin, Message.MessageId.MSG_ERROR_TRY_LATER));

			return;
		}
		return;
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}