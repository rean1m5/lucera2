package ru.catssoftware.gameserver.handler.chathandlers;

import ru.catssoftware.gameserver.handler.IChatHandler;
import ru.catssoftware.gameserver.instancemanager.PetitionManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class ChatPetition implements IChatHandler
{
	private SystemChatChannelId[]	_chatTypes	= { SystemChatChannelId.Chat_GM_Pet, SystemChatChannelId.Chat_User_Pet };

	public SystemChatChannelId[] getChatTypes()
	{
		return _chatTypes;
	}

	public void useChatHandler(L2PcInstance activeChar, String target, SystemChatChannelId chatType, String text)
	{
		if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT);
			return;
		}
		PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
	}
}