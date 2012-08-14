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
package ru.catssoftware.gameserver.model.actor.knownlist;

import java.util.Collection;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;


public class AttackableKnownList extends NpcKnownList
{
	public AttackableKnownList(L2Attackable activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;

		// Remove the L2Object from the _aggrolist of the L2Attackable
		if (object != null && object instanceof L2Character)
			getActiveChar().getAggroList().remove(object);

		// Set the L2Attackable Intention to AI_INTENTION_IDLE
		Collection<L2PcInstance> known = getKnownPlayers().values();

		//FIXME: This is a temporary solution
		L2CharacterAI ai = getActiveChar().getAI();
		if (ai != null && (known == null || known.isEmpty()))
			ai.setIntention(CtrlIntention.AI_INTENTION_IDLE);

		return true;
	}

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
	@Override
	public L2Attackable getActiveChar()
	{
		return (L2Attackable)_activeChar;
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if (getActiveChar().getAggroListRP().get(object) != null)
			return 3000;
		return Math.min(2200, 2 * getDistanceToWatchObject(object));
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FolkInstance || !(object instanceof L2Character))
			return 0;

		if (object instanceof L2PlayableInstance)
			return 1500;

		if (getActiveChar().getAggroRange() > getActiveChar().getFactionRange())
			return getActiveChar().getAggroRange();

		if (getActiveChar().getFactionRange() > 300)
			return getActiveChar().getFactionRange();

		return 300;
	}
}