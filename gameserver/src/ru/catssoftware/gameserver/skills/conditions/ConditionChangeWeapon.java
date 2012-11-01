package ru.catssoftware.gameserver.skills.conditions;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.item.L2Weapon;

class ConditionChangeWeapon extends Condition
{
	private final boolean	_required;

	public ConditionChangeWeapon(boolean required)
	{
		_required = required;
	}

	@Override
	boolean testImpl(Env env)
	{
		if (!(env.player.isPlayer()))
			return false;

		if (_required)
		{
			L2Weapon weaponItem = env.player.getActiveWeaponItem();

			if (weaponItem == null)
				return false;

			if (weaponItem.getChangeWeaponId() == 0)
				return false;

			if (((L2PcInstance)env.player).isEnchanting())
				return false;
		}
		return true;
	}
}