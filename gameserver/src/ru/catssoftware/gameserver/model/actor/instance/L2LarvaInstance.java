package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2LarvaInstance extends L2MonsterInstance {

	public L2LarvaInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
	}
	@Override
	public boolean canReduceHp(double damage, L2Character attacker) {
		return getCurrentHp() - damage > 10;
	}


}
