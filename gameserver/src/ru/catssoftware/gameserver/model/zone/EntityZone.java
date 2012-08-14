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
package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.entity.Siegeable;

public abstract class EntityZone extends L2DefaultZone
{
	protected Siegeable	_entity;

	@Override
	protected void onEnter(L2Character character)
	{
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		super.onExit(character);
	}
}