package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ManagePledgePower;

public class RequestPledgePower extends L2GameClientPacket
{
	private static final String	_C__C0_REQUESTPLEDGEPOWER	= "[C] C0 RequestPledgePower";
	private int					_rank, _action, _privs;

	@Override
	protected void readImpl()
	{
		_rank = readD();
		_action = readD();
		if (_action == 2)
		{
			_privs = readD();
		}
		else
			_privs = 0;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (_action == 2)
		{
			if (player.getClan() != null && player.isClanLeader())
			{
				player.getClan().setRankPrivs(_rank, _privs);
			}
		}
		else
		{
			ManagePledgePower mpp = new ManagePledgePower(getClient().getActiveChar().getClan(), _action, _rank);
			player.sendPacket(mpp);
		}
	}

	@Override
	public String getType()
	{
		return _C__C0_REQUESTPLEDGEPOWER;
	}
}
