package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2RiftBossAI;
import ru.catssoftware.gameserver.model.entity.DimensionalRift;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public final class L2RiftBossInstance extends L2RaidBossInstance
{
	DimensionalRift _dimensionalRift;

	public L2RiftBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public L2CharacterAI getAI() {
		if(_ai==null) 
			_ai = new L2RiftBossAI(new AIAccessor());
		return _ai;
	}
	public void setDimensionalRift(DimensionalRift DR)
	{
		_dimensionalRift = DR;
	}
	public DimensionalRift getDimensionalRift() {
		return _dimensionalRift;
	}

}