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
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.knownlist.FriendlyMobKnownList;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class represents Friendly Mobs lying over the world.
 * These friendly mobs should only attack players with karma > 0
 * and it is always aggro, since it just attacks players with karma
 *
 * @version $Revision: 1.20.4.6 $ $Date: 2005/07/23 16:13:39 $
 */
public class L2FriendlyMobInstance extends L2Attackable
{
	public L2FriendlyMobInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
	}

	@Override
	public final FriendlyMobKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new FriendlyMobKnownList(this);

		return (FriendlyMobKnownList) _knownList;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker.isPlayer())
			return ((L2PcInstance) attacker).getKarma() > 0;
		return false;
	}

	@Override
	public boolean isAggressive()
	{
		return true;
	}
}
