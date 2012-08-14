package ru.catssoftware.gameserver.skills.effects.calc;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;

public class Counterattack {
	public static void vengeanceValue(Env env) {
		// Немного поменяны "местами" переменные (т.к. считаеся отраженный урон)
		// env.player - тот кого атакуют
		// env.target - кто атакует
		if(env.target.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
			env.value = 0;
		else {
			//env.value = env.value * ((double)env.target.getStat().getPAtk(null) / (double)env.player.getStat().getPAtk(null));
			env.value = (int) Formulas.calcPhysDam(env.player, env.target, null, (byte) 0, true, false, env.player.getActiveWeaponInstance().getChargedSoulshot() != L2ItemInstance.CHARGED_NONE);
		}
	}
}
