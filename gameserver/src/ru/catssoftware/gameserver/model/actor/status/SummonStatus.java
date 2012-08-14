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
package ru.catssoftware.gameserver.model.actor.status;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public final class SummonStatus extends CharStatus
{
	public SummonStatus(L2SummonInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	void reduceHp0(double value, L2Character attacker, boolean awake, boolean isDOT)
	{
		super.reduceHp0(value, attacker, awake, isDOT);

		SystemMessage sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		sm.addCharName(attacker);
		sm.addNumber((int) value);
		getActiveChar().getOwner().sendPacket(sm);
	}

	@Override
	public L2SummonInstance getActiveChar()
	{
		return (L2SummonInstance) super.getActiveChar();
	}
}