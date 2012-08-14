package ru.catssoftware.gameserver.skills;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2CubicInstance;

public final class Env
{
	public L2Character		player;
	public L2CubicInstance	cubic;
	public L2Character		target;
	public L2ItemInstance	item;
	public L2Skill			skill;
	public L2Effect			effect;
	public Object			object;
	public double			value;
	public double			baseValue;
	public boolean			skillMastery = false;
}