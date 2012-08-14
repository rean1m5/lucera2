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

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2EffectPointInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author Forsaiken
 */

public final class EffectSignetAntiSummon extends L2Effect
{
	private L2EffectPointInstance	_actor;

	public EffectSignetAntiSummon(Env env, EffectTemplate template)
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
		int mpConsume = getSkill().getMpConsume();

		for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (cha == null)
				continue;

			if (cha instanceof L2PlayableInstance)
			{
				L2PcInstance owner = null;

				if (cha instanceof L2Summon)
					owner = ((L2Summon) cha).getOwner();
				else
					owner = (L2PcInstance) cha;

				if (owner != null && owner.getPet() != null)
				{
					if (mpConsume > getEffector().getStatus().getCurrentMp())
					{
						getEffector().sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
						return false;
					}

					getEffector().reduceCurrentMp(mpConsume);

					owner.getPet().unSummon(owner);
					owner.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
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
