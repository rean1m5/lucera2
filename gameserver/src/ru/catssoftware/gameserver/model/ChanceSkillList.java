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
package ru.catssoftware.gameserver.model;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillLaunched;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import javolution.util.FastMap;


/**
 * @author  kombat/crion
 */
public class ChanceSkillList extends FastMap<L2Skill, ChanceCondition>
{
	private static final long	serialVersionUID	= -2542073222059976854L;
	private L2Character			_owner;

	public ChanceSkillList(L2Character owner)
	{
		super();
		setShared(true);
		_owner = owner;
	}

	public L2Character getOwner()
	{
		return _owner;
	}

	public void setOwner(L2Character owner)
	{
		_owner = owner;
	}

	public void onAttack(L2Object target) {
		int event = ChanceCondition.EVT_ON_ATTACK;
		if(target instanceof L2Character) {
			onEvent(event, (L2Character)target);
		}
	}
	public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_ATTACKED_CRIT;
		}
		else
		{
			event = ChanceCondition.EVT_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_CRIT;
		}

		onEvent(event, target);
	}

	public void onSkillHit(L2Character target, boolean ownerWasHit, boolean wasMagic, boolean wasOffensive)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_HIT_BY_SKILL;
			if (wasOffensive)
			{
				event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
				event |= ChanceCondition.EVT_ATTACKED;
			}
			else
				event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
		}
		else
		{
			event = ChanceCondition.EVT_CAST;
			event |= wasMagic ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
			event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
		}

		onEvent(event, target);
	}
	public void onExit()
	{
		onEvent(ChanceCondition.EVT_ON_EXIT, _owner);
	}	
	public void onEvadedHit(L2Character attacker)
	{
		onEvent(ChanceCondition.EVT_EVADED_HIT, attacker);
	}

	public void onEvent(int event, L2Character target)
	{
		if(_owner.isDead())
			return;
		for (FastMap.Entry<L2Skill, ChanceCondition> e = head(), end = tail(); (e = e.getNext()) != end;)
		{
			if (e.getValue() != null && e.getValue().trigger(event))
				makeCast(e.getKey(), target);
		}
	}

	private void makeCast(L2Skill skill, L2Character target)
	{
		try
		{
			if (skill.getWeaponDependancy(_owner, false))
			{
				if (skill.triggerAnotherSkill()) //should we use this skill or this skill is just referring to another one ...
				{
					skill = SkillTable.getInstance().getInfo(skill.getTriggeredId(), skill.getTriggeredLevel());
					if (skill == null)
						return;
				}

				L2Character[] targets = skill.getTargetList(_owner, false, target);
				if (targets != null && targets.length > 0)
				{
					_owner.broadcastPacket(new MagicSkillLaunched(_owner, skill.getDisplayId(), skill.getLevel(), skill.isPositive(), targets));
					_owner.broadcastPacket(new MagicSkillUse(_owner, targets[0], skill.getDisplayId(), skill.getLevel(), 0, 0, skill.isPositive()));

					// Launch the magic skill and calculate its effects
					SkillHandler.getInstance().getSkillHandler(skill.getSkillType()).useSkill(_owner, skill, targets);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}