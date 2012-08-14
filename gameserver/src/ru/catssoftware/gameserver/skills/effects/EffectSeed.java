package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public class EffectSeed extends L2Effect {

	public EffectSeed(Env env, EffectTemplate template) {
		super(env, template);
	}
	private enum Seeds {
		SeedOfWind,
		SeedOfWater,
		SeedOfFire
	};
	@Override
	protected boolean onStart()
	{
		L2PcInstance player = getEffected().getActingPlayer();
		if(player==null)
			return false;
		player._seeds[Seeds.valueOf(getStackType()).ordinal()]++;
		return true;
	}
	
	@Override
	protected void onExit()
	{
		L2PcInstance player = getEffected().getActingPlayer();
		if(player==null)
			return;
		if(_exitEffector==null || _exitEffector==getEffector()) {
			player._seeds[Seeds.valueOf(getStackType()).ordinal()] = 0;
		}
	}
	public int getForce() {
		L2PcInstance player = getEffected().getActingPlayer();
		if(player==null)
			return 0;
		return player._seeds[Seeds.valueOf(getStackType()).ordinal()];
	}
	@Override
	public L2EffectType getEffectType() {
		return L2EffectType.BUFF;
	}
	@Override
	public boolean onActionTime() {
		return false;
	}
	

}
