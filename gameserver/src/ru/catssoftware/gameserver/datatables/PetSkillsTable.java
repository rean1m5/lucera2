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
import java.util.Collection;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.L2Summon;


public class PetSkillsTable
{
	private static Logger _log = Logger.getLogger(PetSkillsTable.class.getName());

	private FastMap<Integer, Map<Integer, L2PetSkillLearn>> _skillTrees;
	private static PetSkillsTable _instance;

	public static PetSkillsTable getInstance()
	{
		if (_instance == null)
			_instance = new PetSkillsTable();

		return _instance;
	}

	public void reload()
	{
		_skillTrees.clear();
		_instance = new PetSkillsTable();
	}

	private PetSkillsTable()
	{
		int npcId = 0;
		int count = 0;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			try
			{
				PreparedStatement statement = con.prepareStatement("SELECT id FROM npc WHERE type IN ('L2Pet','L2BabyPet','L2SiegeSummon') ORDER BY id");
				ResultSet petlist = statement.executeQuery();
				Map<Integer, L2PetSkillLearn> map;
				L2PetSkillLearn skillLearn;
				while (petlist.next())
				{
					map = new FastMap<Integer, L2PetSkillLearn>();
					npcId = petlist.getInt("id");
					PreparedStatement statement2 = con.prepareStatement("SELECT minLvl, skillId, skillLvl FROM pets_skills WHERE templateId=? ORDER BY skillId, skillLvl");
					statement2.setInt(1, npcId);
					ResultSet skilltree = statement2.executeQuery();

					while (skilltree.next())
					{
						int id = skilltree.getInt("skillId");
						int lvl = skilltree.getInt("skillLvl");
						int minLvl = skilltree.getInt("minLvl");

						skillLearn = new L2PetSkillLearn(id, lvl, minLvl);
						map.put(SkillTable.getSkillHashCode(id, lvl + 1), skillLearn);
					}
					getPetSkillTrees().put(npcId, map);
					skilltree.close();
					statement2.close();

					count += map.size();
					if (_log.isDebugEnabled())
						_log.info("PetSkillsTable: Skill tree for pet " + npcId + " has " + map.size() + " skills");
				}
				petlist.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.fatal("Error while creating pet skill tree (Pet ID " + npcId + "):", e);
			}
			_log.info("PetSkillsTable: Loaded " + count + " skills.");
		}
		catch (Exception e)
		{
			_log.fatal("Error while loading pet skills tables ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	private Map<Integer, Map<Integer, L2PetSkillLearn>> getPetSkillTrees()
	{
		if (_skillTrees == null)
			_skillTrees = new FastMap<Integer, Map<Integer, L2PetSkillLearn>>();

		return _skillTrees;
	}

	public int getAvailableLevel(L2Summon cha, int skillId)
	{
		int lvl = 0;
		if (!getPetSkillTrees().containsKey(cha.getNpcId()))
			return lvl;

		Collection<L2PetSkillLearn> skills = getPetSkillTrees().get(cha.getNpcId()).values();
		for (L2PetSkillLearn temp : skills)
		{
			if (temp.getId() != skillId)
				continue;

			if (temp.getLevel() == 0)
			{
				if (cha.getLevel() < 70)
				{
					lvl = (cha.getLevel() / 10);
					if (lvl <= 0)
						lvl = 1;
				}
				else
					lvl = (7 + ((cha.getLevel() - 70) / 5));

				// formula usable for skill that have 10 or more skill levels
				lvl = Math.min(lvl, SkillTable.getInstance().getMaxLevel(temp.getId()));
				break;
			}
			else if (temp.getMinLevel() <= cha.getLevel())
			{
				if (temp.getLevel() > lvl)
					lvl = temp.getLevel();
			}
		}

		return lvl;
	}

	public FastList<Integer> getAvailableSkills(L2Summon cha)
	{
		FastList<Integer> skillIds = new FastList<Integer>();
		if (!getPetSkillTrees().containsKey(cha.getNpcId()))
			return null;

		Collection<L2PetSkillLearn> skills = getPetSkillTrees().get(cha.getNpcId()).values();
		for (L2PetSkillLearn temp : skills)
		{
			if (skillIds.contains(temp.getId()))
				continue;

			skillIds.add(temp.getId());
		}

		return skillIds;
	}

	public final class L2PetSkillLearn
	{
		private final int _id;
		private final int _level;
		private final int _minLevel;

		public L2PetSkillLearn(int id, int lvl, int minLvl)
		{
			_id = id;
			_level = lvl;
			_minLevel = minLvl;
		}

		public int getId()
		{
			return _id;
		}

		public int getLevel()
		{
			return _level;
		}

		public int getMinLevel()
		{
			return _minLevel;
		}
	}
}