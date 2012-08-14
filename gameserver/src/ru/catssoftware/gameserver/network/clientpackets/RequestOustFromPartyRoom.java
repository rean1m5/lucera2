package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2PartyRoom;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_09_REQUESTOUSTFROMPARTYROOM = "[C] D0:09 RequestOustFromPartyRoom";

	private int _objectId;

    @Override
    protected void readImpl()
    {
    	_objectId = readD();
    }

	@Override
    protected void runImpl()
	{
		L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
			return;
		L2PcInstance target = L2World.getInstance().findPlayer(_objectId);
		if (target == null || target == activeChar)
		{
			ActionFailed();
			return;
		}

		L2Party party = target.getParty();
		if (party != null && party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(activeChar))
		{
			sendPacket(SystemMessageId.COULD_NOT_OUST_FROM_PARTY);
			ActionFailed();
			return;
		}

		L2PartyRoom room = activeChar.getPartyRoom();
		if (room != null && room.getLeader() == activeChar)
		{
			if (party != null)
				party.removePartyMember(target, true);
			else
				room.removeMember(target, true);
		}

		ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__D0_09_REQUESTOUSTFROMPARTYROOM;
	}
}
