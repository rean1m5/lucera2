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
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 *
 * @author Ahmed
 *
 * This is the Effect support for spoil.
 *
 * This was originally done by _drunk_
 */
public final class EffectSpoil extends L2Effect
{
	public EffectSpoil(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SPOIL;
	}

	@Override
	public boolean onStart()
	{
		if (!(getEffector() instanceof L2PcInstance))
			return false;

		if (!(getEffected() instanceof L2MonsterInstance))
			return false;

		L2MonsterInstance target = (L2MonsterInstance) getEffected();

		if (target.isSpoil())
		{
			getEffector().sendPacket(SystemMessageId.ALREADY_SPOILED);
			return false;
		}

		// SPOIL SYSTEM by Lbaldi
		boolean spoil = false;
		if (!target.isDead())
		{
			spoil = Formulas.calcMagicSuccess(getEffector(), target, getSkill());

			if (spoil)
			{
				target.setSpoil(true);
				target.setIsSpoiledBy(getEffector().getObjectId());
				getEffector().sendPacket(SystemMessageId.SPOIL_SUCCESS);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
				sm.addCharName(target);
				sm.addSkillName(this);
				getEffector().sendPacket(sm);
			}
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
		}
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}