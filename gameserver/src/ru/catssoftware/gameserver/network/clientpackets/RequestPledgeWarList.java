package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.PledgeReceiveWarList;

public class RequestPledgeWarList extends L2GameClientPacket
{
	private static final String	_C__D0_1E_REQUESTPLEDGEWARLIST	= "[C] D0:1E RequestPledgeWarList";
	@SuppressWarnings("unused")
	private int					_unk1;
	private int					_tab;

	@Override
	protected void readImpl()
	{
		_unk1 = readD();
		_tab = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (activeChar.getClan() == null)
			return;

		activeChar.sendPacket(new PledgeReceiveWarList(activeChar.getClan(), _tab));
	}

	@Override
	public String getType()
	{
		return _C__D0_1E_REQUESTPLEDGEWARLIST;
	}
}
