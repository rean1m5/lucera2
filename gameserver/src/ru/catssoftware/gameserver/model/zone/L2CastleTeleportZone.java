package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Character;

public class L2CastleTeleportZone extends EntityZone
{
	@Override
	protected void register()
	{
		_entity = CastleManager.getInstance().getCastleById(_castleId);
		if (_entity != null)
			_entity.registerTeleportZone(this);
		else
			_log.warn("Invalid castleId: " + _castleId);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(this,FLAG_NOSUMMON, true);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_NOSUMMON, false);
	}
}