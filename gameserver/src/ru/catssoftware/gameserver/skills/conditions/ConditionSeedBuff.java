package ru.catssoftware.gameserver.skills.conditions;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.effects.EffectSeed;

public class ConditionSeedBuff extends Condition {

	public final static int WIND = 1287;
	public final static int FIRE = 1285;
	public final static int WATER = 1286;
	private int _type;
	private int _value;
	public ConditionSeedBuff( int type, int value) {
		_type = type;
		_value = value;
	}
	@Override
	boolean testImpl(Env env) {
		L2PcInstance player = env.player.getActingPlayer();
		L2Effect effect = player.getFirstEffect(_type);
		if(effect!=null) {
			if(effect instanceof EffectSeed)  {
				if(((EffectSeed)effect).getForce()>= _value) 
					return true;
			}
		}
		
		return false;
	}

}
