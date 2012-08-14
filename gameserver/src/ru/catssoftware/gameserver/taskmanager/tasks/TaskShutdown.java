/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.taskmanager.tasks;

import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 */
public class TaskShutdown extends Task
{
	public static final String	NAME	= "shutdown";

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.taskmanager.Task#onTimeElapsed(ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]), Shutdown.ShutdownModeType.SHUTDOWN);
		handler.start();
	}
}