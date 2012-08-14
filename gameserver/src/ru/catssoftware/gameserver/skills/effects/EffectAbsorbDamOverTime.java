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

import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectAbsorbDamOverTime extends L2Effect
{
	public EffectAbsorbDamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DMG_OVER_TIME;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;

		double damage = calc();
		if (damage >= getEffected().getStatus().getCurrentHp())
		{
			// For DOT skills that will not kill effected player.
			if (!getSkill().killByDOT())
				damage = getEffected().getStatus().getCurrentHp() - 1;
		}

		boolean awake = !(getEffected() instanceof L2Attackable) && !(getSkill().getTargetType() == SkillTargetType.TARGET_SELF && getSkill().isToggle());

		getEffected().reduceCurrentHp(damage, getEffector(), awake, true, getSkill());

		int maxCanAbsorb = (int) (getEffector().getMaxHp() - getEffector().getStatus().getCurrentHp());
		if (damage > maxCanAbsorb)
			damage = maxCanAbsorb; // Can't absorb more than max hp
		if (damage > 0)
			getEffector().getStatus().increaseHp(damage);

		return true;
	}
}