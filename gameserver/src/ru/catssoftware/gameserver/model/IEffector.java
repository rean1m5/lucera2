package ru.catssoftware.gameserver.model;

public interface IEffector {
	public void onEffectFinished(L2Character effected, L2Skill skill);
}
