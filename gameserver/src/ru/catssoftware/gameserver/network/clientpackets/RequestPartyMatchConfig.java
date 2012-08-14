package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.PartyRoomManager;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ListPartyWaiting;

public class RequestPartyMatchConfig extends L2GameClientPacket
{
	private static final String	_C__6F_REQUESTPARTYMATCHCONFIG	= "[C] 6F RequestPartyMatchConfig";

	private int					_page;
	private int					_region;
	private boolean				_showClass;

	@Override
	protected void readImpl()
	{
		_page = readD();
		_region = readD(); // 0 to 15, or -1
		_showClass = readD() == 1; // 1 -> all levels, 0 -> only levels matching my level
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getActiveChar();
		if (player == null)
			return;

		L2Party party = player.getParty();
		if (party != null && !party.isLeader(player))
		{
			sendPacket(SystemMessageId.CANT_VIEW_PARTY_ROOMS);
			ActionFailed();
			return;
		}
		player.setPartyMatchingShowClass(_showClass);
		player.setPartyMatchingRegion(_region);

		PartyRoomManager.getInstance().addToWaitingList(player);
		sendPacket(new ListPartyWaiting(player, _page));

		ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__6F_REQUESTPARTYMATCHCONFIG;
	}
}