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
package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.instancemanager.DayNightSpawnManager;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Rayan
 * @since 2096
 * @project L2Emu Project
 *
 */
public class JailSpawnTable
{
	private static Logger						_log			= Logger.getLogger(JailSpawnTable.class.getName());
	private static JailSpawnTable	_instance;
	protected Map<Integer, L2Spawn>	_jailSpawntable	= new FastMap<Integer, L2Spawn>().setShared(true);
	private int						_jailSpawnCount;
	public static int				_highestJailDbId;

	/**
	 * Loads Jail Spawn System
	 *
	 */
	public void loadJailSpawns()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM jail_spawnlist ORDER BY id");
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			_jailSpawnCount = _jailSpawntable.size();

			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.getType().equalsIgnoreCase("L2SiegeGuard"))
					{
						// Don't spawn siege guards
					}
					else if (template1.getType().equalsIgnoreCase("L2RaidBoss"))
					{
						// Don't spawn raidbosses
					}
					else if (!Config.SPAWN_CLASS_MASTER && template1.getType().equals("L2ClassMaster"))
					{
						// Dont' spawn class masters
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(_jailSpawnCount);
						spawnDat.setDbId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						spawnDat.setCustom();
						int loc_id = rset.getInt("loc_id");
						spawnDat.setLocation(loc_id);

						switch (rset.getInt("periodOfDay"))
						{
						case 0: // default
							_jailSpawnCount += spawnDat.init();
							break;
						case 1: // Day
							DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
							_jailSpawnCount++;
							break;
						case 2: // Night
							DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
							_jailSpawnCount++;
							break;
						}

						if (spawnDat.getDbId() > _highestJailDbId)
							_highestJailDbId = spawnDat.getDbId();
						_jailSpawntable.put(spawnDat.getId(), spawnDat);
					}
				}
				else
				{
					_log.warn("JailSpawnTable: Data missing or incorrect in Custom NPC table for NPC ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warn("JailSpawnTable: Jail spawn could not be initialized: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.error("Error While loading jailspawns " + e.getMessage());
			}
		}
		_jailSpawnCount = _jailSpawntable.size();

		if (_jailSpawnCount > 0)
			_log.info("JailSpawnTable: Loaded " + _jailSpawnCount + " Jail Spawn Locations.");
	}

	public static JailSpawnTable getInstance()
	{
		if (_instance == null)
			_instance = new JailSpawnTable();

		return _instance;
	}
}