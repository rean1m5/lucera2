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
package ru.catssoftware.gameserver.skills.conditions;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.base.PlayerState;
import ru.catssoftware.gameserver.skills.Env;

/**
 * @author mkizub
 */
public final class ConditionPlayerState extends Condition
{
	private final PlayerState _check;
	private final boolean _required;

	public ConditionPlayerState(PlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	boolean testImpl(Env env)
	{
		L2PcInstance player;
		switch (_check)
		{
		case RESTING:
			if (env.player.isPlayer())
				return ((L2PcInstance) env.player).isSitting() == _required;
			break;
		case MOVING:
			return env.player.isMoving() == _required;
		case INPVP:
			if(env.player.getPlayer()!=null)
				if(_required)
					return env.player.getPlayer().getPvpFlag()>0;
				else 
					return env.player.getPlayer().getPvpFlag() == 0;
			break;
		case RUNNING:
			return (env.player.isMoving() && env.player.isRunning()) == _required;
		case WALKING:
			return (env.player.isMoving() && !env.player.isRunning()) == _required;
		case BEHIND:
			return env.player.isBehindTarget() == _required;
		case FRONT:
			return env.player.isInFrontOfTarget() == _required;
		case CHAOTIC:
			player = env.player.getPlayer();
			if (player != null)
				return (player.getKarma() > 0) == _required;
			break;
		case OLYMPIAD:
			player = env.player.getPlayer();
			if (player != null)
				return player.isInOlympiadMode() == _required;
			break;
		case FLYING:
			return env.player.isFlying() == _required;
		}

		return !_required;
	}
}