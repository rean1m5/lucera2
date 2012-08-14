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
package ru.catssoftware.gameserver.templates.item;

import ru.catssoftware.util.StatsSet;

/**
 * This class is dedicated to the management of EtcItem.
 *
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:30:10 $
 */
public final class L2EtcItem extends L2Item
{
	private final String[] _skill;

	/**
	 * Constructor for EtcItem.
	 *
	 * @see L2Item constructor
	 * @param type : L2EtcItemType designating the type of object Etc
	 * @param set : StatsSet designating the set of couples (key,value) for
	 *            description of the Etc
	 */
	public L2EtcItem(L2EtcItemType type, StatsSet set)
	{
		super(type, set);
		_skill = set.getString("skill").split(";");
	}

	/**
	 * Returns the type of Etc Item
	 *
	 * @return L2EtcItemType
	 */
	@Override
	public L2EtcItemType getItemType()
	{
		return (L2EtcItemType) super._type;
	}

	/**
	 * Returns if the item is consumable
	 *
	 * @return boolean
	 */
	@Override
	public final boolean isConsumable()
	{
		return ((getItemType() == L2EtcItemType.SHOT) || (getItemType() == L2EtcItemType.POTION));
	}

	/**
	 * Returns the ID of the Etc item after applying the mask.
	 *
	 * @return int : ID of the EtcItem
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	public String[] getSkills()
	{
		return _skill;
	}
}