package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Character;

public class L2DefenderSpawnZone extends EntityZone
{
	@Override
	protected void register()
	{
		_entity = CastleManager.getInstance().getCastleById(_castleId);
		if (_entity != null)
			_entity.registerDefenderSpawn(this);
		else
			_log.warn("Invalid castleId: " + _castleId);
	}

	// They just define a respawn area
	@Override
	protected void onEnter(L2Character character)
	{
	}

	@Override
	protected void onExit(L2Character character)
	{
	}
}