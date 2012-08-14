package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Character;

public class L2CastleZone extends EntityZone
{
	@Override
	protected void register()
	{
		_entity = CastleManager.getInstance().getCastleById(_castleId);
		if (_entity != null)
			_entity.registerZone(this);
		else
			_log.warn("Invalid castleId: " + _castleId);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(this,FLAG_CASTLE, true);
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_CASTLE, false);
		super.onExit(character);
	}
}