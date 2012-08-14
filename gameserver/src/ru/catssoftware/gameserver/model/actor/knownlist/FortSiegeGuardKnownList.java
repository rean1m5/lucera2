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

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2FortSiegeGuardInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Fort;

public class FortSiegeGuardKnownList extends AttackableKnownList
{
	public FortSiegeGuardKnownList(L2FortSiegeGuardInstance activeChar)
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

		Fort fort = getActiveChar().getFort();
		// Check if siege is in progress 
		if (fort != null && fort.getSiege().getIsInProgress())
		{
			L2PcInstance player = null;
			if (object instanceof L2PcInstance)
				player = (L2PcInstance) object;
			else if (object instanceof L2Summon)
				player = ((L2Summon) object).getOwner();

			// Check if player is not the defender
			if (player != null && (player.getClan() == null || fort.getSiege().getAttackerClan(player.getClan()) != null))
			{
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
			}
		}
		return true;
	}

	@Override
	public final L2FortSiegeGuardInstance getActiveChar()
	{
		return (L2FortSiegeGuardInstance)_activeChar;
	}
}