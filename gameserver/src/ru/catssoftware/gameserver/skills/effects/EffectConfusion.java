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

import java.util.List;

import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.tools.random.Rnd;

/**
 * @author littlecrow
 *
 * Implementation of the Confusion Effect
 */
public final class EffectConfusion extends L2Effect
{

	private static final Logger	_log	= Logger.getLogger(EffectConfusion.class);

	public EffectConfusion(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CONFUSION;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		getEffected().startConfused();
		onActionTime();
		return true;
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		getEffected().stopConfused(this);
	}

	@Override
	public boolean onActionTime()
	{
		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug(getEffected());
		List<L2Character> targetList = new FastList<L2Character>();

		// Getting the possible targets

		for (L2Object obj : getEffected().getKnownList().getKnownObjects().values())
		{
			if ((obj instanceof L2Character) && (obj != getEffected()))
				targetList.add((L2Character) obj);
		}
		// if there is no target, exit function
		if (targetList.size() == 0)
		{
			return true;
		}

		// Choosing randomly a new target
		int nextTargetIdx = Rnd.nextInt(targetList.size());
		L2Object target = targetList.get(nextTargetIdx);

		// Attacking the target
		getEffected().setTarget(target);
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);

		return true;
	}
}