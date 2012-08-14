package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;

public class RequestTutorialQuestionMark extends L2GameClientPacket
{
	private static final String	_C__7D_REQUESTTUTORIALQUESTIONMARK	= "[C] 7D RequestTutorialQuestionMark";
	private int					_id;

	@Override
	protected void readImpl()
	{
		_id = readD(); // id
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;
		if(_id < 0 ) {
			if(player.getGameEvent()!=null)
				player.getGameEvent().onCommand(player, "Mark", String.valueOf(_id));
		}
		else {
			QuestState qs = player.getQuestState("255_Tutorial");
			if (qs != null)
				qs.getQuest().notifyEvent("QM" + _id + "", null, player);
		}
	}

	@Override
	public String getType()
	{
		return _C__7D_REQUESTTUTORIALQUESTIONMARK;
	}
}
