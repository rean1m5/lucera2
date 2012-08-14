package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class PartyInfo implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 81 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		if (!activeChar.isInParty())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_NO_PARTY));
			return false;
		}

		L2Party playerParty = activeChar.getParty();
		int memberCount = playerParty.getMemberCount();
		int lootDistribution = playerParty.getLootDistribution();
		String partyLeader = playerParty.getPartyMembers().get(0).getName();

		activeChar.sendPacket(SystemMessageId.PARTY_INFORMATION);
		switch (lootDistribution)
		{
			case L2Party.ITEM_LOOTER:
				activeChar.sendPacket(SystemMessageId.LOOTING_FINDERS_KEEPERS);
				break;
			case L2Party.ITEM_ORDER:
				activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN);
				break;
			case L2Party.ITEM_ORDER_SPOIL:
				activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL);
				break;
			case L2Party.ITEM_RANDOM:
				activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM);
				break;
			case L2Party.ITEM_RANDOM_SPOIL:
				activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL);
				break;
		}

		SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_LEADER_S1);
		sm.addString(partyLeader);
		activeChar.sendPacket(sm);
		activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_PARTY_MEMBERS_COUNT), memberCount));
		activeChar.sendPacket(SystemMessageId.WAR_LIST);
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}