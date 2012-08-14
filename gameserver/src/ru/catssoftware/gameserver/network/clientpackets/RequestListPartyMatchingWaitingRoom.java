package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	private static final String	_C__D0_16_REQUESTLISTPARTYMATCHINGWAITINGROOM	= "[C] D0:16 RequestListPartyMatchingWaitingRoom";

	@SuppressWarnings("unused")
	private int					_page;
	@SuppressWarnings("unused")
	private boolean				_showAll;
	private int					_minLevel;
	private int					_maxLevel;

	@Override
	protected void readImpl()
	{
		_page = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_showAll = readD() == 1; // client sends 0 if in party room, 1 if not in party room. If you are in party room, only players with matching level are shown.
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (_minLevel < 1)
			_minLevel = 1;
		else if (_minLevel > 85)
			_minLevel = 85;
		if (_maxLevel < _minLevel)
			_maxLevel = _minLevel;
		else if (_maxLevel > 85)
			_maxLevel = 85;
	}

	@Override
	public String getType()
	{
		return _C__D0_16_REQUESTLISTPARTYMATCHINGWAITINGROOM;
	}
}
