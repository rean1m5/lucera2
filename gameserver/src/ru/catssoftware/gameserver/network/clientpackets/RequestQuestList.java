package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.QuestList;

public class RequestQuestList extends L2GameClientPacket
{
	private static final String	_C__63_REQUESTQUESTLIST	= "[C] 63 RequestQuestList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() != null)
			sendPacket(new QuestList(getClient().getActiveChar()));
	}

	@Override
	public String getType()
	{
		return _C__63_REQUESTQUESTLIST;
	}
}