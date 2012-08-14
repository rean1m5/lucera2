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

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2EffectPointInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @authors Forsaiken, Sami
 */

public final class EffectSignetNoise extends L2Effect
{
	private L2EffectPointInstance	_actor;

	public EffectSignetNoise(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SIGNET_GROUND;
	}

	@Override
	public boolean onStart()
	{
		_actor = (L2EffectPointInstance) getEffected();
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		if (getCount() == getTotalCount() - 1)
			return true; // do nothing first time

		for (L2Character target : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (target == null)
				continue;

			L2Effect[] effects = target.getAllEffects();
			if (effects != null)
			{
				for (L2Effect effect : effects)
				{
					if (effect.getSkill().isDance() || effect.getSkill().isSong())
						effect.exit();
				}
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}
