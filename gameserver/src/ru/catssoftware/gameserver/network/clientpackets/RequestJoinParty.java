package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.BlockList;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AskJoinParty;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestJoinParty extends L2GameClientPacket
{
	private static final String	_C__29_REQUESTJOINPARTY	= "[C] 29 RequestJoinParty";
	private String				_name;
	private int					_itemDistribution;

	@Override
	protected void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance requestor = getClient().getActiveChar();
		L2PcInstance target = L2World.getInstance().getPlayer(_name);

		if (requestor == null || target == null)
			return;

		if (target.isGM() && target.getAppearance().isInvisible() && !requestor.isGM())
		{
			requestor.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}

		if(target.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE) {
			requestor.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}
		if (BlockList.isBlocked(target, requestor))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addCharName(target);
			requestor.sendPacket(sm);
			return;
		}
		if (target.isInParty())
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.S1_IS_ALREADY_IN_PARTY);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
			return;
		}
		if (target.isOfflineTrade())
		{
			requestor.sendMessage(target.getName() + " не может вступить в группу."); 
			return;
		}
		if (target == requestor)
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped())
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		if (!requestor.isGM() && target.getInstanceId() != requestor.getInstanceId())
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if(requestor.getGameEvent()!=null && !requestor.getGameEvent().canInteract(requestor, target))
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
			
		if (target.isInJail() || requestor.isInJail())
		{
			requestor.sendMessage("Игрок в тюрьме");
			return;
		}
		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
			return;
		if (target.isInDuel() || requestor.isInDuel())
			return;
		if (!requestor.isInParty()) //Asker has no party
			createNewParty(target, requestor);
		else //Asker is in party
		{
			if (requestor.getParty().isInDimensionalRift())
				requestor.sendMessage("Игрок находится в Дименьшен Рифте, приглашение невозможно.");
			else
				addTargetToParty(target, requestor);
		}
	}

	private void addTargetToParty(L2PcInstance target, L2PcInstance requestor)
	{
		SystemMessage msg;
		if (requestor.getParty().getMemberCount() + requestor.getParty().getPendingInvitationNumber() >= 9)
		{
			requestor.sendPacket(SystemMessageId.PARTY_FULL);
			return;
		}
		if (!requestor.getParty().isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
			return;
		}
		if (!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(), requestor.getParty().getLootDistribution()));
			requestor.getParty().increasePendingInvitationNumber();
			msg = new SystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
		}
		else
		{
			msg = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName());
			requestor.sendPacket(msg);
		}
		msg = null;
	}

	private void createNewParty(L2PcInstance target, L2PcInstance requestor)
	{
		SystemMessage msg;

		if (!target.isProcessingRequest())
		{
			requestor.setParty(new L2Party(requestor, _itemDistribution));

			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
			requestor.getParty().increasePendingInvitationNumber();
			msg = new SystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
		}
		else
		{
			msg = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
		}
	}

	@Override
	public String getType()
	{
		return _C__29_REQUESTJOINPARTY;
	}
}