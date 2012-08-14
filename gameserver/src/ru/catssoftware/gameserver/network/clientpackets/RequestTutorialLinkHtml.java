package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;

public class RequestTutorialLinkHtml extends L2GameClientPacket
{
	private static final String	_C__7B_REQUESTTUTORIALLINKHTML	= "[C] 7B equestTutorialLinkHtml";
	private String				_link;

	@Override
	protected void readImpl()
	{
		_link = readS(); // link
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent(_link, null, player);
	}

	@Override
	public String getType()
	{
		return _C__7B_REQUESTTUTORIALLINKHTML;
	}
}
