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

import java.util.ArrayList;


import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2Skill;

/**
 * @author G1ta0
 */
public final class HeroSkillTable
{
	private static final Logger _log = Logger.getLogger(HeroSkillTable.class);

	private static final int[] HERO_SKILL_IDS = { 395, 396, 1374, 1375, 1376 };

	private static HeroSkillTable _instance;

	public static HeroSkillTable getInstance()
	{
		if (_instance == null)
			_instance = new HeroSkillTable();

		return _instance;
	}

	private final ArrayList<L2Skill> _heroSkills = new ArrayList<L2Skill>();

	private HeroSkillTable()
	{
		for (int skillId : HERO_SKILL_IDS)
			_heroSkills.add(SkillTable.getInstance().getInfo(skillId, 1));

		_log.info("HeroSkillTable: Initialized.");
	}

	public Iterable<L2Skill> getHeroSkills()
	{
		return _heroSkills;
	}

	public static boolean isHeroSkill(int skillId)
	{
		return ArrayUtils.contains(HERO_SKILL_IDS, skillId);
	}
}