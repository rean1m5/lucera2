package ru.catssoftware.gameserver.taskmanager;

import java.util.concurrent.ScheduledFuture;

import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

public abstract class Task
{
	public void initializate(){}

	public ScheduledFuture<?> launchSpecial(ExecutedTask instance)
	{
		return null;
	}

	public abstract String getName();

	public abstract void onTimeElapsed(ExecutedTask task);

	public void onDestroy()
	{
	}
}