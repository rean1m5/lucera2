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
package ru.catssoftware.gameserver.model.actor.stat;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.skills.Stats;

public class NpcStat extends CharStat
{
	public NpcStat(L2NpcInstance activeChar)
	{
		super(activeChar);

		setLevel(getActiveChar().getTemplate().getLevel());
	}

	@Override
	public L2NpcInstance getActiveChar()
	{
		return (L2NpcInstance) _activeChar;
	}

	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, getActiveChar().getTemplate().getBaseHpMax() * (getActiveChar().isChampion() ? Config.CHAMPION_HP : 1), null, null);
	}
}