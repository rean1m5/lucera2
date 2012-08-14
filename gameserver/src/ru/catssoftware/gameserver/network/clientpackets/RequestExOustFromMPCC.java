package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestExOustFromMPCC extends L2GameClientPacket
{
	private static final String	_C__D0_0F_REQUESTEXOUSTFROMMPCC	= "[C] D0:0F RequestExOustFromMPCC";
	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance target = L2World.getInstance().getPlayer(_name);
		L2PcInstance activeChar = getClient().getActiveChar();

		if (target != null && target.isInParty() && activeChar.isInParty() && activeChar.getParty().isInCommandChannel()
				&& target.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar))
		{
			target.getParty().getCommandChannel().removeParty(target.getParty());

			SystemMessage sm = SystemMessage.sendString("Ваша группа покинула командный канал.");
			target.getParty().broadcastToPartyMembers(sm);

			sm = SystemMessage.sendString(target.getParty().getPartyMembers().get(0).getName() + "'s party was dismissed from the CommandChannel.");
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_0F_REQUESTEXOUSTFROMMPCC;
	}
}