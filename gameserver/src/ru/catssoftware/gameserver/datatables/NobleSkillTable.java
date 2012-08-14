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
 * @author -Nemesiss-
 */
public final class NobleSkillTable
{
	private static final Logger _log = Logger.getLogger(NobleSkillTable.class);

	private static final int[] NOBLE_SKILL_IDS = { 325, 326, 327, 1323, 1324, 1325, 1326, 1327 };

	private static NobleSkillTable _instance;

	public static NobleSkillTable getInstance()
	{
		if (_instance == null)
			_instance = new NobleSkillTable();

		return _instance;
	}

	private final ArrayList<L2Skill> _nobleSkills = new ArrayList<L2Skill>();

	private NobleSkillTable()
	{
		for (int skillId : NOBLE_SKILL_IDS)
			_nobleSkills.add(SkillTable.getInstance().getInfo(skillId, 1));

		CrownTable.getInstance();
		_log.info("NobleSkillTable: Initialized.");
	}

	public Iterable<L2Skill> getNobleSkills()
	{
		return _nobleSkills;
	}

	public static boolean isNobleSkill(int skillId)
	{
		return ArrayUtils.contains(NOBLE_SKILL_IDS, skillId);
	}
}