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
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author Faror
 */
public final class EffectPhoenixBless extends L2Effect
{
	public EffectPhoenixBless(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PHOENIX_BLESSING;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2PlayableInstance)
		{
			((L2PlayableInstance) getEffected()).startPhoenixBlessing();
			return true;
		}
		return false;
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		((L2PlayableInstance) getEffected()).stopPhoenixBlessing(this);
	}

	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}