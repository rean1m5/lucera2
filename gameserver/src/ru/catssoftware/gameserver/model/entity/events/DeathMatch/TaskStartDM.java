package ru.catssoftware.gameserver.model.entity.events.DeathMatch;

import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author m095
 * @version 1.0
 */

public class TaskStartDM extends Task
{
	
	@Override
	public String getName()
	{
		return DeathMatch.getInstance().getName();
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if(DeathMatch.getInstance()!=null && DeathMatch.getInstance().isState(GameEvent.State.STATE_OFFLINE))
		{
			DeathMatch.getInstance().start();
		}
	}
}