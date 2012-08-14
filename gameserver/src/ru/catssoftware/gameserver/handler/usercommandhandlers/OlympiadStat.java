package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;

public class OlympiadStat implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 109 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;

		if (activeChar == null)
			return false;

		// Получаем необходимые данные
		int _objId = activeChar.getObjectId();
		int _won = Olympiad.getInstance().getCompetitionWon(_objId);
		int _lost = Olympiad.getInstance().getCompetitionLost(_objId);
		int _points = Olympiad.getInstance().getNoblePoints(_objId);
		// Отправляем сообщение. Есть системное сообщение, но из-за конфликта Ru/Eu клиентов сообщение идет путем sendMessage(s)
		activeChar.sendMessage("В течение этой Олимпиады были получены следующие результаты: " + _won + " побед " + _lost + " поражений. Вы заработали " + _points + " очков Олимпиады.");
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}