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
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.util.Util;

public final class PcStatus extends CharStatus
{
	public PcStatus(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	void reduceHp0(double value, L2Character attacker, boolean awake, boolean isDOT)
	{
		double realValue = value;

		if (attacker != null && attacker != getActiveChar())
		{
			// Check and calculate transfered damage
			L2Summon summon = getActiveChar().getPet();

			if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
			{
				int tDmg = (int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

				// Only transfer dmg up to current HP, it should not be killed
				if (summon.getStatus().getCurrentHp() < tDmg)
					tDmg = (int) summon.getStatus().getCurrentHp() - 1;

				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					value -= tDmg;
					realValue = value;
				}
			}

			if (attacker instanceof L2PlayableInstance)
			{
				if (getCurrentCp() >= value)
				{
					setCurrentCp(getCurrentCp() - value); // Set Cp to diff of Cp vs value
					value = 0; // No need to subtract anything from Hp
				}
				else
				{
					value -= getCurrentCp(); // Get diff from value vs Cp; will apply diff to Hp
					setCurrentCp(0); // Set Cp to 0
				}
			}
		}

		super.reduceHp0(value, attacker, awake, isDOT);

		if (!getActiveChar().isDead() && getActiveChar().isSitting())
			getActiveChar().standUp();

		if (getActiveChar().isFakeDeath())
			getActiveChar().stopFakeDeath(null);

		if (attacker != getActiveChar() && realValue > 0)
		{
			SystemMessage smsg = new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG);
			smsg.addCharName(attacker);
			smsg.addNumber((int)realValue);
			getActiveChar().sendPacket(smsg);
		}
	}

	@Override
	public L2PcInstance getActiveChar()
	{
		return (L2PcInstance) _activeChar;
	}

	public void restoreHpMp() {
		setCurrentHpMp(_activeChar.getMaxHp(), _activeChar.getMaxMp());
		
	}
}