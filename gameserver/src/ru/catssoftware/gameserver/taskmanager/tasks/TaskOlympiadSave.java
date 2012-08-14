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


import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager;
import ru.catssoftware.gameserver.taskmanager.TaskTypes;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * Updates all data of Olympiad nobles in db
 *
 * @author godson
 */
public class TaskOlympiadSave extends Task
{
	private static final Logger	_log	= Logger.getLogger(TaskOlympiadSave.class.getName());
	public static final String	NAME	= "olympiad_save";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if (Olympiad.getInstance().inCompPeriod())
		{
			Olympiad.getInstance().saveOlympiadStatus();
			_log.info("Olympiad System: Data updated successfully.");
		}
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "900000", "1800000", "");
	}
}