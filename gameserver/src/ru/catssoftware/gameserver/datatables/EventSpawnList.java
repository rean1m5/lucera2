/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.catssoftware.gameserver.datatables;

import java.util.Date;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.script.DateRange;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @Author neoDeviL
 */
public class EventSpawnList
{

	private static Logger				_log	= Logger.getLogger(EventSpawnList.class.getName());

	private static EventSpawnList	_instance;

	public static EventSpawnList getInstance()
	{
		if (_instance == null)
		{
			_instance = new EventSpawnList();
		}
		return _instance;
	}

	public static void addNewGlobalSpawn(int NpcId, int Xpos, int Ypos, int Zpos, int count, int Heading, int respavntime, DateRange DateRanges)
	{
		L2NpcTemplate template;
		Date currentDate = new Date();

		if (DateRanges.isWithinRange(currentDate))
		{
			template = NpcTable.getInstance().getTemplate(NpcId);
			try
			{
				L2Spawn spawn = new L2Spawn(template);
				spawn.setLocx(Xpos);
				spawn.setLocy(Ypos);
				spawn.setLocz(Zpos);
				spawn.setAmount(count);
				spawn.setHeading(Heading);
				spawn.setRespawnDelay(respavntime);

				SpawnTable.getInstance().addNewSpawn(spawn, false);
				spawn.init();
				System.out.println("Global Spawn :: NPCId: " + NpcId + ", Date Range From: " + DateRanges.getStartDate() + " To: " + DateRanges.getEndDate()
						+ " Now: " + currentDate);
			}
			catch (Exception e)
			{
				_log.error("error while creating npc spawn: " + e);
			}
		}
	}
}