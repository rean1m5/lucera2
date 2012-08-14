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
import java.sql.SQLException;
import java.util.Collection;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;
import ru.catssoftware.util.StatsSet;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.1.2.10 $ $Date: 2005/03/29 14:00:54 $
 */
public class CharTemplateTable
{
	private final static Logger				_log			= Logger.getLogger(CharTemplateTable.class.getName());

	private static CharTemplateTable		_instance;

	public static final String[]			CHAR_CLASSES	=
															{
			"Human Fighter",
			"Warrior",
			"Gladiator",
			"Warlord",
			"Human Knight",
			"Paladin",
			"Dark Avenger",
			"Rogue",
			"Treasure Hunter",
			"Hawkeye",
			"Human Mystic",
			"Human Wizard",
			"Sorceror",
			"Necromancer",
			"Warlock",
			"Cleric",
			"Bishop",
			"Prophet",
			"Elven Fighter",
			"Elven Knight",
			"Temple Knight",
			"Swordsinger",
			"Elven Scout",
			"Plainswalker",
			"Silver Ranger",
			"Elven Mystic",
			"Elven Wizard",
			"Spellsinger",
			"Elemental Summoner",
			"Elven Oracle",
			"Elven Elder",
			"Dark Fighter",
			"Palus Knight",
			"Shillien Knight",
			"Bladedancer",
			"Assassin",
			"Abyss Walker",
			"Phantom Ranger",
			"Dark Elven Mystic",
			"Dark Elven Wizard",
			"Spellhowler",
			"Phantom Summoner",
			"Shillien Oracle",
			"Shillien Elder",
			"Orc Fighter",
			"Orc Raider",
			"Destroyer",
			"Orc Monk",
			"Tyrant",
			"Orc Mystic",
			"Orc Shaman",
			"Overlord",
			"Warcryer",
			"Dwarven Fighter",
			"Dwarven Scavenger",
			"Bounty Hunter",
			"Dwarven Artisan",
			"Warsmith",
			"dummyEntry1",
			"dummyEntry2",
			"dummyEntry3",
			"dummyEntry4",
			"dummyEntry5",
			"dummyEntry6",
			"dummyEntry7",
			"dummyEntry8",
			"dummyEntry9",
			"dummyEntry10",
			"dummyEntry11",
			"dummyEntry12",
			"dummyEntry13",
			"dummyEntry14",
			"dummyEntry15",
			"dummyEntry16",
			"dummyEntry17",
			"dummyEntry18",
			"dummyEntry19",
			"dummyEntry20",
			"dummyEntry21",
			"dummyEntry22",
			"dummyEntry23",
			"dummyEntry24",
			"dummyEntry25",
			"dummyEntry26",
			"dummyEntry27",
			"dummyEntry28",
			"dummyEntry29",
			"dummyEntry30",
			"Duelist",
			"DreadNought",
			"Phoenix Knight",
			"Hell Knight",
			"Sagittarius",
			"Adventurer",
			"Archmage",
			"Soultaker",
			"Arcana Lord",
			"Cardinal",
			"Hierophant",
			"Eva Templar",
			"Sword Muse",
			"Wind Rider",
			"Moonlight Sentinel",
			"Mystic Muse",
			"Elemental Master",
			"Eva's Saint",
			"Shillien Templar",
			"Spectral Dancer",
			"Ghost Hunter",
			"Ghost Sentinel",
			"Storm Screamer",
			"Spectral Master",
			"Shillien Saint",
			"Titan",
			"Grand Khavatari",
			"Dominator",
			"Doomcryer",
			"Fortune Seeker",
			"Maestro",
			"dummyEntry31",
			"dummyEntry32",
			"dummyEntry33",
			"dummyEntry34",
			"Male Soldier",
			"Female Soldier",
			"Dragoon",
			"Warder",
			"Berserker",
			"Male Soulbreaker",
			"Female Soulbreaker",
			"Arbalester",
			"Doombringer",
			"Male Soulhound",
			"Female Soulhound",
			"Trickster",
			"Inspector",
			"Judicator"									};

	private final FastMap<Integer, L2PcTemplate> _templates = new FastMap<Integer, L2PcTemplate>();

	public static CharTemplateTable getInstance()
	{
		if (_instance == null)
			_instance = new CharTemplateTable();

		return _instance;
	}

	private CharTemplateTable()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT * FROM class_list, char_templates, lvlupgain"
					+ " WHERE class_list.id = char_templates.classId" + " AND class_list.id = lvlupgain.classId" + " ORDER BY class_list.id");
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				StatsSet set = new StatsSet();
				set.set("classId", rset.getInt("id"));
				set.set("className", rset.getString("className"));
				set.set("raceId", rset.getInt("raceId"));
				set.set("baseSTR", rset.getInt("STR"));
				set.set("baseCON", rset.getInt("CON"));
				set.set("baseDEX", rset.getInt("DEX"));
				set.set("baseINT", rset.getInt("_INT"));
				set.set("baseWIT", rset.getInt("WIT"));
				set.set("baseMEN", rset.getInt("MEN"));
				set.set("baseHpMax", rset.getFloat("defaultHpBase"));
				set.set("lvlHpAdd", rset.getFloat("defaultHpAdd"));
				set.set("lvlHpMod", rset.getFloat("defaultHpMod"));
				set.set("baseMpMax", rset.getFloat("defaultMpBase"));
				set.set("baseCpMax", rset.getFloat("defaultCpBase"));
				set.set("lvlCpAdd", rset.getFloat("defaultCpAdd"));
				set.set("lvlCpMod", rset.getFloat("defaultCpMod"));
				set.set("lvlMpAdd", rset.getFloat("defaultMpAdd"));
				set.set("lvlMpMod", rset.getFloat("defaultMpMod"));
				set.set("baseHpReg", 1.5);
				set.set("baseMpReg", 0.9);
				set.set("basePAtk", rset.getInt("p_atk"));
				set.set("basePDef", /*classId.isMage()? 77 : 129*/rset.getInt("p_def"));
				set.set("baseMAtk", rset.getInt("m_atk"));
				set.set("baseMDef", rset.getInt("char_templates.m_def"));
				set.set("classBaseLevel", rset.getInt("class_lvl"));
				set.set("basePAtkSpd", rset.getInt("p_spd"));
				set.set("baseMAtkSpd", /*classId.isMage()? 166 : 333*/rset.getInt("char_templates.m_spd"));
				set.set("baseCritRate", rset.getInt("char_templates.critical") / 10);
				set.set("baseRunSpd", rset.getInt("move_spd") * Config.RATE_RUN_SPEED);
				set.set("baseWalkSpd", 0);
				set.set("baseShldDef", 0);
				set.set("baseShldRate", 0);
				set.set("baseAtkRange", 40);

				set.set("spawnX", rset.getInt("x"));
				set.set("spawnY", rset.getInt("y"));
				set.set("spawnZ", rset.getInt("z"));

				L2PcTemplate ct;

				set.set("collision_radius", rset.getDouble("m_col_r"));
				set.set("collision_height", rset.getDouble("m_col_h"));
				// Add-on for females
				set.set("fcollision_radius", rset.getDouble("f_col_r"));
				set.set("fcollision_height", rset.getDouble("f_col_h"));
				ct = new L2PcTemplate(set);

				_templates.put(ct.getClassId().getId(), ct);
			}

			rset.close();
			statement.close();

			_log.info("CharTemplateTable: Loaded " + _templates.size() + " Character Templates.");
		}
		catch (SQLException e)
		{
			_log.fatal("Failed loading char templates", e);
		}

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT classId, itemId, amount, equipped FROM char_creation_items");
			ResultSet rset = statement.executeQuery();

			int classId, itemId, amount;
			boolean equipped;
			while (rset.next())
			{
				classId = rset.getInt("classId");
				itemId = rset.getInt("itemId");
				amount = rset.getInt("amount");
				equipped = rset.getString("equipped").equals("true");

				if (ItemTable.getInstance().getTemplate(itemId) != null)
				{
					if (classId == -1)
					{
						for (L2PcTemplate pct : _templates.values())
							pct.addItem(itemId, amount, equipped);
					}
					else
					{
						L2PcTemplate pct = _templates.get(classId);
						if (pct != null)
							pct.addItem(itemId, amount, equipped);
						else
							_log.warn("char_creation_items: Entry for undefined class, classId: "+classId);
					}
				}
				else
					_log.warn("char_creation_items: No data for itemId: "+itemId+" defined for classId "+classId);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.fatal("Failed loading char creation items.", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (Exception e)
			{
				// nothing
			}
		}
		ClassTreeTable.getInstance();
	}

	public L2PcTemplate getTemplate(ClassId classId)
	{
		return getTemplate(classId.getId());
	}

	public L2PcTemplate getTemplate(int classId)
	{
		return _templates.get(classId);
	}

	public Collection<L2PcTemplate> getAll() {
		return _templates.values();
	}
	public static final String getClassNameById(int classId)
	{
		return CHAR_CLASSES[classId];
	}
}