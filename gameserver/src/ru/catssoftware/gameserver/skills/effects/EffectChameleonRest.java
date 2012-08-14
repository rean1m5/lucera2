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

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public final class EffectChameleonRest extends L2Effect
{
	public EffectChameleonRest(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.RELAXING;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		L2Character effected = getEffected();
		if (effected instanceof L2PcInstance)
		{
			setChameleon(true);
			((L2PcInstance) effected).setSilentMoving(true);
			((L2PcInstance) effected).sitDown();
		}
		else
			effected.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		return true;
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
		setChameleon(false);

		L2Character effected = getEffected();
		if (effected instanceof L2PcInstance)
			((L2PcInstance) effected).setSilentMoving(false);
	}

	@Override
	public boolean onActionTime()
	{
		L2Character effected = getEffected();
		boolean retval = true;

		if (effected.isDead())
			retval = false;

		// Only cont skills shouldn't end
		if (getSkill().getSkillType() != L2SkillType.CONT)
			return false;

		if (effected instanceof L2PcInstance)
		{
			if (!((L2PcInstance) effected).isSitting())
				retval = false;
		}

		double manaDam = calc();

		if (manaDam > effected.getStatus().getCurrentMp())
		{
			effected.sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			return false;
		}

		if (!retval)
			setChameleon(retval);
		else
			effected.reduceCurrentMp(manaDam);

		return retval;
	}

	private void setChameleon(boolean val)
	{
		L2Character effected = getEffected();
		if (effected instanceof L2PcInstance)
			((L2PcInstance) effected).setRelax(val);
	}
}