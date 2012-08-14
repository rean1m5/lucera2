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

import java.util.Calendar;
import java.util.Map;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.RaidPointsManager;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import ru.catssoftware.gameserver.taskmanager.Task;
import ru.catssoftware.gameserver.taskmanager.TaskManager;
import ru.catssoftware.gameserver.taskmanager.TaskTypes;
import ru.catssoftware.gameserver.taskmanager.TaskManager.ExecutedTask;

public class TaskRaidPointsReset extends Task
{
	protected static final Logger	_log	= Logger.getLogger(TaskRaidPointsReset.class);

	public static final String	NAME	= "raid_points_reset";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Calendar cal = Calendar.getInstance();

		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
		{
			// reward clan reputation points
			Map<Integer, Integer> rankList = RaidPointsManager.getRankList();
			for (L2Clan c : ClanTable.getInstance().getClans())
			{
				for (Map.Entry<Integer, Integer> entry : rankList.entrySet())
				{
					if (entry.getValue() <= 100 && c.isMember(entry.getKey()))
					{
						int reputation = 0;
						switch (entry.getValue())
						{
						case 1:
							reputation = 1250;
							break;
						case 2:
							reputation = 900;
							break;
						case 3:
							reputation = 700;
							break;
						case 4:
							reputation = 600;
							break;
						case 5:
							reputation = 450;
							break;
						case 6:
							reputation = 350;
							break;
						case 7:
							reputation = 300;
							break;
						case 8:
							reputation = 200;
							break;
						case 9:
							reputation = 150;
							break;
						case 10:
							reputation = 100;
							break;
						default:
							if (entry.getValue() <= 50)
								reputation = 25;
							else
								reputation = 12;
							break;
						}
						c.setReputationScore(c.getReputationScore() + reputation, true);
						c.broadcastToOnlineMembers(new PledgeShowInfoUpdate(c));
					}
				}
			}
			RaidPointsManager.cleanUp();
			_log.info("Raid Points Reset Global Task: launched.");
		}
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "00:10:00", "");
	}
}