package ru.catssoftware.gameserver.model.entity.events.LastHero;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;

public class VoiceLastHero implements IVoicedCommandHandler
{
	@Override
	public String[] getVoicedCommandList() 
	{
		return new String [] {"lhjoin", "lhleave"};
	}

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) 
	{
		if (activeChar == null)
			return false;

		if (command.equals("lhjoin"))
		{
			if(LastHero.getInstance().register(activeChar))
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_REGISTERED),"LastHero"));
			return true;
		}
		else if (command.equals("lhleave"))
		{
			if(LastHero.getInstance().isState(GameEvent.State.STATE_ACTIVE) && LastHero.getInstance().isParticipant(activeChar))
			{
				LastHero.getInstance().remove(activeChar);
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_CANCEL_REG),"LastHero"));
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
		if(command.equals("lhjoin"))
			return "Присоеденится к турниру Last Hero.";
		if(command.equals("lhleave"))
			return "Отменить учястие в турнире Last Hero.";
		return null;
	}
}