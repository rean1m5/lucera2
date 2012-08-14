package ru.catssoftware.gameserver.handler;

/*
 * @author Ro0TT
 * @date 29.05.2012
 */

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnswerHandler
{
	public Map<Integer, IAnswerHandler> _dialogAnswers;

	public static AnswerHandler _instance;
	public static AnswerHandler getInstance()
	{
		if (_instance == null)
			_instance = new AnswerHandler();
		return _instance;
	}

	public AnswerHandler()
	{
		_dialogAnswers = new ConcurrentHashMap<Integer, IAnswerHandler>();
	}

	public void regAnswerHandler(int charId, IAnswerHandler handler)
	{
		checkHandler(charId, false);
		_dialogAnswers.put(charId, handler);
	}

	public void checkHandler(int charId, boolean answer)
	{
		IAnswerHandler handler = _dialogAnswers.remove(charId);

		if (handler!=null)
			handler.dialogAnswer(charId, answer);
	}
}
