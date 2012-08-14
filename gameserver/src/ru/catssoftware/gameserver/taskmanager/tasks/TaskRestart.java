package ru.catssoftware.gameserver.taskmanager.tasks;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager;
import ru.catssoftware.gameserver.taskmanager.TaskTypes;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

public final class TaskRestart extends Task
{
	public static final String	NAME				= "restart";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if (Config.ENABLE_RESTART)
		{
			Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]), Shutdown.ShutdownModeType.RESTART);
			handler.start();
		}
	}

	@Override
	public void initializate()
	{
		if (Config.ENABLE_RESTART)
		{
			int timeInMin = Integer.parseInt(Config.RESTART_WARN_TIME);
			int finalTime = timeInMin * 60;
			String timer = ("" + finalTime);
			super.initializate();
			TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", Config.RESTART_TIME, timer);
		}
	}
}