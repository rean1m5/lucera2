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
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeSummonInstance;
import ru.catssoftware.gameserver.network.serverpackets.StartRotation;
import ru.catssoftware.gameserver.network.serverpackets.StopRotation;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author decad
 * @rework by L2CatsSoftware DevTeam
 * @update Ro0TT (12.06.2012)
 */

public final class EffectBluff extends L2Effect
{
	public EffectBluff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLUFF;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2FolkInstance)
			return false;
		if (getEffected() instanceof L2NpcInstance && ((L2NpcInstance)getEffected()).getNpcId() == 35062)
			return false;
		if (getEffected() instanceof L2SiegeSummonInstance)
			return false;

		getEffected().setTarget(null);
		getEffected().abortAttack();
		getEffected().abortCast();
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, getEffector());
		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(),1, 0xFFFF));
		getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 0xFFFF));
		getEffected().setHeading(getEffector().getHeading());
		getEffected().startStunning(getEffector());
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopStunning(this);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
