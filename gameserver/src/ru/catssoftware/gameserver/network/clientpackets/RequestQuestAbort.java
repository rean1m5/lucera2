package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.QuestList;

public class RequestQuestAbort extends L2GameClientPacket
{
	private static final String	_C__64_REQUESTQUESTABORT	= "[C] 64 RequestQuestAbort";
	private int					_questId;

	@Override
	protected void readImpl()
	{
		_questId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		Quest qe = QuestManager.getInstance().getQuest(_questId);
		if (qe != null)
		{
			QuestState qs = activeChar.getQuestState(qe.getName());
			if (qs != null)
			{
				qs.checkQuestInstance();
				qs.exitQuest(true);
				activeChar.sendMessage("Квест отменен.");
				activeChar.sendPacket(new QuestList(activeChar));
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__64_REQUESTQUESTABORT;
	}
}