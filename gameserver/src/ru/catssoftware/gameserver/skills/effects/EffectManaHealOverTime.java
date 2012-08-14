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
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectManaHealOverTime extends L2Effect
{
	public EffectManaHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.MANA_HEAL_OVER_TIME;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;

		double mp = getEffected().getStatus().getCurrentMp();
		double maxmp = getEffected().getMaxMp();
		mp += calc();
		if (mp > maxmp)
		{
			mp = maxmp;
		}
		getEffected().getStatus().setCurrentMp(mp);
		StatusUpdate sump = new StatusUpdate(getEffected().getObjectId());
		sump.addAttribute(StatusUpdate.CUR_MP, (int) mp);
		getEffected().sendPacket(sump);
		return true;
	}
}
