package ru.catssoftware.gameserver.model.entity.events.CTF;

import org.apache.log4j.Logger;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;


public final class TaskCtfStart extends Task
{
	private static final Logger	_log	= Logger.getLogger(TaskCtfStart.class.getName());	

	@Override
	public String getName()
	{
		return CTF.getInstance().getName();
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if(CTF.getInstance()!=null)
			if(CTF.getInstance().isState(GameEvent.State.STATE_OFFLINE)) {
				CTF.getInstance().start();
				_log.info("CTF Event started by Global Task Manager");
			}
	}
}