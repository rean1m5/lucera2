package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;


/**
 * Author: M-095
 * Version: 1.0
 */

public final class EffectTouchofDeath extends L2Effect
{
	public EffectTouchofDeath(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() == null)
			return true;

		// Собираем все эффекты чара
		L2Effect[] effects = getEffected().getAllEffects();
		double count = 0;
		
		// Перебираем собранные эфекты
		for (L2Effect e : effects)
		{
			// Null эфекты и скилы нам не надо, пропускаем
			if (e == null || e.getSkill() == null)
				continue;

			// Список ID, которые нельзя отменять
			switch (e.getSkill().getId())
			{
				case 110:
				case 111:
				case 1323:
				case 1325:
				case 4082:
				case 4215:
				case 4515:
				case 5182:
					continue;
			}

			// Если скил подходит по типу, то отменяем его
			switch (e.getSkill().getSkillType())
			{
				case BUFF:
				case HEAL_PERCENT:
				case REFLECT:
				case COMBATPOINTHEAL:
					count += 1;
					e.exit();
			}

			// 5 отмененных эфекта это предел
			if (count > 5)
				break;
		}
		return true;
	}

	@Override
	public void onExit()
	{
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}