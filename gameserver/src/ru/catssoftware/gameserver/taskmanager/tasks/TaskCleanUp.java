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


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Tempy
 */
public final class TaskCleanUp extends Task
{
	public static final String	NAME	= "clean_up";
	protected static final Logger	_log	= Logger.getLogger(TaskCleanUp.class.getName());

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		_log.info("Executing clean up task");
		System.gc();
		System.runFinalization();
		_log.info("RAM Used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576));
	}
}