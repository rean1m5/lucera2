package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAskJoinMPCC;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestExAskJoinMPCC extends L2GameClientPacket
{
	private static final String	_C__D0_0D_REQUESTEXASKJOINMPCC	= "[C] D0:0D RequestExAskJoinMPCC";
	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		L2PcInstance activeChar = getClient().getActiveChar();
		L2PcInstance player = L2World.getInstance().getPlayer(_name);

		if (activeChar == null)
			return;
		if (player == null)
			return;
		if (activeChar.isInParty() && player.isInParty() && activeChar.getParty().equals(player.getParty()))
			return;
		if (activeChar.isInParty())
		{
			L2Party activeParty = activeChar.getParty();
			if (activeParty.getLeader() == activeChar)
			{
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
				{
					if (player.isInParty())
					{
						if (player.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL);
							sm.addString(player.getName());
							activeChar.sendPacket(sm);
						}
						else
							askJoinMPCC(activeChar, player);
					}
					else
						activeChar.sendMessage("Ваша цель не в группе.");
				}
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
					activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
				else
				{
					if (player.isInParty())
					{
						if (player.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL);
							sm.addString(player.getName());
							activeChar.sendPacket(sm);
						}
						else
							askJoinMPCC(activeChar, player);
					}
					else
						activeChar.sendMessage("Ваша цель не в группе.");
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
		}
	}

	private void askJoinMPCC(L2PcInstance requestor, L2PcInstance target)
	{
		if (!requestor.getParty().isInCommandChannel())
		{
			boolean hasRight = false;
			if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId() && requestor.getClan().getLevel() >= 5) // Clanleader
				hasRight = true;
			else
			{
				for (L2Skill skill : requestor.getAllSkills())
				{
					if (skill.getId() == 391)
					{
						hasRight = true;
						break;
					}
				}
			}

			if (!hasRight)
			{
				if (requestor.destroyItemByItemId("MPCC", 8871, 1, requestor, false))
				{
					hasRight = true;
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(8871);
					sm.addNumber(1);
					requestor.sendPacket(sm);
				}
			}

			if (!hasRight)
			{
				requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
				return;
			}
		}

		if (!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_INVITING_YOU_TO_COMMAND_CHANNEL_CONFIRM);
			sm.addString(requestor.getName());
			target.getParty().getLeader().sendPacket(sm);
			target.getParty().getLeader().sendPacket(new ExAskJoinMPCC(requestor.getName()));
			requestor.sendMessage("Вы пригласили " + target.getName() + " в командный чат.");
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_0D_REQUESTEXASKJOINMPCC;
	}
}
