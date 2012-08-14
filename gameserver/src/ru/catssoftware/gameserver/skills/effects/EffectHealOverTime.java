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
package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExRegMax;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectHealOverTime extends L2Effect
{
	public EffectHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.HEAL_OVER_TIME;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2PcInstance)
			getEffected().sendPacket(new ExRegMax(calc(), getTotalCount() * getPeriod(), getPeriod()));
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;

		if (getEffected() instanceof L2DoorInstance)
			return false;

		double hp = getEffected().getStatus().getCurrentHp();
		double maxhp = getEffected().getMaxHp();
		hp += calc();
		if (hp > maxhp)
		{
			hp = maxhp;
		}
		getEffected().getStatus().setCurrentHp(hp);
		StatusUpdate suhp = new StatusUpdate(getEffected().getObjectId());
		suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
		getEffected().sendPacket(suhp);
		return true;
	}
}
