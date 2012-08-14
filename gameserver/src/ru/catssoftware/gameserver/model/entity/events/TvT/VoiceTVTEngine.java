package ru.catssoftware.gameserver.model.entity.events.TvT;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;

public class VoiceTVTEngine implements IVoicedCommandHandler
{
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[] { "tvtjoin", "tvtleave" };
	}

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
			return false;

		if(command.equals("tvtjoin"))
		{
			if(TvT.getInstance().register(activeChar))
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_REGISTERED),"TvT"));
			return true;
		}
		else if(command.equals("tvtleave"))
		{
			if(TvT.getInstance().isState(GameEvent.State.STATE_ACTIVE) && TvT.getInstance().isParticipant(activeChar))
			{
				TvT.getInstance().remove(activeChar);
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_CANCEL_REG),"TvT"));
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
		if(command.equals("tvtjoin"))
			return "Присоеденится к турниру TvT.";
		if(command.equals("tvtleave"))
			return "Отменить учястие в турнире TvT.";
		return null;
	}
}