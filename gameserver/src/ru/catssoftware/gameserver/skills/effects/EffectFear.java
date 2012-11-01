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

import javolution.util.FastMap;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

import java.util.Map;

//import java.util.logging.Logger;


/**
 * @author littlecrow
 *
 * Implementation of the Fear Effect
 */
public final class EffectFear extends L2Effect
{
	public static final int	FEAR_RANGE	= 500;
	//protected static Logger				_log				= Logger.getLogger(EffectFear.class.getName());
	private static Map<Integer, float []> _deltas = new FastMap<Integer, float[]>();

	public EffectFear(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		// Fear skills cannot be used L2Pcinstance to L2Pcinstance.
		// Heroic Dread, Curse: Fear, Fear, Horror, Sword Symphony, Word of Fear and Mass Curse Fear are the exceptions.
		if (getEffected().isPlayer() && getEffector().isPlayer())
		{
			switch (getSkill().getId())
			{
			case 65:
			case 98:
			case 1092:
			case 1169:
			case 1272:
			case 1376:
			case 1381:
				// all ok
				break;
			default:
				return false;
			}
		}

		if (getEffected() instanceof L2FolkInstance || getEffected() instanceof L2SiegeGuardInstance || getEffected() instanceof L2SiegeFlagInstance
				|| getEffected() instanceof L2SiegeSummonInstance || getEffected() instanceof L2FortSiegeGuardInstance
				|| getEffected() instanceof L2FortCommanderInstance)
			return false;

		if (!getEffected().isAfraid())
		{
			float _dX = getEffector().getX() - getEffected().getX();
			float _dY = getEffector().getY() - getEffected().getY();
			
			if (_dX == 0)
			{
				_dX = 0;
				if (_dY > 0)
					_dY = -1;
				else
					_dY = 1;
			}
			else if (_dY == 0)
			{
				_dY = 0;
				if (_dX > 0)
					_dX = -1;
				else
					_dX = 1;
			}
			else if (_dX > 0 && _dY > 0)
			{
				//_log.warning("Situation A-NW ("+_dX+";"+_dY+")");
				if (_dX > _dY)
				{
					_dY = -1*_dY/_dX;
					_dX = -1;
				}
				else
				{
					_dX = -1*_dX/_dY;
					_dY = -1;
				}
			}
			else if (_dX > 0 && _dY < 0)
			{
				//_log.warning("Situation B-SW ("+_dX+";"+_dY+")");
				if (_dX > (-1*_dY))
				{
					_dY = -1*(_dY/_dX);
					_dX = -1;
				}
				else
				{
					_dX = _dX/_dY;
					_dY = 1;
				}
			}
			else if (_dX < 0 && _dY > 0)
			{
				//_log.warning("Situation C-NE ("+_dX+";"+_dY+")");
				if ((-1*_dX) > _dY)
				{
					_dY = _dY/_dX;
					_dX = 1;
				}
				else
				{
					_dX = -1*(_dX/_dY);
					_dY = -1;
				}
			}
			else if (_dX < 0 && _dY < 0)
			{
				//_log.warning("Situation D-SE ("+_dX+";"+_dY+")");
				if (_dX > _dY)
				{
					_dY = _dY/_dX;
					_dX = 1;
				}
				else
				{
					_dX = _dX/_dY;
					_dY = 1;
				}
			}
			else if (_dX == 0 && _dY == 0)
			{
				//_log.warning("Situation X");
				_dX = -1;
				_dY = -1;
			}
			_deltas.put(getEffected().getObjectId(), new float[] {_dX,_dY});
			getEffected().startFear(getEffector());
			onActionTime();
			return true;
		}
		return false;
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		getEffected().stopFear(this);
		getEffected().setTarget(null);
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		_deltas.remove(getEffected().getObjectId());
	}

	@Override
	public boolean onActionTime()
	{
		int posX = getEffected().getX();
		int posY = getEffected().getY();
		int posZ = getEffected().getZ();
		float [] delta = _deltas.get(getEffected().getObjectId());
		float _dX = -1;
		float _dY = -1;
		if(delta!=null) {
			_dX = delta[0];
			_dY = delta[1];
		}
		//_log.warning("Start Position on Fear. X: " + posX + "  Y: " + posY);
		if (_dX != 0)
			posX += _dX * FEAR_RANGE;
		if (_dY != 0)
			posY += _dY * FEAR_RANGE;
		//_log.warning("Next Position on Fear. X: " + posX + "  Y: " + posY);
		Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ, getEffected().getInstanceId());
		if (!(getEffected() instanceof L2PetInstance))
			getEffected().setRunning();
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(destiny.getX(), destiny.getY(), destiny.getZ(), 0));
		
		// Give damage if "val" > 0
		double damage = calc();
		if (damage != 0)
			getEffected().reduceCurrentHp(damage, getEffector(), true, true, getSkill());
		return true;
	}
}