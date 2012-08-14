package ru.catssoftware.gameserver.model.entity.events.DeathMatch;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;

/**
 * @author m095
 * @version 1.0
 */

public class VoiceDeathMatch implements IVoicedCommandHandler
{
	@Override
	public String[] getVoicedCommandList() 
	{
		return new String [] {"dmjoin", "dmleave"};
	}

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) 
	{
		if (activeChar == null)
			return false;

		if (command.equals("dmjoin"))
		{
			if(DeathMatch.getInstance().register(activeChar))
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_REGISTERED),"DeathMatch"));
			return true;
		}
		else if (command.equals("dmleave"))
		{
			if(DeathMatch.getInstance().isState(GameEvent.State.STATE_ACTIVE) && DeathMatch.getInstance().isParticipant(activeChar))
			{
				DeathMatch.getInstance().remove(activeChar);
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_CANCEL_REG),"DeathMatch"));
			}
			else
				activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_NOT_REGISTERED));
			return true;
		}
		return false;
	}

	@Override
	public String getDescription(String command)
	{
		if(command.equals("dmjoin"))
			return "Присоеденится к турниру Death Match.";
		if(command.equals("dmleave"))
			return "Отменить учястие в турнире Death Match.";
		return null;
	}
}
