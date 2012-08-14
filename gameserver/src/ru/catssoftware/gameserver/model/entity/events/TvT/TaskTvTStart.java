package ru.catssoftware.gameserver.model.entity.events.TvT;

import org.apache.log4j.Logger;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;


public final class TaskTvTStart extends Task
{
	private static final Logger	_log	= Logger.getLogger(TaskTvTStart.class.getName());	

	@Override
	public String getName()
	{
		return TvT.getInstance().getName();
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{	
			
			if (TvT.getInstance() == null || !TvT.getInstance().isState(GameEvent.State.STATE_OFFLINE))
				return;
			_log.info("TeamVsTeam Event started by Global Task Manager");
			TvT.getInstance().start();
	}
}