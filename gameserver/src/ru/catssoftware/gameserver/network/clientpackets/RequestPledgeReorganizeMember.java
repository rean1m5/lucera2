package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestPledgeReorganizeMember extends L2GameClientPacket
{
	private static final String	_C__D0_24_REQUESTPLEDGEREORGANIZEMEMBER	= "[C] D0:24 RequestPledgeReorganizeMember";

	private int			_isMemberSelected;
	private String		_memberName;
	private int			_newPledgeType;
	private String		_selectedMember;

	@Override
	protected void readImpl()
	{
		_isMemberSelected = readD();
		_memberName = readS();
		_newPledgeType = readD();
		_selectedMember = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		//do we need powers to do that??
		L2Clan clan = activeChar.getClan();
		if (clan == null)
			return;
		
		L2ClanMember member2 = null;
		L2ClanMember member1 = clan.getClanMember(_memberName);
		
		if (_isMemberSelected == 0)
		{
			if (clan.getSubPledgeMembersCount(_newPledgeType) >= clan.getMaxNrOfMembers(_newPledgeType))
			{
				if (_newPledgeType == 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_FULL);
					sm.addString(clan.getName());
					activeChar.sendPacket(sm);
					sm = null;
				}
				else
					activeChar.sendPacket(SystemMessageId.SUBCLAN_IS_FULL);
				return;
			}
			if (member1 == null)
				return;
		}
		else
		{
			member2 = clan.getClanMember(_selectedMember);
			if (member1 == null || member2 == null)
				return;
		}
		
		int oldPledgeType = member1.getSubPledgeType();
		if (oldPledgeType == -1 || _newPledgeType == -1)
			return;
		if (oldPledgeType == _newPledgeType)
			return;

		member1.setSubPledgeType(_newPledgeType);
		if (_isMemberSelected != 0)
			member2.setSubPledgeType(oldPledgeType);
		clan.broadcastClanStatus();
	}

	@Override
	public String getType()
	{
		return _C__D0_24_REQUESTPLEDGEREORGANIZEMEMBER;
	}
}