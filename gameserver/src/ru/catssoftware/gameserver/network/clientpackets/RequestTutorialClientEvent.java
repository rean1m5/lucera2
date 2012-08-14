package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;

/*
 * events:
 * 00 none
 * 01 Move Char
 * 02 Move Point of View
 * 03 ??
 * 04 ??
 * 05 ??
 * 06 ??
 * 07 ??
 * 08 Talk to Newbie Helper
 */

/**
 * 7E 01 00 00 00
 *
 * Format: (c) cccc
 *
 * @author  DaDummy
 */
public class RequestTutorialClientEvent extends L2GameClientPacket
{
	private static final String	_C__7E_REQUESTTUTORIALCLIENTEVENT	= "[C] 7E RequestTutorialClientEvent";
	private int					_event;

	@Override
	protected void readImpl()
	{
		_event = readD(); // event
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;

		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("CE" + _event + "", null, player);
	}

	@Override
	public String getType()
	{
		return _C__7E_REQUESTTUTORIALCLIENTEVENT;
	}
}
