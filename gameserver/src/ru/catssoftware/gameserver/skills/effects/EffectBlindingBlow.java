package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeSummonInstance;
import ru.catssoftware.gameserver.network.serverpackets.StartRotation;
import ru.catssoftware.gameserver.network.serverpackets.StopRotation;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public final class EffectBlindingBlow extends L2Effect
{
	public EffectBlindingBlow(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLUFF;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2FolkInstance)
			return false;
		if (getEffected() instanceof L2NpcInstance && ((L2NpcInstance)getEffected()).getNpcId() == 35062)
			return false;
		if (getEffected() instanceof L2SiegeSummonInstance)
			return false;

		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, getEffector());
		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
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
