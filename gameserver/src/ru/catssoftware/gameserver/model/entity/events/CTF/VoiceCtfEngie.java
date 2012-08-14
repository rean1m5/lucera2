package ru.catssoftware.gameserver.model.entity.events.CTF;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IVoicedCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;

public class VoiceCtfEngie implements IVoicedCommandHandler
{
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[] {"ctfjoin", "ctfleave"};
	}

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
			return false;

		if (command.equals("ctfjoin"))
		{
			if(CTF.getInstance().register(activeChar))
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_REGISTERED),"CTF"));
			return true;
		}
		else if (command.equals("ctfleave"))
		{
			if(CTF.getInstance().isState(GameEvent.State.STATE_ACTIVE) && CTF.getInstance().isParticipant(activeChar))
			{
				CTF.getInstance().remove(activeChar);
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_EVENT_CANCEL_REG),"CTF"));
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
		if(command.equals("ctfjoin"))
			return "Присоеденится к турниру CTF.";
		if(command.equals("ctfleave"))
			return "Отменить учястие в турнире CTF.";
		return null;
	}
}