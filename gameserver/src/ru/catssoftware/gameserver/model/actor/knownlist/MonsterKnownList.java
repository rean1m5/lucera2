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

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class MonsterKnownList extends AttackableKnownList
{
	public MonsterKnownList(L2MonsterInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(getKnownObjects().containsKey(object.getObjectId()))
			return true;

		if (!super.addKnownObject(object, dropper))
			return false;

		// Set the L2MonsterInstance Intention to AI_INTENTION_ACTIVE if the state was AI_INTENTION_IDLE
		if (object instanceof L2PcInstance && getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
		return true;
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;

		if (!(object instanceof L2Character))
			return true;

		if (getActiveChar().hasAI())
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object); // Notify the L2MonsterInstance AI with EVT_FORGET_OBJECT

		if (getActiveChar().isVisible() && getKnownPlayers().isEmpty())
			getActiveChar().clearAggroList(); // Clear the _aggroList of the L2MonsterInstance

		return true;
	}

	@Override
	public final L2MonsterInstance getActiveChar()
	{
		return (L2MonsterInstance)_activeChar;
	}
}