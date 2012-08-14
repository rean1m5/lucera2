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
package ru.catssoftware.gameserver.items.model;

import ru.catssoftware.gameserver.model.L2Skill;

/**
 * @author -Nemesiss-
 */
public class L2ExtractableProductItem
{
	private final Integer[]	_id;
	private final Integer[]	_ammount;
	private final int	_chance;
	private L2Skill _skill;

	public L2ExtractableProductItem(Integer[] id, Integer[] ammount, int chance, L2Skill skill)
	{
		_id = id;
		_ammount = ammount;
		_chance = chance;
		_skill = skill;
	}

	public Integer[] getId()
	{
		return _id;
	}

	public Integer[] getAmmount()
	{
		return _ammount;
	}

	public int getChance()
	{
		return _chance;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}
}