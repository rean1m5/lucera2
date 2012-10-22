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
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @author mkizub
 */
public class EffectBuff extends L2Effect
{
	public EffectBuff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	// Special constructor to steal this effect
	public EffectBuff(Env env, L2Effect effect)
	{
		super(env, effect);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	@Override
	protected boolean onStart()
	{

		if (getSkill().getId() == 350 || getSkill().getId() == 351)
		{
			listner = new checkChangeWeapon();
			getEffected().getInventory().addPaperdollListener(listner);
		}
		return true;
	}

	@Override
	protected void onExit()
	{
		if (listner != null)
			getEffected().getInventory().removePaperdollListener(listner);
	}

	private checkChangeWeapon listner;
	class checkChangeWeapon implements Inventory.PaperdollListener
	{

		@Override
		public void notifyEquiped(int slot, L2ItemInstance inst) {

		}

		@Override
		public void notifyUnequiped(int slot, L2ItemInstance inst) {
			if (inst.getItemType().equals(L2WeaponType.NONE))
				exit();
		}
	}

	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}
