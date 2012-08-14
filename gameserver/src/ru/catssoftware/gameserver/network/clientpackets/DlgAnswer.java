package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.AnswerHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class DlgAnswer extends L2GameClientPacket
{
	private static final String	_C__C5_DLGANSWER	= "[C] C5 DlgAnswer";

	private int	_messageId;
	private int	_answer;
	private int _requesterId;

	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requesterId = readD();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance cha = getClient().getActiveChar();
		if (cha == null)
			return;

		if (_messageId == SystemMessageId.S1_MAKING_RESSURECTION_REQUEST.getId())
			cha.reviveAnswer(_answer);
		else if (_messageId == SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
			cha.teleportAnswer(_answer, _requesterId);
		else if (Config.ALLOW_WEDDING && cha.isEngageRequest())
			cha.engageAnswer(_answer);
		else if (_messageId == 1140)
			cha.gatesAnswer(_answer, 1);
		else if (_messageId == 1141)
			cha.gatesAnswer(_answer, 0);
		else
			AnswerHandler.getInstance().checkHandler(cha.getObjectId(), _answer == 1);
	}

	@Override
	public String getType()
	{
		return _C__C5_DLGANSWER;
	}
}