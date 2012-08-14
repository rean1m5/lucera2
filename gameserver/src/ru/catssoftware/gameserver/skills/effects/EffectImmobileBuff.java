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
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.effects.EffectBuff;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author mkizub
 */
public final class EffectImmobileBuff extends EffectBuff
{
	public EffectImmobileBuff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	// Special constructor to steal this effect
	public EffectImmobileBuff(Env env, L2Effect effect)
	{
		super(env, effect);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		getEffector().setIsImmobilized(true);
		return true;
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		getEffector().setIsImmobilized(false);
	}

	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		getEffector().setIsImmobilized(false);
		return false;
	}
}
