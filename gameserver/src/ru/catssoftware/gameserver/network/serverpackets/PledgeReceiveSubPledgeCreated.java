package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.L2Clan.SubPledge;

public class PledgeReceiveSubPledgeCreated extends L2GameServerPacket
{
	private static final String	_S__FE_3F_PLEDGERECEIVESUBPLEDGECREATED	= "[S] FE:3F PledgeReceiveSubPledgeCreated";
	private SubPledge			_subPledge;
	private L2Clan				_clan;

	public PledgeReceiveSubPledgeCreated(SubPledge subPledge, L2Clan clan)
	{
		_subPledge = subPledge;
		_clan = clan;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x3F);
		writeD(0x01);
		writeD(_subPledge.getId());
		writeS(_subPledge.getName());
		writeS(getLeaderName());
	}

	private String getLeaderName()
	{
		if (_subPledge.getLeaderId() != 0 && _subPledge.getId() != L2Clan.SUBUNIT_ACADEMY)
		{
			if (_clan != null)
			{
				L2ClanMember player = _clan.getClanMember(_subPledge.getLeaderId());
				if (player != null)
					return player.getName();
			}
		}
		return "";
	}

	@Override
	public String getType()
	{
		return _S__FE_3F_PLEDGERECEIVESUBPLEDGECREATED;
	}
}