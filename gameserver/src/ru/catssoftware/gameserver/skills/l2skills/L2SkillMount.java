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
package ru.catssoftware.gameserver.skills.l2skills;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.util.StatsSet;

public class L2SkillMount extends L2Skill
{
	private int _npcId;
	private int _itemId;

	public L2SkillMount(StatsSet set)
	{
		super(set);
		_npcId = set.getInteger("npcId", 0);
		_itemId = set.getInteger("itemId", 0);
	}

	@Override
	public void useSkill(L2Character caster, L2Character... targets)
	{
		if (!(caster instanceof L2PcInstance))
			return;

		L2PcInstance activePlayer = (L2PcInstance)caster;

		if (!FloodProtector.tryPerformAction(activePlayer, Protected.ITEMPETSUMMON))
			return;

		// Dismount Action
		if (_npcId == 0)
		{
			activePlayer.dismount();
			return;
		}

		if (activePlayer.isSitting())
		{
			activePlayer.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}

		if (activePlayer.inObserverMode())
			return;
		
		if (activePlayer.isInOlympiadMode())
		{
			activePlayer.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if (activePlayer.getPet() != null || activePlayer.isMounted())
		{
			activePlayer.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return;
		}
		
		if (activePlayer.isAttackingNow() || activePlayer.isCursedWeaponEquipped())
		{
			activePlayer.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT);
			return;
		}
		
		activePlayer.mount(_npcId, _itemId, false);
	}
}